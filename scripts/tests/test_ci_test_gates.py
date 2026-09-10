import re
import unittest
from pathlib import Path


WORKFLOW = Path(__file__).parents[2] / ".github" / "workflows" / "ci.yml"


class CiTestGatesTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.workflow = WORKFLOW.read_text(encoding="utf-8")

    def test_every_maven_test_command_explicitly_enables_tests(self):
        commands = re.findall(r"(?ms)^\s*run:\s*(?:\|\s*)?(.*?)(?=^\s*- name:|^\s*- uses:|^\s{2}\w|\Z)", self.workflow)
        maven_commands = [command for command in commands if "./mvnw" in command]
        self.assertTrue(maven_commands)
        for command in maven_commands:
            self.assertIn("-DskipTests=false", command, command)

    def test_test_report_upload_fails_when_reports_are_missing(self):
        self.assertIn("if-no-files-found: error", self.workflow)

    def test_all_test_jobs_verify_non_empty_reports(self):
        self.assertEqual(3, self.workflow.count("scripts/verify-test-reports.py"))

    def test_every_broker_declares_and_requires_its_integration_class(self):
        self.assertEqual(13, len(re.findall(r"^\s+test_class:", self.workflow, re.MULTILINE)))
        self.assertEqual(13, len(re.findall(r"^\s+required_test:", self.workflow, re.MULTILINE)))
        self.assertIn('--require-class "${{ matrix.test_class }}"', self.workflow)
        self.assertIn('--require-test "${{ matrix.required_test }}"', self.workflow)

    def test_every_required_broker_method_exists_in_source(self):
        repo = WORKFLOW.parents[2]
        required_tests = re.findall(r"^\s+required_test:\s+(\S+)$", self.workflow, re.MULTILINE)
        for required_test in required_tests:
            class_name, method_name = required_test.rsplit("#", 1)
            sources = list(repo.rglob(f"{class_name.rsplit('.', 1)[-1]}.java"))
            self.assertEqual(1, len(sources), required_test)
            self.assertIn(f"void {method_name}(", sources[0].read_text(encoding="utf-8"), required_test)


if __name__ == "__main__":
    unittest.main()
