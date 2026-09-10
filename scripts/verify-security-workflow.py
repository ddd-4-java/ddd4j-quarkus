#!/usr/bin/env python3
"""Verify that dependency scanning is an explicit, aggregate CI gate."""

from __future__ import annotations

import sys
from pathlib import Path


def require(text: str, fragment: str, label: str) -> None:
    if fragment not in text:
        raise ValueError(f"security workflow is missing {label}: {fragment}")


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    workflow = (root / ".github/workflows/security.yml").read_text(encoding="utf-8")
    regular_ci = (root / ".github/workflows/ci.yml").read_text(encoding="utf-8")
    pom = (root / "pom.xml").read_text(encoding="utf-8")

    expected_java = "21" if "<revision>4.0.x." in pom else "17"
    requirements = {
        "branch JDK": f"java-version: '{expected_java}'",
        "Maven settings secret": "secrets.MAVEN_SETTINGS_XML",
        "aggregate Dependency-Check goal": "dependency-check-maven:12.2.2:aggregate",
        "vulnerability database update goal": "dependency-check-maven:12.2.2:update-only",
        "inherited skip override": "-Ddependency-check.skip=false",
        "offline aggregate scan": "-DautoUpdate=false",
        "high-severity failure threshold": "-DfailBuildOnCVSS=7",
        "NVD key environment binding": "-DnvdApiKeyEnvironmentVariable=NVD_API_KEY",
        "required NVD key secret": "NVD_API_KEY organization secret is missing",
        "NVD retry policy": "-DnvdMaxRetryCount=10",
        "vulnerability database restore": "actions/cache/restore@v4",
        "vulnerability database save": "actions/cache/save@v4",
        "aggregate CycloneDX SBOM": "cyclonedx-maven-plugin:2.9.2:makeAggregateBom",
        "security report artifact": "name: security-reports",
    }
    try:
        for label, fragment in requirements.items():
            require(workflow, fragment, label)
        if workflow.count("dependency-check-maven:12.2.2:aggregate") != 1:
            raise ValueError("Dependency-Check must run exactly once as an aggregate goal")
        if "dependency-check-maven" in regular_ci:
            raise ValueError("Dependency-Check must not repeat in the regular module CI workflow")
    except ValueError as exc:
        print(exc, file=sys.stderr)
        return 1

    print(f"security workflow verified: aggregate scan, CVSS 7 gate, SBOM, Java {expected_java}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
