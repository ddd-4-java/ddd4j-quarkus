#!/usr/bin/env python3
"""Verify that the Quarkus platform BOM owns its ecosystem versions."""

from __future__ import annotations

import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

def value(node: ET.Element, name: str, default: str = "") -> str:
    child = node.find(name)
    return default if child is None or child.text is None else child.text.strip()


def coordinate(node: ET.Element) -> tuple[str, str, str, str]:
    return (
        value(node, "groupId"),
        value(node, "artifactId"),
        value(node, "type", "jar"),
        value(node, "classifier"),
    )


def managed_versions(path: Path) -> dict[tuple[str, str, str, str], str]:
    root = ET.parse(path).getroot()
    for node in root.iter():
        node.tag = node.tag.rsplit("}", 1)[-1]
    result: dict[tuple[str, str, str, str], str] = {}
    nodes = root.findall("dependencyManagement/dependencies/dependency")
    for node in nodes:
        key = coordinate(node)
        version = value(node, "version")
        if all(key[:2]) and version and key not in result:
            result[key] = version
    return result


def main() -> int:
    module = Path(__file__).resolve().parents[3]
    repository = module.parent
    pom = module / "pom.xml"
    root = ET.parse(pom).getroot()
    for node in root.iter():
        node.tag = node.tag.rsplit("}", 1)[-1]
    properties = root.find("properties")
    assert properties is not None
    quarkus_version = value(properties, "quarkus-bom.version")

    imports = [
        (value(node, "groupId"), value(node, "artifactId"))
        for node in root.findall("dependencyManagement/dependencies/dependency")
        if value(node, "type", "jar") == "pom" and value(node, "scope") == "import"
    ]
    expected_first = ("io.quarkus.platform", "quarkus-bom")
    if not imports or imports[0] != expected_first:
        print(f"Quarkus BOM must be the first imported BOM; found {imports[:3]}", file=sys.stderr)
        return 1

    quarkus_pom = (
        Path.home()
        / ".m2/repository/io/quarkus/platform/quarkus-bom"
        / quarkus_version
        / f"quarkus-bom-{quarkus_version}.pom"
    )
    with tempfile.TemporaryDirectory(prefix="ddd4j-quarkus-bom-") as temp_dir:
        effective_pom = Path(temp_dir) / "effective-pom.xml"
        command = [
            str(repository / "mvnw"),
            "-q",
            "-f",
            str(pom),
            "help:effective-pom",
            "-Dverbose",
            f"-Doutput={effective_pom}",
        ]
        subprocess.run(command, cwd=repository, check=True)
        effective = managed_versions(effective_pom)

    if not quarkus_pom.is_file():
        print(f"Maven did not cache the Quarkus BOM: {quarkus_pom}", file=sys.stderr)
        return 1

    platform = managed_versions(quarkus_pom)
    conflicts = []
    for key, expected in platform.items():
        actual = effective.get(key)
        if actual is not None and actual != expected:
            conflicts.append((key, expected, actual))

    if conflicts:
        print("Unexpected Quarkus platform version conflicts:", file=sys.stderr)
        for key, expected, actual in conflicts:
            print(f"  {':'.join(key)} expected={expected} actual={actual}", file=sys.stderr)
        return 1

    assertions = {
        ("io.quarkus", "quarkus-core", "jar", ""): quarkus_version,
        ("org.testcontainers", "testcontainers", "jar", ""): "2.0.5",
    }
    for key, expected in assertions.items():
        actual = effective.get(key)
        if actual != expected:
            print(f"Version assertion failed for {':'.join(key)}: {actual} != {expected}", file=sys.stderr)
            return 1

    print(f"BOM arbitration verified: Quarkus {quarkus_version}, Testcontainers 2.0.5")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
