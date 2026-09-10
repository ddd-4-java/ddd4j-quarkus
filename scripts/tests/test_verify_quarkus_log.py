import subprocess
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).parents[1] / "verify-quarkus-log.py"


class VerifyQuarkusLogTest(unittest.TestCase):

    def run_verifier(self, content: str) -> subprocess.CompletedProcess[str]:
        with tempfile.TemporaryDirectory() as directory:
            log = Path(directory) / "verify.log"
            log.write_text(content, encoding="utf-8")
            return subprocess.run(
                ["python3", str(SCRIPT), str(log)],
                text=True,
                capture_output=True,
                check=False,
            )

    def test_rejects_unrecognized_quarkus_configuration(self):
        result = self.run_verifier(
            'WARN Unrecognized configuration key "quarkus.hibernate-orm.enabled" was provided\n'
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("unrecognized Quarkus configuration", result.stderr)

    def test_rejects_deprecated_quarkus_configuration(self):
        result = self.run_verifier(
            'WARN The "quarkus.hibernate-orm.database.generation" config property is deprecated '
            'and should not be used anymore.\n'
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("deprecated Quarkus configuration", result.stderr)

    def test_accepts_clean_log(self):
        result = self.run_verifier("[INFO] BUILD SUCCESS\n")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("Quarkus log verified", result.stdout)


if __name__ == "__main__":
    unittest.main()
