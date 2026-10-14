import importlib.util
import pathlib
import tempfile
import unittest
import base64

spec = importlib.util.spec_from_file_location('uniform', pathlib.Path(__file__).parents[1] / 'check_dependencies.py')
checker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checker)


class UniformContract(unittest.TestCase):
    def test_pattern_applies_without_exact_branch_entry(self):
        policy = checker.resolve_policy({'branches': {'feature/*': {'fixed_properties': {'v': '2'}}}}, 'feature/new')
        self.assertEqual(policy['fixed_properties'], {'v': '2'})

    def test_fixed_dependency_preserves_managed_property(self):
        policy = checker.resolve_policy({'fixed_dependencies': {'a:b': '1'}}, 'main')
        self.assertIn('-DprocessProperties=true', checker.update_commands(policy)[-1])

    def test_cloud_parent_selects_actual_build_line(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            for line, expected in [('2.4.x', '8'), ('3.4.x', '17'), ('4.0.x', '21')]:
                (root / 'pom.xml').write_text('<project><modelVersion>4.0.0</modelVersion><parent><groupId>io.ddd4j.boot</groupId><artifactId>ddd4j-boot-parent</artifactId><version>' + line + '.20260630-SNAPSHOT</version></parent></project>')
                self.assertEqual(checker.runtime_config(root)['java'], expected)

    def test_settings_accept_raw_and_existing_base64_without_disclosure(self):
        with tempfile.TemporaryDirectory() as directory:
            path = pathlib.Path(directory) / 'settings.xml'
            raw = '<settings><servers/></settings>'
            for value in [raw, base64.b64encode(raw.encode()).decode()]:
                checker.configure_settings(value, path)
                self.assertEqual(path.read_text(), raw)
                self.assertEqual(path.stat().st_mode & 0o777, 0o600)

    def test_non_maven_branch_is_explicitly_not_applicable(self):
        with tempfile.TemporaryDirectory() as directory:
            self.assertEqual(checker.runtime_config(pathlib.Path(directory))['project'], 'false')

    def test_branch_patterns_apply_in_order(self):
        policy = checker.resolve_policy({'branches': {'feature/*': {'fixed_properties': {'v': '2'}},
                                                      'feature/old': {'fixed_properties': {'v': '1'}}}}, 'feature/old')
        self.assertEqual(policy['fixed_properties'], {'v': '1'})

    def test_internal_properties_are_always_protected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            (root / 'pom.xml').write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><properties><revision>1</revision><java.version>8</java.version></properties></project>')
            policy = checker.resolve_policy({}, 'main')
            checker.protected_properties(root, policy)
            self.assertIn('revision', policy['exclude_properties'])
            self.assertIn('java.version', policy['exclude_properties'])

    def test_shared_excluded_artifact_property_is_protected_before_update(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            (root / 'pom.xml').write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><properties><shared>1</shared></properties><dependencies><dependency><groupId>a</groupId><artifactId>b</artifactId><version>${shared}</version></dependency></dependencies></project>')
            policy = checker.resolve_policy({'exclude': ['a:*']}, 'main')
            checker.protected_properties(root, policy)
            self.assertIn('shared', policy['exclude_properties'])

    def test_empty_scan_is_not_clean(self):
        with tempfile.TemporaryDirectory() as directory:
            report = pathlib.Path(directory) / 'dependency-check-report.json'
            report.write_text('{"dependencies": []}')
            with self.assertRaises(ValueError):
                checker.validate_scan_report(report)

    def test_test_and_provided_dependencies_are_scanned(self):
        command = checker.scan_command({'cvss_threshold': 7}, pathlib.Path('/tmp/report'))
        self.assertIn('-DskipTestScope=false', command)
        self.assertIn('-DskipProvidedScope=false', command)

    def test_no_literal_java_baseline_does_not_guess(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            (root / 'pom.xml').write_text('<project><modelVersion>4.0.0</modelVersion></project>')
            with self.assertRaises(ValueError):
                checker.runtime_config(root)


if __name__ == '__main__':
    unittest.main()
