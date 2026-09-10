import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


REPOSITORY = Path(__file__).parents[2]
UNIX_ENTRYPOINT = REPOSITORY / "mvnw-quarkus"
WINDOWS_ENTRYPOINT = REPOSITORY / "mvnw-quarkus.cmd"


class QuarkusMavenEntrypointTest(unittest.TestCase):

    def test_unix_entrypoint_uses_absolute_compatibility_paths_and_preserves_exit_code(self):
        self.assertTrue(UNIX_ENTRYPOINT.is_file())
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            shutil.copy2(UNIX_ENTRYPOINT, root / "mvnw-quarkus")
            (root / "mvnw-quarkus").chmod(0o755)
            settings = root / ".mvn" / "maven3-home" / "conf" / "settings.xml"
            settings.parent.mkdir(parents=True)
            settings.write_text("<settings/>\n", encoding="utf-8")
            capture = root / "arguments.txt"
            fake_maven = root / "mvnw"
            fake_maven.write_text(
                "#!/bin/sh\nprintf '%s\\n' \"$@\" > \"$CAPTURE\"\nexit \"$FAKE_EXIT\"\n",
                encoding="utf-8",
            )
            fake_maven.chmod(0o755)
            environment = os.environ.copy()
            environment.update(CAPTURE=str(capture), FAKE_EXIT="23")

            result = subprocess.run(
                [str(root / "mvnw-quarkus"), "verify", "-Dmessage=hello world"],
                cwd="/",
                env=environment,
                check=False,
            )

            self.assertEqual(23, result.returncode)
            self.assertEqual(
                [
                    "-gs",
                    str(settings),
                    f"-Dddd4j.maven.home={root / '.mvn' / 'maven3-home'}",
                    "verify",
                    "-Dmessage=hello world",
                ],
                capture.read_text(encoding="utf-8").splitlines(),
            )

    def test_windows_entrypoint_declares_sibling_wrapper_and_exit_code_forwarding(self):
        self.assertTrue(WINDOWS_ENTRYPOINT.is_file())
        content = WINDOWS_ENTRYPOINT.read_text(encoding="utf-8")
        self.assertIn("%~dp0", content)
        self.assertIn("mvnw.cmd", content)
        self.assertIn("%*", content)
        self.assertIn("exit /b", content.lower())


if __name__ == "__main__":
    unittest.main()
