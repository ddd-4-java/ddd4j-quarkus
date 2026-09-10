import unittest
import xml.etree.ElementTree as ET

import verify_bom_arbitration as gate


class ProjectIndexTest(unittest.TestCase):

    def test_inherits_group_id_and_rejects_duplicate_coordinates(self):
        projects = [
            ET.fromstring("<project><parent><groupId>io.ddd4j.quarkus</groupId></parent><artifactId>duplicate</artifactId></project>"),
            ET.fromstring("<project><groupId>io.ddd4j.quarkus</groupId><artifactId>duplicate</artifactId></project>"),
        ]
        with self.assertRaisesRegex(ValueError, "Duplicate reactor project coordinate"):
            gate.index_projects(projects)


if __name__ == "__main__":
    unittest.main()
