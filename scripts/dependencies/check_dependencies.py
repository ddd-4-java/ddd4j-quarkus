#!/usr/bin/env python3
"""按分支策略更新 Maven 依赖，分别记录构建与漏洞扫描结果。"""
import argparse
import base64
import fnmatch
import json
import os
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

VERSIONS = 'org.codehaus.mojo:versions-maven-plugin:2.23.0:'
CHECK = 'org.owasp:dependency-check-maven:13.0.0:aggregate'
KEYS = {'exclude', 'exclude_properties', 'fixed_properties', 'fixed_dependencies', 'cvss_threshold', 'branches'}
INTERNAL_PROPERTIES = {'revision', 'changelist', 'sha1', 'java.version', 'maven.version',
                       'maven.compiler.source', 'maven.compiler.target', 'maven.compiler.release'}


def runtime_config(root):
    """读取目标分支工具链；无 Maven 项目显式标记，不猜测 JDK。"""
    if not (root / 'pom.xml').is_file():
        return {'project': 'false', 'java': '21', 'maven': '3.9.16'}
    node = ET.parse(root / 'pom.xml').getroot()
    for element in node.iter():
        element.tag = element.tag.rsplit('}', 1)[-1]
    values = {child.tag: child.text or '' for child in node.findall('./properties/*')}
    version = next((values[key] for key in ('java.version', 'maven.compiler.release', 'maven.compiler.source')
                    if key in values and '${' not in values[key]), '')
    if version == '1.8':
        version = '8'
    if not re.fullmatch(r'[0-9]+', version):
        parent_group = node.findtext('parent/groupId', '')
        parent_version = node.findtext('parent/version', '')
        if parent_group == 'org.springframework.boot' and re.match(r'[23]\.', parent_version):
            version = '8' if parent_version.startswith('2.') else '17'
        elif parent_group == 'io.ddd4j.boot' and re.match(r'[234]\.', parent_version):
            version = {'2': '8', '3': '17', '4': '21'}[parent_version[0]]
        else:
            raise ValueError('目标 POM 未声明可识别的 Java 基线')
    return {'project': 'true', 'java': version,
            'maven': '4.0.0-rc-7' if node.findtext('modelVersion') == '4.1.0' else '3.9.16'}


def maven_command(root):
    """优先使用目标分支 Wrapper；历史分支使用 CI 校验下载的 Maven。"""
    return ['bash', './mvnw'] if (root / 'mvnw').is_file() else ['mvn']


def validate_scan_report(path):
    """要求真实、非空的依赖扫描报告，空扫描不能作为无漏洞证明。"""
    report = json.loads(path.read_text())
    if not isinstance(report.get('dependencies'), list) or not report['dependencies']:
        raise ValueError('扫描报告缺少非空 dependencies')


def configure_settings(value, path):
    """兼容原文和 base64 Maven settings，不输出凭据。"""
    if not value.strip():
        return
    if not value.lstrip().startswith('<'):
        value = base64.b64decode(value, validate=True).decode('utf-8')
    node = ET.fromstring(value)
    if node.tag.rsplit('}', 1)[-1] != 'settings':
        raise ValueError('Maven settings 根节点错误')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.touch(mode=0o600, exist_ok=True)
    path.chmod(0o600)
    path.write_text(value)


