#!/usr/bin/env python3
"""Fail CI when Maven reports contain no executed tests or omit required classes."""

from __future__ import annotations

import argparse
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def report_files(roots: list[Path]) -> list[Path]:
    reports: set[Path] = set()
    for root in roots:
        reports.update(root.glob("**/target/surefire-reports/TEST-*.xml"))
        reports.update(root.glob("**/target/failsafe-reports/TEST-*.xml"))
    return sorted(reports)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--require-class",
        action="append",
        default=[],
        help="fully qualified test class that must have at least one non-skipped testcase",
    )
    parser.add_argument(
        "--require-test",
        action="append",
        default=[],
        help="fully qualified Class#method that must execute without being skipped",
    )
    parser.add_argument("roots", nargs="+", type=Path)
    args = parser.parse_args()

    files = report_files(args.roots)
    if not files:
        print("no Surefire or Failsafe XML reports found", file=sys.stderr)
        return 1

    tests = skipped = failures = errors = 0
    executed_by_class: dict[str, int] = {}
    executed_tests: set[str] = set()
    for report in files:
        try:
            root = ET.parse(report).getroot()
        except ET.ParseError as exc:
            print(f"invalid test report {report}: {exc}", file=sys.stderr)
            return 1
        for testcase in root.iter("testcase"):
            tests += 1
            classname = testcase.get("classname", "")
            is_skipped = testcase.find("skipped") is not None
            if is_skipped:
                skipped += 1
            else:
                executed_by_class[classname] = executed_by_class.get(classname, 0) + 1
                executed_tests.add(f"{classname}#{testcase.get('name', '')}")
            failures += len(testcase.findall("failure"))
            errors += len(testcase.findall("error"))

    executed = tests - skipped
    if executed <= 0:
        print("zero executed tests across Surefire and Failsafe reports", file=sys.stderr)
        return 1

    for required_class in args.require_class:
        if executed_by_class.get(required_class, 0) <= 0:
            print(
                f"required test class has no executed tests: {required_class}",
                file=sys.stderr,
            )
            return 1

    for required_test in args.require_test:
        if required_test not in executed_tests:
            print(f"required test method was not executed: {required_test}", file=sys.stderr)
            return 1

    print(
        f"test reports verified: files={len(files)} tests={tests} executed={executed} "
        f"skipped={skipped} failures={failures} errors={errors}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
