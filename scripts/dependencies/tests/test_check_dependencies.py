import importlib.util
import pathlib
import unittest

spec = importlib.util.spec_from_file_location('checker', pathlib.Path(__file__).parents[1] / 'check_dependencies.py')
checker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checker)


class PolicyTests(unittest.TestCase):
    def test_branch_overrides_merge_pins_and_exclusions(self):
        policy = {'fixed_properties': {'a.version': '1'}, 'exclude': ['spring:*'],
                  'branches': {'legacy': {'fixed_properties': {'b.version': '2'}, 'exclude': ['legacy:*']}}}
        resolved = checker.resolve_policy(policy, 'legacy')
        self.assertEqual(resolved['fixed_properties'], {'a.version': '1', 'b.version': '2'})
        self.assertEqual(resolved['exclude'], ['spring:*', 'legacy:*'])

    def test_pins_are_excluded_from_latest_and_applied_explicitly(self):
        policy = checker.resolve_policy({'fixed_dependencies': {'a:b': '1.2'},
                                       'fixed_properties': {'c.version': '3'}, 'exclude': ['spring:*']}, 'main')
        commands = checker.update_commands(policy)
        self.assertTrue(any('a:b' in arg and arg.startswith('-Dexcludes=') for cmd in commands for arg in cmd))
        self.assertTrue(any('-Dproperty=c.version' in cmd and '-DnewVersion=3' in cmd for cmd in commands))
        self.assertTrue(any('-Dincludes=a:b' in cmd and '-DdepVersion=1.2' in cmd for cmd in commands))
        self.assertTrue(all('-DallowSnapshots=false' in cmd for cmd in commands))

    def test_unknown_policy_key_fails(self):
        with self.assertRaises(ValueError):
            checker.resolve_policy({'exlcude': ['spring:*']}, 'main')

    def test_unsafe_or_wildcard_fixed_coordinates_fail(self):
        for coordinate in ['a:*', 'a:b,c:d', '-Dx=y', 'a']:
            with self.subTest(coordinate=coordinate), self.assertRaises(ValueError):
                checker.resolve_policy({'fixed_dependencies': {coordinate: '1'}}, 'main')

    def test_scan_keeps_pinned_and_excluded_components(self):
        command = checker.scan_command({'cvss_threshold': 7}, pathlib.Path('/tmp/reports'))
        self.assertIn('-DfailBuildOnCVSS=7', command)
        self.assertFalse(any(arg.startswith('-Dexcludes') for arg in command))

class ValidationTests(unittest.TestCase):
    def test_fixed_dependency_must_exist_and_take_effect(self):
        import tempfile
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            (root / 'pom.xml').write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><properties><v>1</v></properties><dependencies><dependency><groupId>a</groupId><artifactId>b</artifactId><version>${v}</version></dependency></dependencies></project>')
            policy = checker.resolve_policy({'fixed_dependencies': {'a:b': '2'}}, 'main')
            with self.assertRaises(ValueError):
                checker.validate_updates(root, policy, {})
            (root / 'pom.xml').write_text((root / 'pom.xml').read_text().replace('<v>1</v>', '<v>2</v>'))
            checker.validate_updates(root, policy, {})

    def test_excluded_shared_property_must_not_change(self):
        import tempfile
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            pom = root / 'pom.xml'
            pom.write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><properties><v>1</v></properties><dependencies><dependency><groupId>spring</groupId><artifactId>core</artifactId><version>${v}</version></dependency></dependencies></project>')
            policy = checker.resolve_policy({'exclude': ['spring:*']}, 'main')
            before = checker.declared_versions(root)
            pom.write_text(pom.read_text().replace('<v>1</v>', '<v>2</v>'))
            with self.assertRaises(ValueError):
                checker.validate_updates(root, policy, before)

    def test_inherited_dependency_property_is_materialized_but_spring_is_protected(self):
        import tempfile
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            (root / 'pom.xml').write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><properties><revision>1</revision></properties><dependencies><dependency><groupId>io.undertow</groupId><artifactId>undertow-core</artifactId><version>${undertow.version}</version></dependency><dependency><groupId>org.springframework</groupId><artifactId>spring-core</artifactId><version>${spring.version}</version></dependency></dependencies></project>')
            effective = root / 'effective.xml'
            effective.write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><properties><undertow.version>2.3.0.Final</undertow.version><spring.version>6.0.0</spring.version></properties></project>')
            policy = checker.resolve_policy({'exclude': ['org.springframework*:*'], 'exclude_properties': ['spring*']}, 'main')
            checker.materialize_inherited_properties(root, policy, effective)
            text = (root / 'pom.xml').read_text()
            self.assertIn('<undertow.version>2.3.0.Final</undertow.version>', text)
            self.assertNotIn('<spring.version>', text)

    def test_fixed_property_overrides_artifact_exclusion(self):
        import tempfile
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            pom = root / 'pom.xml'
            pom.write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><properties><spring.version>1</spring.version></properties><dependencies><dependency><groupId>spring</groupId><artifactId>core</artifactId><version>${spring.version}</version></dependency></dependencies></project>')
            policy = checker.resolve_policy({'exclude': ['spring:*'], 'exclude_properties': ['spring.version'], 'fixed_properties': {'spring.version': '2'}}, 'main')
            before = checker.declared_versions(root)
            pom.write_text(pom.read_text().replace('<spring.version>1</spring.version>', '<spring.version>2</spring.version>'))
            checker.validate_updates(root, policy, before)


