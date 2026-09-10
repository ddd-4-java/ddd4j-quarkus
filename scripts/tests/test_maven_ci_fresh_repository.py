import re
import unittest
from pathlib import Path


REPOSITORY = Path(__file__).parents[2]


class MavenCiFreshRepositoryTest(unittest.TestCase):

    def test_all_ci_maven_invocations_use_one_job_scoped_fresh_repository(self):
        files = sorted((REPOSITORY / ".github").rglob("*.yml"))
        files += sorted((REPOSITORY / ".github").rglob("*.yaml"))
        missing_update = []
        missing_repository = []
        forbidden_maven_caches = []
        for path in files:
            lines = path.read_text(encoding="utf-8").splitlines()
            for line_number, line in enumerate(lines, 1):
                if re.search(r"^\s*cache:\s*maven\s*$", line):
                    forbidden_maven_caches.append(f"{path.relative_to(REPOSITORY)}:{line_number}")
                if not re.search(r"\./mvnw(?:-quarkus)?(?:\s|$)", line):
                    continue
                command = " ".join(lines[line_number - 1:line_number + 12])
                location = f"{path.relative_to(REPOSITORY)}:{line_number}"
                if " -U" not in command:
                    missing_update.append(location)
                if '-Dmaven.repo.local="$DDD4J_QUARKUS_CI_REPO"' not in command:
                    missing_repository.append(location)
        self.assertEqual([], forbidden_maven_caches, "setup-java Maven caches: " + ", ".join(forbidden_maven_caches))
        self.assertEqual([], missing_update, "Maven calls without -U: " + ", ".join(missing_update))
        self.assertEqual([], missing_repository, "Maven calls without job repository: " + ", ".join(missing_repository))

        configure_action = (REPOSITORY / ".github/actions/configure-maven/action.yml").read_text(encoding="utf-8")
        self.assertIn("DDD4J_QUARKUS_CI_REPO", configure_action)
        self.assertIn("GITHUB_RUN_ID", configure_action)
        self.assertIn("GITHUB_RUN_ATTEMPT", configure_action)
        self.assertIn("GITHUB_JOB", configure_action)


if __name__ == "__main__":
    unittest.main()