def resolve_policy(raw, branch):
    """合并公共策略和精确匹配的分支策略，拒绝拼写错误。"""
    if not isinstance(raw, dict):
        raise ValueError('策略必须是 JSON 对象')
    override = {}
    for pattern, layer in raw.get('branches', {}).items():
        if not isinstance(layer, dict) or set(layer) - (KEYS - {'branches'}):
            raise ValueError('非法分支策略: ' + pattern)
        if fnmatch.fnmatchcase(branch, pattern):
            for key, value in layer.items():
                if key in ('fixed_properties', 'fixed_dependencies'):
                    override[key] = {**override.get(key, {}), **value}
                elif key in ('exclude', 'exclude_properties'):
                    override[key] = override.get(key, []) + value
                else:
                    override[key] = value
    for obj in (raw, override):
        if set(obj) - KEYS:
            raise ValueError('未知策略字段: ' + str(set(obj) - KEYS))
    result = {}
    for key in ('exclude', 'exclude_properties'):
        result[key] = list(dict.fromkeys(raw.get(key, []) + override.get(key, [])))
        if any(not isinstance(v, str) or ',' in v or not v for v in result[key]):
            raise ValueError('排除规则必须是非空字符串，且不得含逗号')
    for key in ('fixed_properties', 'fixed_dependencies'):
        result[key] = {**raw.get(key, {}), **override.get(key, {})}
        for name, version in result[key].items():
            pattern = r'[\w.-]+:[\w.-]+' if key == 'fixed_dependencies' else r'[\w.-]+'
            if not re.fullmatch(pattern, name) or not isinstance(version, str) or not re.fullmatch(r'[\w.+-]+', version):
                raise ValueError('非法固定版本规则: ' + name)
    result['cvss_threshold'] = override.get('cvss_threshold', raw.get('cvss_threshold', 7))
    if not 0 <= float(result['cvss_threshold']) <= 10:
        raise ValueError('CVSS 阈值必须在 0 到 10 之间')
    if set(result['fixed_properties']) & INTERNAL_PROPERTIES:
        raise ValueError('不能通过依赖更新策略修改项目或工具链版本')
    result['exclude_properties'] += sorted(INTERNAL_PROPERTIES)
    return result


def update_commands(policy):
    """生成无 shell 插值的 Maven 参数；固定版本优先于排除。"""
    common = ['-DgenerateBackupPoms=false', '-Dversions.skip=false', '-DprocessDependencies=true',
              '-DprocessDependencyManagement=true', '-DallowSnapshots=false', '-DallowPreReleases=false',
              '-DprocessParent=true', '-DexcludeReactor=true', '-DallowMajorUpdates=true',
              '-DallowMinorUpdates=true', '-DallowIncrementalUpdates=true', '-DallowDowngrade=true']
    excludes = policy['exclude'] + list(policy['fixed_dependencies'])
    if excludes:
        common.append('-Dexcludes=' + ','.join(excludes))
    prop_excludes = policy['exclude_properties'] + list(policy['fixed_properties'])
    property_args = ['-DexcludeProperties=' + ','.join(prop_excludes)] if prop_excludes else []
    commands = [[VERSIONS + 'update-properties', *common, *property_args],
                [VERSIONS + 'use-latest-versions', *common]]
    # 固定规则单独应用，不能被公共排除规则拦截。
    pin_common = [arg for arg in common if not arg.startswith('-Dexcludes=')]
    for name, version in policy['fixed_properties'].items():
        commands.append([VERSIONS + 'set-property', *pin_common, '-Dproperty=' + name, '-DnewVersion=' + version])
    for name, version in policy['fixed_dependencies'].items():
        commands.append([VERSIONS + 'use-dep-version', *pin_common, '-Dincludes=' + name,
                         '-DdepVersion=' + version, '-DforceVersion=true', '-DprocessProperties=true'])
    return commands


def scan_command(policy, output):
    """扫描所有解析依赖；升级排除规则不得成为漏洞抑制规则。"""
    return [CHECK, '-DfailBuildOnCVSS=' + str(policy['cvss_threshold']), '-DfailOnError=true',
            '-DskipTestScope=false', '-DskipProvidedScope=false', '-DossindexAnalyzerEnabled=false',
            '-Ddependency-check.skip=false', '-Dformats=HTML,JSON', '-DoutputDirectory=' + str(output),
            '-DnvdApiKeyEnvironmentVariable=NVD_API_KEY', '-DdataDirectory=' + str(Path.home() / '.cache/dependency-check')]