class PipelineTests(unittest.TestCase):
    def run_pipeline(self, failed_scan_index=None, fail_update=False, missing_reports=False):
        import contextlib
        import io
        import json
        import os
        import subprocess
        import tempfile
        from unittest.mock import patch
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            project = root / 'project'
            project.mkdir()
            (project / 'pom.xml').write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>app</artifactId><version>1</version></project>')
            policy = root / 'policy.json'
            policy.write_text('{}')
            reports = root / 'reports'
            scans = []

            def execute(command, **kwargs):
                code = 0
                if checker.CHECK in command:
                    scans.append(command)
                    directory = pathlib.Path(next(arg.split('=', 1)[1] for arg in command if arg.startswith('-DoutputDirectory=')))
                    directory.mkdir(parents=True, exist_ok=True)
                    if not missing_reports:
                        (directory / 'dependency-check-report.html').write_text('<html></html>')
                        (directory / 'dependency-check-report.json').write_text('{"dependencies": [{"fileName": "fixture.jar"}]}')
                    if len(scans) == failed_scan_index:
                        code = 1
                if fail_update and any(arg.endswith(':update-properties') for arg in command):
                    code = 1
                return subprocess.CompletedProcess(command, code)

            argv = ['check', '--root', str(project), '--policy', str(policy), '--branch', 'main', '--reports', str(reports)]
            with patch('sys.argv', argv), patch.object(checker.subprocess, 'run', side_effect=execute), patch.dict(os.environ, {'GITHUB_OUTPUT': str(root / 'output')}), contextlib.redirect_stdout(io.StringIO()):
                exit_code = checker.main()
            return exit_code, json.loads((reports / 'result.json').read_text()), len(scans)

    def test_original_vulnerability_keeps_ci_failed_but_allows_fixed_candidate_pr(self):
        exit_code, report, scans = self.run_pipeline(failed_scan_index=1)
        self.assertEqual(exit_code, 1)
        self.assertTrue(report['pr_ready'])
        self.assertEqual(scans, 2)

    def test_vulnerable_candidate_blocks_pr(self):
        exit_code, report, scans = self.run_pipeline(failed_scan_index=2)
        self.assertEqual(exit_code, 1)
        self.assertFalse(report['pr_ready'])
        self.assertEqual(scans, 2)

    def test_failed_upgrade_preserves_baseline_scan_and_blocks_pr(self):
        exit_code, report, scans = self.run_pipeline(fail_update=True)
        self.assertEqual(exit_code, 1)
        self.assertFalse(report['pr_ready'])
        self.assertEqual(scans, 1)

    def test_missing_scan_reports_block_pr_even_if_maven_returns_success(self):
        exit_code, report, scans = self.run_pipeline(missing_reports=True)
        self.assertEqual(exit_code, 1)
        self.assertFalse(report['pr_ready'])
        self.assertEqual(scans, 2)


if __name__ == '__main__':
    unittest.main()
