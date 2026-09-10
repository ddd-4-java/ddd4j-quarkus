#!/usr/bin/env python3
"""Verify Quarkus BOM ownership in every reactor leaf effective model."""
from __future__ import annotations
import subprocess, sys, tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

Coordinate = tuple[str, str, str, str]

def parse(path: Path) -> ET.Element:
    root = ET.parse(path).getroot()
    for node in root.iter(): node.tag = node.tag.rsplit("}", 1)[-1]
    return root

def value(node: ET.Element, name: str, default: str = "") -> str:
    child = node.find(name)
    return default if child is None or child.text is None else child.text.strip()

def coordinate(node: ET.Element) -> Coordinate:
    return value(node, "groupId"), value(node, "artifactId"), value(node, "type", "jar"), value(node, "classifier")

def project_key(project: ET.Element) -> tuple[str, str]:
    parent = project.find("parent")
    group_id = value(project, "groupId") or (value(parent, "groupId") if parent is not None else "")
    return group_id, value(project, "artifactId")

def index_projects(projects: list[ET.Element]) -> dict[tuple[str, str], ET.Element]:
    result: dict[tuple[str, str], ET.Element] = {}
    for project in projects:
        key = project_key(project)
        if key in result:
            raise ValueError(f"Duplicate reactor project coordinate: {key[0]}:{key[1]}")
        result[key] = project
    return result

def versions(nodes: list[ET.Element]) -> dict[Coordinate, str]:
    result: dict[Coordinate, str] = {}
    for node in nodes:
        key, version = coordinate(node), value(node, "version")
        if all(key[:2]) and version and key not in result: result[key] = version
    return result

def managed_versions(path: Path) -> dict[Coordinate, str]:
    return versions(parse(path).findall("dependencyManagement/dependencies/dependency"))

def reactor_leaves(repository: Path) -> list[Path]:
    visited: set[Path] = set(); leaves: list[Path] = []
    def visit(pom: Path) -> None:
        pom = pom.resolve()
        if pom in visited: return
        visited.add(pom)
        modules = [node.text.strip() for node in parse(pom).findall("modules/module") if node.text]
        if not modules:
            if value(parse(pom), "packaging", "jar") != "pom": leaves.append(pom)
            return
        for module in modules:
            child = pom.parent / module / "pom.xml"
            if not child.is_file(): raise RuntimeError(f"Missing reactor module POM: {child}")
            visit(child)
    visit(repository / "pom.xml")
    return leaves

def maven_args(repository: Path) -> list[str]:
    # Maven reads both .mvn/maven.config and MAVEN_ARGS itself. Keeping the child
    # command unmodified preserves the caller's settings and local-repository contract.
    return []

def run_maven(repository: Path, args: list[str], *goals: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run([str(repository / "mvnw"), *args, *goals], cwd=repository, check=True, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

def main() -> int:
    module = Path(__file__).resolve().parents[3]; repository = module.parent; pom = module / "pom.xml"
    root = parse(pom); properties = root.find("properties"); assert properties is not None
    quarkus_version = value(properties, "quarkus-bom.version")
    imports = [(value(node, "groupId"), value(node, "artifactId")) for node in root.findall("dependencyManagement/dependencies/dependency") if value(node, "type", "jar") == "pom" and value(node, "scope") == "import"]
    if not imports or imports[0] != ("io.quarkus.platform", "quarkus-bom"):
        print(f"Quarkus BOM must be the first imported BOM; found {imports[:3]}", file=sys.stderr); return 1
    args = maven_args(repository)
    failures: list[str] = []; leaves = reactor_leaves(repository)
    with tempfile.TemporaryDirectory(prefix="ddd4j-quarkus-bom-") as temp_dir:
        effective_pom = Path(temp_dir) / "reactor-effective-poms.xml"
        run_maven(repository, args, "-q", "-f", str(repository / "pom.xml"), "help:effective-pom", f"-Doutput={effective_pom}")
        evaluated = run_maven(repository, args, "-q", "-N", "-f", str(pom), "help:evaluate", "-Dexpression=settings.localRepository", "-DforceStdout").stdout.strip().splitlines()
        local_repository = Path(evaluated[-1].split()[-1])
        quarkus_pom = local_repository / "io/quarkus/platform/quarkus-bom" / quarkus_version / f"quarkus-bom-{quarkus_version}.pom"
        if not quarkus_pom.is_file():
            print(f"Maven did not cache the Quarkus BOM in {local_repository}", file=sys.stderr); return 1
        platform = managed_versions(quarkus_pom)
        aggregate = parse(effective_pom)
        try:
            projects = index_projects(aggregate.findall("project"))
        except ValueError as failure:
            print(str(failure), file=sys.stderr); return 1
        expected_revision = value(properties, "revision")
        for leaf in leaves:
            key = project_key(parse(leaf))
            effective_root = projects.get(key)
            if effective_root is None:
                failures.append(f"{leaf.relative_to(repository)}: current reactor effective model is missing")
                continue
            if value(effective_root, "version") != expected_revision:
                failures.append(f"{leaf.relative_to(repository)}: expected reactor revision {expected_revision}, actual={value(effective_root, 'version')}")
                continue
            managed = versions(effective_root.findall("dependencyManagement/dependencies/dependency"))
            missing = sorted(set(platform) - set(managed))
            conflicts = sorted((key, expected, managed[key]) for key, expected in platform.items() if key in managed and managed[key] != expected)
            direct = versions(effective_root.findall("dependencies/dependency"))
            direct_conflicts = sorted((key, platform[key], actual) for key, actual in direct.items() if key in platform and actual != platform[key])
            if missing: failures.append(f"{leaf.relative_to(repository)}: missing {len(missing)} platform coordinates; first={':'.join(missing[0])}")
            failures.extend(f"{leaf.relative_to(repository)}: {':'.join(key)} expected={expected} actual={actual}" for key, expected, actual in [*conflicts, *direct_conflicts])
    if failures:
        print("BOM arbitration failures:\n" + "\n".join(f"  {failure}" for failure in failures), file=sys.stderr); return 1
    print(f"BOM arbitration verified in {len(leaves)} reactor leaves: Quarkus {quarkus_version}, Testcontainers 2.0.5")
    return 0

if __name__ == "__main__": raise SystemExit(main())