def protected_properties(root, policy):
    """把属性通配符展开成 Versions 插件接受的精确属性名。"""
    names = set()
    shared = set()
    for pom in root.rglob('pom.xml'):
        if 'target' in pom.parts or '.git' in pom.parts:
            continue
        tree = ET.parse(pom)
        ns = {'m': tree.getroot().tag.split('}')[0][1:]}
        for props in tree.findall('.//m:properties', ns):
            names.update(child.tag.split('}')[-1] for child in props)
        for dep in tree.findall('.//m:dependency', ns) + tree.findall('.//m:parent', ns):
            coordinate = dep.findtext('m:groupId', '', ns) + ':' + dep.findtext('m:artifactId', '', ns)
            if coordinate not in policy['fixed_dependencies'] and any(fnmatch.fnmatchcase(coordinate, pattern) for pattern in policy['exclude']):
                shared.update(re.findall(r'\$\{([^}]+)\}', dep.findtext('m:version', '', ns)))
    missing = set(policy['fixed_properties']) - names
    if missing:
        raise ValueError('固定属性在分支源码中不存在: ' + ','.join(sorted(missing)))
    policy['exclude_properties'] = sorted(shared | {name for name in names if any(
        fnmatch.fnmatchcase(name, pattern) for pattern in policy['exclude_properties'])})


def declared_versions(root):
    """读取源码声明及本地父 POM 属性，用于防止共享属性绕过排除规则。"""
    result = {}
    cache = {}

    def properties(pom, seen):
        if pom in seen:
            raise ValueError('本地父 POM 循环: ' + str(pom))
        if pom in cache:
            return cache[pom]
        tree = ET.parse(pom)
        node = tree.getroot()
        ns = {'m': node.tag.split('}')[0][1:]}
        values = {}
        relative = node.findtext('m:parent/m:relativePath', default='../pom.xml', namespaces=ns)
        parent = (pom.parent / relative).resolve() if relative else None
        if parent and parent != pom and root in parent.parents and parent.is_file():
            values.update(properties(parent, seen | {pom}))
        for props in tree.findall('./m:properties', ns):
            values.update({child.tag.split('}')[-1]: child.text or '' for child in props})
        cache[pom] = values
        return values

    for pom in sorted(root.rglob('pom.xml')):
        if 'target' in pom.parts or '.git' in pom.parts:
            continue
        tree = ET.parse(pom)
        ns = {'m': tree.getroot().tag.split('}')[0][1:]}
        values = properties(pom, set())
        file = str(pom.relative_to(root))
        for index, dep in enumerate(tree.findall('.//m:dependency', ns) + tree.findall('.//m:parent', ns)):
            coordinate = dep.findtext('m:groupId', default='', namespaces=ns) + ':' + dep.findtext('m:artifactId', default='', namespaces=ns)
            version = dep.findtext('m:version', default='', namespaces=ns)
            for _ in range(20):
                resolved = re.sub(r'\$\{([^}]+)\}', lambda m: values.get(m.group(1), m.group(0)), version)
                if resolved == version:
                    break
                version = resolved
            result[(file, coordinate, index)] = version
        for props in tree.findall('.//m:properties', ns):
            for index, child in enumerate(props):
                result[(file, '#' + child.tag.split('}')[-1], index)] = child.text or ''
        result[(file, '#project.version', -1)] = tree.getroot().findtext('m:version', '', ns)
    return result


def validate_updates(root, policy, before):
    """固定规则必须实际生效；排除组件和保护属性必须保持版本。"""
    after = declared_versions(root)
    pinned_property_dependencies = set()
    for pom in root.rglob('pom.xml'):
        if 'target' in pom.parts or '.git' in pom.parts:
            continue
        node = ET.parse(pom).getroot()
        ns = {'m': node.tag.split('}')[0][1:]}
        for index, dep in enumerate(node.findall('.//m:dependency', ns) + node.findall('.//m:parent', ns)):
            refs = re.findall(r'\$\{([^}]+)\}', dep.findtext('m:version', default='', namespaces=ns))
            if set(refs) & set(policy['fixed_properties']):
                coordinate = dep.findtext('m:groupId', default='', namespaces=ns) + ':' + dep.findtext('m:artifactId', default='', namespaces=ns)
                pinned_property_dependencies.add((str(pom.relative_to(root)), coordinate, index))
    for key, version in before.items():
        coordinate = key[1]
        if coordinate.startswith('#'):
            name = coordinate[1:]
            protected = (name in policy['exclude_properties'] or name == 'project.version') and name not in policy['fixed_properties']
        else:
            protected = key not in pinned_property_dependencies and coordinate not in policy['fixed_dependencies'] and any(
                fnmatch.fnmatchcase(coordinate, pattern) for pattern in policy['exclude'])
        if protected and after.get(key) != version:
            raise ValueError('排除版本被修改: ' + str(key))
    for coordinate, version in policy['fixed_dependencies'].items():
        actual = [value for key, value in after.items() if key[1] == coordinate and value]
        if not actual or any(value != version for value in actual):
            raise ValueError('固定依赖未生效（需要本仓库显式版本声明）: ' + coordinate)
    for name, version in policy['fixed_properties'].items():
        actual = [value for key, value in after.items() if key[1] == '#' + name]
        if not actual or any(value != version for value in actual):
            raise ValueError('固定属性未生效: ' + name)


