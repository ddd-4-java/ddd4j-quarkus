> 2026-10-14 更新：以下内容保留为旧实现记录。当前执行契约、脚本和配置以 `docs/weekly-dependencies.md`、`.github/workflows/weekly-dependencies.yml`、`.github/dependency-policy.json` 与 `scripts/dependencies/check_dependencies.py` 为准；每个分支均保留 CI，读取目标分支策略。下文已退役的 Renovate/旧脚本路径不再作为运行指引。

# 每周依赖维护

`.github/workflows/dependency-maintenance.yml` 每周一北京时间 **10:17** 运行，也可在
Actions → Weekly dependency maintenance → Run workflow 手动执行。GitHub 的定时任务可能延迟。

Renovate 每次动态列出仓库全部分支，排除 `renovate/` 机器人分支。新增业务分支无需改矩阵。
没有依赖文件的分支正常无更新；有 Maven POM、Maven wrapper、GitHub Actions 等受支持
依赖文件的分支都会检查。各分支创建独立更新 PR，未约束的依赖选择最新稳定版本，
包括 major。PR 合并后才会改变目标分支实际使用的版本，不把构建版本改成 `LATEST`。

## 首次启用

1. 将此工作流、策略、装配脚本、测试合入默认分支 **master**。只放在维护分支不会触发 schedule，
   手动更新 job 也只允许默认分支执行。此处不要求把业务源码合入 master。
2. 在仓库或组织 Actions secrets 配置 `RENOVATE_TOKEN`。使用专用机器人 PAT，授权此仓库
   Contents、Pull requests、Issues 的读写权限；更新 GitHub Actions 文件还需对应 Workflows
   权限。若使用 classic PAT，则需 `repo` 和 `workflow` scope。组织有 SSO 时需授权。
   使用专用 token 是为了让机器人 PR 正常触发构建 CI；本工作流的 `GITHUB_TOKEN` 仅只读。
3. 私有 Maven 仓库如需认证，另设 `RENOVATE_HOST_RULES` secret，值为 JSON 数组，例如：

   ```json
   [
     {
       "hostType": "maven",
       "matchHost": "https://packages.aliyun.com/",
       "username": "你的只读仓库用户名",
       "password": "你的只读仓库凭据"
     }
   ]
   ```

   缺省为 `[]`。Renovate 使用自己的 HTTP 客户端，不直接读取已有 `MAVEN_SETTINGS_XML`。
   即使上游坐标被排除，解析父 POM/属性时仍可能需要私仓认证。认证或解析告警必须在
   Dashboard / 日志检查，不能将未解析成功的依赖当作已经最新。
   已启用 `exitCodeForErrors`，Renovate 已知仓库错误返回非零并使 CI 失败；仅有 lookup
   warning 时仍需按 Dashboard / 日志判断覆盖完整性。
4. 手动运行一次，检查 Dependency Dashboard、Actions 日志及
   `dependency-maintenance-<run_id>` artifact，确认每个业务分支被处理，并检查更新 PR 的 CI。
   目前 master / feature/superpowers-init 无 Maven POM，两个维护分支有业务依赖。

PR/push 只执行无 secret 的策略测试、Renovate 官方配置验证和 actionlint；不会创建更新 PR。
定时及手动更新 job 的 token 不暴露给 PR job。不要再让另一套 Renovate 实例同时使用
`renovate/` 前缀维护此仓库，以免相互清理机器人分支。

## 固定、范围与排除配置

统一编辑 **`.github/dependency-policy.json`**，规则采用 Renovate 官方 `packageRules`
语法。自托管装配器用 `requireConfig: ignored`，各业务分支的 renovate.json 不参与策略，
因此无需将本策略复制到每个分支。策略改动须合入默认分支才能影响下次检查。

当前显式约束：

| 坐标/分支 | 策略 |
|---|---|
| `io.quarkus:*`、`io.quarkus.platform:*` / feature/3.3.x | 固定 3.37.4 |
| `io.quarkus:*`、`io.quarkus.platform:*` / feature/4.0.x | 固定 3.38.2 |
| `io.ddd4j:*` 及子 group | 排除更新，保留各维护线的上游底座及本仓版本 |
| 其余 | 最新稳定版本，包括 major，不自动合并 |

Quarkiverse 扩展不匹配 Quarkus 核心固定规则，仍追踪最新版；版本不兼容应由现有测试发现，
需要约束时添加独立固定/范围/排除规则。新分支如果需要固定 Quarkus，增加对应
`matchBaseBranches`，否则按最新稳定版处理。

固定某个依赖（省略 `matchBaseBranches` 则对所有分支有效）：

```json
{
  "matchBaseBranches": ["feature/4.0.x"],
  "matchPackageNames": ["org.testcontainers:testcontainers-bom"],
  "allowedVersions": "/^2\\.0\\.5$/"
}
```

`allowedVersions` 限制更新候选；不会强制降级或在没有候选发布的情况下凭空写版本。
设置固定值时应与该分支当前 POM 对齐；若要采用更低版本，应另行验证并修改 POM。
也可使用 Maven 版本范围，例如 `"allowedVersions": "[2.0,3.0)"`，取范围内最新候选。

排除某个依赖或坐标族：

```json
{
  "matchPackageNames": ["org.example:legacy-sdk", "/^com\\.example:/"],
  "enabled": false
}
```

精确坐标也可加入顶层 `ignoreDeps`。多条规则匹配时按顺序合并，后面的同名字段覆盖前面；
排除规则建议放最后。删除固定/排除规则即可恢复最新稳定版本更新。

## 安全与验收边界

这个 CI 维护版本新鲜度；最新版仍可能有漏洞。现有 `security.yml` 保持独立：
每周扫描两条受保护维护线，以及受保护分支 push/手动触发的 OWASP Dependency-Check
门禁（CVSS ≥ 7 失败）。固定/排除更新不等于漏洞豁免；这些依赖的修复需调整策略或上游版本。
已有安全矩阵不因此变成所有分支的完整漏洞扫描。

Renovate 可更新本仓声明的版本/属性/BOM，不能直接修改由上游 `ddd4j-dependencies`
管理的间接版本；这些依赖的修复应在上游仓库完成，再更新本仓的底座约束。
需要排查漏更新时同时检查私仓认证、未解析属性、BOM 来源和 Dashboard 中的告警。

本地配置检查：

```bash
node --test scripts/tests/test_dependency_policy.cjs
actionlint .github/workflows/dependency-maintenance.yml
GITHUB_REPOSITORY=ddd-4-java/ddd4j-quarkus \
  node scripts/build-renovate-config.cjs > /tmp/ddd4j-quarkus-renovate-config.json
docker run --rm -v "$PWD:/config:ro" ghcr.io/renovatebot/renovate:latest \
  renovate-config-validator --strict /config/.github/dependency-policy.json
```

本地测试与 schema/workflow 检查不证明真实 API、私仓解析、跨分支更新 PR 或 PR 构建已通过。
正式启用验收以默认分支上首次 Actions 运行结果为准。
