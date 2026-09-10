import subprocess
import tempfile
import textwrap
import unittest
from pathlib import Path


SCRIPT = Path(__file__).parents[1] / "verify-test-reports.py"


class VerifyTestReportsTest(unittest.TestCase):

    def run_verifier(self, xml: str | None, *args: str) -> subprocess.CompletedProcess[str]:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            if xml is not None:
                reports = root / "module" / "target" / "surefire-reports"
                reports.mkdir(parents=True)
                (reports / "TEST-example.xml").write_text(textwrap.dedent(xml), encoding="utf-8")
            return subprocess.run(
                ["python3", str(SCRIPT), *args, str(root)],
                text=True,
                capture_output=True,
                check=False,
            )

    def test_rejects_missing_reports(self):
        result = self.run_verifier(None)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("no Surefire or Failsafe XML reports", result.stderr)

    def test_rejects_zero_executed_tests(self):
        result = self.run_verifier(
            """
            <testsuite name="ExampleTest" tests="1" failures="0" errors="0" skipped="1">
              <testcase classname="ExampleTest" name="disabled"><skipped/></testcase>
            </testsuite>
            """
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("zero executed tests", result.stderr)

    def test_rejects_required_class_when_all_its_tests_are_skipped(self):
        result = self.run_verifier(
            """
            <testsuite name="io.example.BrokerIT" tests="2" failures="0" errors="0" skipped="1">
              <testcase classname="io.example.BrokerIT" name="roundTrip"><skipped/></testcase>
              <testcase classname="io.example.OtherTest" name="runs"/>
            </testsuite>
            """,
            "--require-class",
            "io.example.BrokerIT",
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("required test class has no executed tests", result.stderr)

    def test_accepts_and_aggregates_surefire_and_failsafe_reports(self):
        result = self.run_verifier(
            """
            <testsuite name="io.example.BrokerIT" tests="2" failures="0" errors="0" skipped="1">
              <testcase classname="io.example.BrokerIT" name="roundTrip"/>
              <testcase classname="io.example.BrokerIT" name="platformOnly"><skipped/></testcase>
            </testsuite>
            """,
            "--require-class",
            "io.example.BrokerIT",
        )
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("tests=2", result.stdout)
        self.assertIn("executed=1", result.stdout)
        self.assertIn("skipped=1", result.stdout)

    def test_rejects_required_test_method_when_it_is_skipped(self):
        result = self.run_verifier(
            """
            <testsuite name="io.example.BrokerIT" tests="2" failures="0" errors="0" skipped="1">
              <testcase classname="io.example.BrokerIT" name="roundTrip"><skipped/></testcase>
              <testcase classname="io.example.BrokerIT" name="injectsClient"/>
            </testsuite>
            """,
            "--require-test",
            "io.example.BrokerIT#roundTrip",
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("required test method was not executed", result.stderr)

    def test_rejects_report_with_test_failure(self):
        result = self.run_verifier(
            """
            <testsuite name="ExampleTest" tests="1" failures="1" errors="0" skipped="0">
              <testcase classname="ExampleTest" name="fails"><failure message="boom"/></testcase>
            </testsuite>
            """
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("test reports contain failures or errors", result.stderr)

    def test_rejects_report_with_test_error(self):
        result = self.run_verifier(
            """
            <testsuite name="ExampleTest" tests="1" failures="0" errors="1" skipped="0">
              <testcase classname="ExampleTest" name="errors"><error message="boom"/></testcase>
            </testsuite>
            """
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("test reports contain failures or errors", result.stderr)


if __name__ == "__main__":
    unittest.main()
