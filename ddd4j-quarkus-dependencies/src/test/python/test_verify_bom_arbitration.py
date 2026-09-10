import os
import unittest
import xml.etree.ElementTree as ET

import verify_bom_arbitration as gate


class ProjectIndexTest(unittest.TestCase):

    def test_ci_maven_calls_use_the_job_scoped_repository_and_refresh_snapshots(self):
        previous = os.environ.get("DDD4J_QUARKUS_CI_REPO")
        try:
            os.environ["DDD4J_QUARKUS_CI_REPO"] = "/tmp/ddd4j-quarkus-ci"
            self.assertEqual(
                ["-U", "-Dmaven.repo.local=/tmp/ddd4j-quarkus-ci"],
                gate.maven_args(gate.Path(".")),
            )
        finally:
            if previous is None:
                os.environ.pop("DDD4J_QUARKUS_CI_REPO", None)
            else:
                os.environ["DDD4J_QUARKUS_CI_REPO"] = previous

    def test_inherits_group_id_and_rejects_duplicate_coordinates(self):
        projects = [
            ET.fromstring("<project><parent><groupId>io.ddd4j.quarkus</groupId></parent><artifactId>duplicate</artifactId></project>"),
            ET.fromstring("<project><groupId>io.ddd4j.quarkus</groupId><artifactId>duplicate</artifactId></project>"),
        ]
        with self.assertRaisesRegex(ValueError, "Duplicate reactor project coordinate"):
            gate.index_projects(projects)


if __name__ == "__main__":
    unittest.main()
