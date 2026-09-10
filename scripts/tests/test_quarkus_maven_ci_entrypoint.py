import re
import unittest
from pathlib import Path


REPOSITORY = Path(__file__).parents[2]


class QuarkusMavenCiEntrypointTest(unittest.TestCase):

    def test_workflows_and_composite_actions_do_not_bypass_quarkus_entrypoint(self):
        files = sorted((REPOSITORY / ".github").rglob("*.yml"))
        files += sorted((REPOSITORY / ".github").rglob("*.yaml"))
        bypasses = []
        bridge_uses = []
        for path in files:
            for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
                if re.search(r"(?:^|\s)\./mvnw(?:\s|$)", line):
                    bypasses.append(f"{path.relative_to(REPOSITORY)}:{line_number}")
                if "./mvnw-quarkus" in line:
                    bridge_uses.append(f"{path.relative_to(REPOSITORY)}:{line_number}")
        self.assertEqual([], bypasses, "direct Maven wrapper calls: " + ", ".join(bypasses))
        self.assertTrue(bridge_uses, "no workflow or composite action uses mvnw-quarkus")


if __name__ == "__main__":
    unittest.main()
