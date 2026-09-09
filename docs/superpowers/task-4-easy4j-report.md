# P5-B0 Task 4 Easy4J Structural Gate Report

## Scope

- Branch and baseline: `feature/4.0.x` at `5ab77ee`.
- Maven contract: Java 21 with Maven `4.0.0-rc-6`, Model `4.1.0` and active `<modules>`.
- This task aligns extension coordinates with the ddd4j 3.0.x BOM; it does not run the full dual-branch release gate.

## RED Baseline

1. The tracked-source scan found eight occurrences of the retired predecessor extension group: two local dependency-management entries, one QR-code leaf dependency, one Jackson Javadoc reference, and the P5-B0 plan/spec gate text.
2. The system Maven was Maven 3 and therefore rejected Model 4.1; Maven `4.0.0-rc-6` was used for the dependency-tree contract instead.
3. Before this change, the Jackson leaf carried a duplicate explicit `3.0.x.20260630-SNAPSHOT` version while the QR-code leaf resolved the retired group.

## Governing Evidence

The consumed `io.ddd4j:ddd4j-dependencies:3.0.x.20260630-SNAPSHOT` BOM manages both extension artifacts under `io.github.easy4j`:

| Artifact | BOM-managed version | Leaf policy |
|---|---|---|
| `jackson-extension` | `3.0.x.20260630-SNAPSHOT` | versionless direct dependency |
| `zxing-extension` | `3.0.x.20260630-SNAPSHOT` | versionless direct dependency |

The sibling 3.3.x commit `06c7222` was used only as the reviewed governance precedent. This 4.0.x change preserves the Java 21/ddd4j 3.0.x BOM contract rather than reusing the 3.3.x dependency version line.

## Implemented Change

1. Removed the obsolete local extension management entries from `ddd4j-quarkus-dependencies`.
2. Changed the QR-code leaf to the active Easy4J group without a leaf version.
3. Kept the Jackson leaf active but made it versionless, so its version comes from the ddd4j 3.0.x BOM.
4. Revised Jackson wording to preserve the self-contained Jackson 2 compatibility rationale without retaining the retired coordinate.
5. Rephrased the P5-B0 plan and specification gate so the tracked-source invariant is enforceable without embedding the retired coordinate.

## GREEN Evidence

| Check | Result |
|---|---|
| Maven 4 QR-code dependency tree | `io.github.easy4j:zxing-extension:3.0.x.20260630-SNAPSHOT:compile`; `BUILD SUCCESS` |
| Maven 4 Jackson dependency tree | `io.github.easy4j:jackson-extension:3.0.x.20260630-SNAPSHOT:compile`; `BUILD SUCCESS` |
| Java 21 focused tests | `JacksonQuarkusTest`: 4 tests, 0 failures/errors; `QrCodeQuarkusTest`: 2 tests, 0 failures/errors; `BUILD SUCCESS` |
| Tracked-source retirement scan | no matches |
| Maven aggregation shape | Model `4.1.0` remains in root/BOM/dependencies/parent/extensions POMs; active aggregation remains `<modules>` with no active `<subprojects>` element |
| Patch hygiene | `git diff --check` exited 0 |

Commands used Maven `4.0.0-rc-6` with `-Denforcer.skip=true` and `-pl` for the two extension leaves plus their reactor ancestors. This is focused structural and module-test evidence, not a replacement for the full Task 4 reactor gate.

The full P5-B0 dual-branch verification, CI, publication, and clean-cache consumption remain outside this structural task.