def materialize_inherited_properties(root, policy, effective):
    """将本仓库显式引用的外部父属性覆盖到根 POM，保留排除共享属性。"""
    defined, referenced, protected = set(), set(), set()
    for pom in root.rglob('pom.xml'):
        if 'target' in pom.parts or '.git' in pom.parts:
            continue
        node = ET.parse(pom).getroot()
        ns = {'m': node.tag.split('}')[0][1:]}
        for props in node.findall('.//m:properties', ns):
            defined.update(child.tag.split('}')[-1] for child in props)
        for dep in node.findall('.//m:dependency', ns):
            version = dep.findtext('m:version', default='', namespaces=ns)
            match = re.fullmatch(r'\$\{([\w.-]+)\}', version)
            if not match:
                continue
            name = match.group(1)
            coordinate = dep.findtext('m:groupId', default='', namespaces=ns) + ':' + dep.findtext('m:artifactId', default='', namespaces=ns)
            referenced.add(name)
            if any(fnmatch.fnmatchcase(coordinate, pattern) for pattern in policy['exclude']):
                protected.add(name)
    wanted = referenced - defined - protected
    wanted = {name for name in wanted if not name.startswith(('project.', 'pom.')) and not any(
        fnmatch.fnmatchcase(name, pattern) for pattern in policy['exclude_properties'])}
    if not wanted:
        return
    node = ET.parse(effective).getroot()
    ns = {'m': node.tag.split('}')[0][1:]}
    props = node.find('./m:properties', ns)
    values = {} if props is None else {child.tag.split('}')[-1]: child.text or '' for child in props}
    if wanted - set(values):
        raise ValueError('外部父属性无法解析: ' + ','.join(sorted(wanted - set(values))))
    from xml.sax.saxutils import escape
    additions = ''.join('        <' + name + '>' + escape(values[name]) + '</' + name + '>\n' for name in sorted(wanted))
    pom = root / 'pom.xml'
    text = pom.read_text()
    if ET.parse(pom).getroot().find('./m:properties', ns) is not None:
        if '</properties>' not in text:
            raise ValueError('不支持带 XML 前缀的 properties；请显式声明版本属性')
        text = text.replace('</properties>', '\n' + additions + '    </properties>', 1)
    else:
        if '</project>' not in text:
            raise ValueError('不支持带 XML 前缀的 project')
        text = text.replace('</project>', '    <properties>\n' + additions + '    </properties>\n</project>', 1)
    pom.write_text(text)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, required=True)
    parser.add_argument('--runtime', action='store_true', help='仅输出分支工具链配置')
    parser.add_argument('--policy', type=Path)
    parser.add_argument('--branch', default='')
    parser.add_argument('--reports', type=Path)
    args = parser.parse_args()
    if args.runtime:
        for key, value in runtime_config(args.root.resolve()).items():
            print(key + '=' + value)
        return 0
    if not args.policy or not args.reports or not args.branch:
        parser.error('--policy, --reports and --branch are required for a check')
    root, reports = args.root.resolve(), args.reports.resolve()
    reports.mkdir(parents=True, exist_ok=True)
    if not (root / 'pom.xml').exists():
        (reports / 'result.json').write_text(json.dumps({'branch': args.branch, 'status': 'not-applicable-no-maven-project', 'pr_ready': False}))
        print('该分支无根 POM，未执行 Maven 检查。')
        return 0
    policy = resolve_policy(json.loads(args.policy.read_text()), args.branch)
    property_patterns = list(policy['exclude_properties'])
    protected_properties(root, policy)
    (reports / 'policy.json').write_text(json.dumps(policy, indent=2))
    statuses = {}

    def run(stage, command):
        with (reports / (stage + '.log')).open('w') as log:
            env = os.environ.copy()
            if not stage.endswith('-scan') and env.get('BUILD_JAVA_HOME'):
                env['JAVA_HOME'] = env['BUILD_JAVA_HOME']
            if env.get('JAVA_HOME'):
                env['PATH'] = str(Path(env['JAVA_HOME']) / 'bin') + os.pathsep + env.get('PATH', '')
            result = subprocess.run([*maven_command(root), '-B', '-ntp', '-U', *command], cwd=root, env=env, stdout=log, stderr=subprocess.STDOUT)
        code = result.returncode
        if stage.endswith('-scan') and code == 0:
            directory = reports / stage.removesuffix('-scan')
            if not all((directory / ('dependency-check-report.' + suffix)).is_file() for suffix in ('html', 'json')):
                with (reports / (stage + '.log')).open('a') as log:
                    log.write('\n扫描未生成 HTML/JSON 报告，拒绝视为通过。\n')
                code = 1
            else:
                try:
                    validate_scan_report(directory / 'dependency-check-report.json')
                except (ValueError, OSError) as error:
                    with (reports / (stage + '.log')).open('a') as log:
                        log.write(str(error) + '\n')
                    code = 1
        statuses[stage] = code
        print(stage + ': ' + ('通过' if code == 0 else '失败（见日志）'), flush=True)
        return code == 0

    build = ['install', '-DskipTests=false', '-Dmaven.test.skip=false', '-Dsurefire.skip=false', '-Denforcer.skip=true', '-Ddependency-check.skip=true']
    run('baseline-build', build)
    run('baseline-scan', scan_command(policy, reports / 'baseline'))
    before = declared_versions(root)
    updated = run('effective-model', ['org.apache.maven.plugins:maven-help-plugin:3.5.1:effective-pom', '-N', '-Doutput=' + str(reports / 'effective-pom.xml')])
    if updated:
        try:
            materialize_inherited_properties(root, {**policy, 'exclude_properties': property_patterns}, reports / 'effective-pom.xml')
            statuses['inherited-properties'] = 0
        except ValueError as error:
            (reports / 'inherited-properties.log').write_text(str(error))
            statuses['inherited-properties'] = 1
            updated = False
    for index, command in enumerate(update_commands(policy) if updated else []):
        if not run('update-' + str(index), command):
            updated = False
            break
    if updated:
        try:
            validate_updates(root, policy, before)
            statuses['policy-validation'] = 0
        except ValueError as error:
            (reports / 'policy-validation.log').write_text(str(error))
            statuses['policy-validation'] = 1
            updated = False
    # 升级失败仍保留原依赖扫描证据，不将部分升级作为有效 PR。
    if updated:
        run('updated-build', build)
        run('updated-scan', scan_command(policy, reports / 'updated'))
    with (reports / 'dependencies.patch').open('w') as patch:
        subprocess.run(['git', 'diff', '--', '**/pom.xml', 'pom.xml'], cwd=root, stdout=patch, check=True)
    ready = updated and statuses.get('updated-build') == 0 and statuses.get('updated-scan') == 0
    (reports / 'result.json').write_text(json.dumps({'branch': args.branch, 'stages': statuses, 'pr_ready': ready}, indent=2))
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
            output.write('pr_ready=' + str(ready).lower() + '\n')
    # 原依赖有漏洞也保持失败状态，即使升级后的 PR 已可供合并。
    return int(any(statuses.values()))


if __name__ == '__main__':
    raise SystemExit(main())
