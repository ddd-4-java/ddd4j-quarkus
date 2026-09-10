"""Maven Model 4.1 parent declaration contract tests."""

import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

from verify_model41_parent_contract import verify


ROOT = Path(__file__).parents[1]


def child(parent, name):
    return parent.find(f"{{*}}{name}")


def value(parent, name):
    element = child(parent, name)
    return (element.text or "").strip() if element is not None else ""


class Model41RepositoryContractTest(unittest.TestCase):

    def test_all_model41_parents_use_full_gav_without_relative_path(self):
        violations = []
        for pom in sorted(ROOT.rglob("pom.xml")):
            if ".git" in pom.parts or "target" in pom.parts:
                continue
            project = ET.parse(pom).getroot()
            if value(project, "modelVersion") != "4.1.0":
                continue
            parent = child(project, "parent")
            if parent is None:
                continue
            coordinates = tuple(value(parent, name) for name in ("groupId", "artifactId", "version"))
            if child(parent, "relativePath") is not None or not all(coordinates):
                violations.append(pom.relative_to(ROOT).as_posix())

        self.assertEqual(violations, [])

    def test_flatten_plugin_has_governed_version(self):
        project = ET.parse(ROOT / "pom.xml").getroot()
        plugins = project.findall("./{*}build/{*}plugins/{*}plugin")
        flatten = next(plugin for plugin in plugins if value(plugin, "artifactId") == "flatten-maven-plugin")

        self.assertEqual(value(flatten, "version"), "${maven-flatten-plugin.version}")


class Model41AllowlistContractTest(unittest.TestCase):

    PROJECT = """\
<project xmlns="http://maven.apache.org/POM/4.1.0">
  <modelVersion>4.1.0</modelVersion>
  {parent}
  <groupId>io.ddd4j.quarkus</groupId>
  <artifactId>{artifact_id}</artifactId>
  <version>1</version>
</project>
"""

    def write_project(self, root, relative, artifact_id, parent=""):
        path = root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(self.PROJECT.format(parent=parent, artifact_id=artifact_id), encoding="utf-8")

    def test_non_default_parent_path_requires_exact_allowlist(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self.write_project(root, "pom.xml", "root")
            self.write_project(root, "parents/pom.xml", "parent")
            self.write_project(root, "child/pom.xml", "child", """
  <parent>
    <groupId>io.ddd4j.quarkus</groupId>
    <artifactId>parent</artifactId>
    <version>1</version>
  </parent>""")

            errors = verify(root)

            self.assertTrue(any("default parent path mismatch is not allowlisted" in error for error in errors))
            self.assertEqual(verify(root, {"child/pom.xml"}), [])

    def test_stale_allowlist_entry_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self.write_project(root, "pom.xml", "root")

            errors = verify(root, {"removed/pom.xml"})

            self.assertTrue(any("allowlist entry no longer matches" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
