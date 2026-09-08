# P5-A Stable Publish and Consumer Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `feature/4.0.x` build and test against remotely available ddd4j `3.0.x.20260630-SNAPSHOT` artifacts without mutating a ddd4j checkout, while proving Web and License consumer behavior and making GitHub Actions truthful.

**Architecture:** Keep P5-A within the existing module topology. Centralize the exact upstream version in the root/BOM/dependencies chain, add a minimal remote-consumer Maven fixture for empty-cache resolution, finish Web and License behavior contracts, and replace the source-mutating CI action with a settings-and-remote-resolution action. Runtime/deployment splitting is deferred to P5-B.

**Tech Stack:** Java 21, Maven 4.0.0-rc-6, Maven Model 4.1.0, Quarkus 3.38.2, ddd4j 3.0.x.20260630-SNAPSHOT, Testcontainers 2.0.5, JUnit Jupiter, RestAssured, GitHub Actions, actionlint

**Spec:** `docs/superpowers/specs/2026-09-08-quarkus-production-extension-convergence-design.md`

## Execution Record — 2026-09-09

**Status:** P5-A 本地完成，P5-B–E 待实施。下列勾选对应本地实施/验收，GitHub Actions 远端 gate 未完成。

| Task | Local evidence |
|---|---|
| 1 | `e405c32` + `a303675`：版本与 BOM、独立远端消费者、过期注释收敛；最终空仓命令再次 exit 0 |
| 2 | `5e51f13` + `6d07329`：Web consumer 与同线程响应后清理探针；最终模块 4 tests，0 failures/errors/skips |
| 3 | `6bd01bc` + `584f529`：真实 License 安装/验签及每次运行独立、权限受限、应用关闭后清理的临时夹具；最终模块 3 tests，0 failures/errors/skips |
| 4 | `10bccd9` + `278f119`：已发布依赖消费 CI、真实 lint、经临时文件校验后原子替换 settings；最终 actionlint exit 0 |
| 5 | 新空仓 go-offline、Web、License、clean verify、XML 汇总、结构扫描、六份文档与本地提交检查 |

Final commands and observations:

- `./mvnw -B -U -f .github/maven/ddd4j-remote-consumer/pom.xml -Dmaven.repo.local=/tmp/ddd4j-p5a-final.xcqRbQ -Dmaven.resolver.transport=wagon dependency:go-offline`: exit 0, 1:39; repository was freshly created with `mktemp`. Required runtime/web/license POM/JAR timestamps: `3.0.x.20260630-20260908.152610-5`.
- `./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-web -am test`: exit 0, 4 tests, 8.307 s.
- `./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-auth/ddd4j-quarkus-auth-license -am test`: exit 0, 3 tests, 8.147 s.
- `./mvnw -B clean verify -Denforcer.skip=true`: exit 0, 62/62 reactor modules, 9:57; finished 2026-09-09 02:09:38 +08:00.
- Final XML: 47 Surefire suites, 0 Failsafe suites, **141 tests / 0 failures / 0 errors / 3 skipped**. The total includes all skips.
- Skip methods: `MicaMqttQuarkusIntegrationTest#shouldPublishAndConsumeOrderCreatedEventEndToEnd` (macOS arm64 AIO known defect), `OnsQuarkusIntegrationTest#shouldPublishAndConsumeOrderCreatedEventEndToEnd` (commercial protocol without Testcontainers image), `SecurityQuarkusConfigTest#subjectProviderExposedAsCdiBeanAndRegisteredInSubjectKit` (deprecated module). Full package names and literal XML reasons are in spec section 9.
- `actionlint .github/workflows/ci.yml` and `git diff --check`: exit 0. Stale upstream/source-mutation/lint-placeholder scan: no matches. The subprojects text scan matches only the existing root POM:47 comment; XML parsing finds zero actual elements in all six specified aggregation POMs.

Execution refinements retained after review: a second HTTP request alone is not a deterministic cleanup proof, so Task 2 added a same-thread post-response probe; Task 3 isolated fixtures and their shutdown cleanup; Task 4 validates settings before atomic replacement. The relevant RED/GREEN and review evidence remains in the local Task 1–4 reports and commits.

The remote fixture proves upstream closure download; the full reactor uses the configured developer repository and is not an empty-cache whole-reactor proof. Enforcer was skipped. Existing effective-model, release-metadata RFC9457, wrapper settings parsing, read-only resources parameter and unrecognized test-configuration warnings remain. TDMQ memory fallback is not cloud-service acceptance.

No push/deploy/tag/workflow dispatch was performed. GitHub Actions execution and organization-secret availability remain unverified; authorization for push/deploy is a separate next checkpoint. P5-B–E and 3.3.x/Native/Dev Mode remain pending; Maven 4 `subprojects` remains upstream-blocked. The unrelated MQTT directory is preserved untracked.

## Global Constraints

- Work only on `feature/4.0.x`; do not create or use a Git worktree and do not switch branches during P5-A.
- Preserve every existing uncommitted POM, Web, and License candidate change; adjust overlapping files only with minimal patches.
- Preserve `ddd4j-quarkus-mq/ddd4j-quarkus-mq-mqtt/ddd4j-mq-720a11be-fdbd-4905-83e5-008a70164f2e-tcplocalhost65400/`; never stage, delete, move, or edit it.
- Use `revision=4.0.x.20260630-SNAPSHOT`, `ddd4j.version=3.0.x.20260630-SNAPSHOT`, Quarkus BOM/plugin `3.38.2`, Testcontainers `2.0.5`, Java 21, Maven Model 4.1.0, and temporary `modules/module` aggregation.
- Do not introduce `subprojects/subproject`; it remains blocked until Quarkus WorkspaceLoader supports Maven Model 4.1 aggregation.
- Do not split runtime/deployment artifacts, change Data/Auth/MQ production semantics, or touch `feature/3.3.x` in this plan.
- Do not print `MAVEN_SETTINGS_XML`, Maven usernames, passwords, repository authorization headers, or decoded settings content.
- Use Maven Wagon transport for authenticated Aliyun snapshot resolution: `-Dmaven.resolver.transport=wagon`.
- Do not push or deploy without a new explicit authorization after local verification.
- A build, deploy exit code, injected Bean, HTTP 200, or skipped test is not sufficient acceptance evidence by itself.

---

## File Responsibility Map

### Version and remote-consumer contract

- `pom.xml`: authoritative 4.0.x upstream ddd4j version and Quarkus line.
- `ddd4j-quarkus-dependencies/pom.xml`: imported ddd4j BOM, Quarkus BOM, Testcontainers BOM, and managed upstream runtime/web/license coordinates.
- `ddd4j-quarkus-bom/pom.xml`: public dependency-management surface for ddd4j and ddd4j-quarkus artifacts.
- `.github/maven/ddd4j-remote-consumer/pom.xml`: minimal standalone POM that resolves required upstream artifacts from an empty Maven repository.

### Web behavior contract

- `ddd4j-quarkus-web/pom.xml`: consumes managed `ddd4j-web-quarkus` and `ddd4j-runtime-quarkus` coordinates.
- `ddd4j-quarkus-web/src/test/java/io/ddd4j/quarkus/web/Ddd4jQuarkusWebConsumerTest.java`: tenant/request-id and post-request cleanup contract.
- `ddd4j-quarkus-web/src/test/java/io/ddd4j/quarkus/web/Ddd4jQuarkusWebContractResource.java`: observes current ThreadContext only; contains no test setup behavior.

### License behavior contract

- `ddd4j-quarkus-auth/ddd4j-quarkus-auth-license/pom.xml`: consumes the BOM-managed license artifact.
- `ddd4j-quarkus-auth/ddd4j-quarkus-auth-license/src/test/java/io/ddd4j/quarkus/auth/license/LicenseEnabledEndToEndQuarkusTest.java`: prepares a signed license before CDI startup and verifies install/signature behavior.

### CI

- Create `.github/actions/configure-maven/action.yml`: decode the organization secret safely and prove remote ddd4j resolution.
- Delete `.github/actions/install-ddd4j/action.yml`: remove checkout/build/source-mutation behavior.
- `.github/workflows/ci.yml`: execute real actionlint, configure Maven, run the root gate and broker matrix.

### Documentation

- `README.md`, `CONTRIBUTING.md`, `docs/CAPABILITY-ALIGNMENT.md`, `docs/superpowers/README.md`: record only the evidence observed in P5-A.
- P5 spec and this plan: update status/checkmarks only after the proving command succeeds.

---

### Task 1: Lock the upstream version and prove empty-cache resolution

**Files:**
- Modify: `pom.xml`
- Modify: `ddd4j-quarkus-dependencies/pom.xml`
- Modify: `ddd4j-quarkus-bom/pom.xml`
- Create: `.github/maven/ddd4j-remote-consumer/pom.xml`

**Interfaces:**
- Consumes: Aliyun Maven settings with server id `2624322-snapshot-3EoOv3`.
- Produces: a standalone resolution contract for `ddd4j-runtime-quarkus`, `ddd4j-web-quarkus`, and `ddd4j-extension-license` at `3.0.x.20260630-SNAPSHOT`.

- [x] **Step 1: Capture the protected baseline**

Run:

```bash
git branch --show-current
git status --short --branch
git diff -- pom.xml ddd4j-quarkus-dependencies/pom.xml ddd4j-quarkus-bom/pom.xml
./mvnw --version
```

Expected: branch `feature/4.0.x`, Maven `4.0.0-rc-6`, Java 21, existing candidate diffs visible, and the protected MQTT directory untracked.

- [x] **Step 2: Add the standalone remote-consumer POM**

Create `.github/maven/ddd4j-remote-consumer/pom.xml` with this complete content:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>io.ddd4j.quarkus.verification</groupId>
    <artifactId>ddd4j-remote-consumer</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <properties>
        <ddd4j.version>3.0.x.20260630-SNAPSHOT</ddd4j.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.ddd4j</groupId>
                <artifactId>ddd4j-dependencies</artifactId>
                <version>${ddd4j.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>io.ddd4j</groupId>
            <artifactId>ddd4j-runtime-quarkus</artifactId>
            <version>${ddd4j.version}</version>
        </dependency>
        <dependency>
            <groupId>io.ddd4j</groupId>
            <artifactId>ddd4j-web-quarkus</artifactId>
            <version>${ddd4j.version}</version>
        </dependency>
        <dependency>
            <groupId>io.ddd4j</groupId>
            <artifactId>ddd4j-extension-license</artifactId>
            <version>${ddd4j.version}</version>
        </dependency>
    </dependencies>
</project>
```

This fixture intentionally uses Model 4.0.0 so the test isolates repository consumption from the P5-E Maven Model 4.1 aggregation issue.

- [x] **Step 3: Run the empty-cache contract before changing remaining POM entries**

Run:

```bash
P5A_REMOTE_REPO="$(mktemp -d /tmp/ddd4j-p5a-remote.XXXXXX)"
./mvnw -B -U -f .github/maven/ddd4j-remote-consumer/pom.xml \
  -Dmaven.repo.local="$P5A_REMOTE_REPO" \
  -Dmaven.resolver.transport=wagon \
  dependency:go-offline
```

Expected: dependencies are downloaded from configured remotes into the new directory and the build succeeds. If an artifact is absent, preserve the exact missing coordinate and stop this task; do not fall back to the developer repository.

- [x] **Step 4: Normalize the three dependency-management surfaces**

Ensure all three POMs resolve these literal values:

```xml
<revision>4.0.x.20260630-SNAPSHOT</revision>
<ddd4j.version>3.0.x.20260630-SNAPSHOT</ddd4j.version>
<quarkus-bom.version>3.38.2</quarkus-bom.version>
```

In `ddd4j-quarkus-dependencies/pom.xml`, retain Testcontainers BOM `2.0.5` before the ddd4j BOM and manage:

```xml
<dependency>
    <groupId>io.ddd4j</groupId>
    <artifactId>ddd4j-extension-license</artifactId>
    <version>${ddd4j.version}</version>
</dependency>
<dependency>
    <groupId>io.ddd4j</groupId>
    <artifactId>ddd4j-runtime-quarkus</artifactId>
    <version>${ddd4j.version}</version>
</dependency>
<dependency>
    <groupId>io.ddd4j</groupId>
    <artifactId>ddd4j-web-quarkus</artifactId>
    <version>${ddd4j.version}</version>
</dependency>
```

Mirror the same three upstream coordinates in `ddd4j-quarkus-bom/pom.xml`. Remove stale comments that still claim `3.0.x.20260730-SNAPSHOT` is the active line.

- [x] **Step 5: Verify effective versions and resolution**

Run:

```bash
rg -n '<revision>|<ddd4j.version>|<quarkus-bom.version>|<testcontainers-bom.version>' \
  pom.xml ddd4j-quarkus-dependencies/pom.xml ddd4j-quarkus-bom/pom.xml \
  .github/maven/ddd4j-remote-consumer/pom.xml
./mvnw -B -N validate -Denforcer.skip=true
```

Expected: no `3.0.x.20260730-SNAPSHOT` reference remains in active POM/config content; root validation succeeds.

- [x] **Step 6: Commit only Task 1 files**

```bash
git add pom.xml ddd4j-quarkus-dependencies/pom.xml ddd4j-quarkus-bom/pom.xml \
  .github/maven/ddd4j-remote-consumer/pom.xml
git diff --cached --check
git commit -m "build: align Quarkus 4 with published ddd4j snapshot"
```

---

### Task 2: Complete the Web consumer and cleanup contract

**Files:**
- Modify: `ddd4j-quarkus-web/pom.xml`
- Modify: `ddd4j-quarkus-web/src/test/java/io/ddd4j/quarkus/web/Ddd4jQuarkusWebConsumerTest.java`
- Create: `ddd4j-quarkus-web/src/test/java/io/ddd4j/quarkus/web/Ddd4jQuarkusWebContractResource.java` (already present as an untracked candidate; preserve and review it)

**Interfaces:**
- Consumes: `io.ddd4j:ddd4j-web-quarkus` request filter and `ThreadContext` propagation.
- Produces: `GET /ddd4j/contract`, returning JSON field `tenantId`, plus assertions for `X-Request-Id` and request-scope cleanup.

- [x] **Step 1: Run the existing candidate contract unchanged**

Run:

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-web -am \
  -Dtest=Ddd4jQuarkusWebConsumerTest test
```

Expected: record the actual result. The candidate may already be GREEN because it was authored before this plan; do not revert it merely to manufacture RED.

- [x] **Step 2: Add the missing cleanup assertion first**

Extend `Ddd4jQuarkusWebConsumerTest` with a second request in the same test:

```java
given()
        .when().get("/ddd4j/contract")
        .then()
        .statusCode(200)
        .body("tenantId", nullValue());
```

Add:

```java
import static org.hamcrest.Matchers.nullValue;
```

The first request remains unchanged and must continue asserting tenant propagation and a non-empty request id.

- [x] **Step 3: Run the contract to establish RED or confirm pre-existing cleanup**

Run the Task 2 Step 1 command again.

Expected: if tenant state leaks, the second request fails with the previous value `tenant-consumer`; if it already passes, record that the upstream filter already supplies cleanup and treat the new assertion as a regression lock.

- [x] **Step 4: Apply only the minimal consumer wiring correction if required**

The `ddd4j-quarkus-web/pom.xml` dependencies must be versionless and managed by Task 1:

```xml
<dependency>
    <groupId>io.ddd4j</groupId>
    <artifactId>ddd4j-web-quarkus</artifactId>
</dependency>
<dependency>
    <groupId>io.ddd4j</groupId>
    <artifactId>ddd4j-runtime-quarkus</artifactId>
</dependency>
```

Do not copy the filter from ddd4j into this repository. If the filter is not discovered, add the required dependency indexing/configuration in the consumer module rather than creating a second implementation.

- [x] **Step 5: Verify Web behavior**

Run:

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-web -am test
```

Expected: health, tenant propagation, request-id, and cleanup tests all execute with zero failures and zero errors.

- [x] **Step 6: Commit only Web files**

```bash
git add ddd4j-quarkus-web/pom.xml \
  ddd4j-quarkus-web/src/test/java/io/ddd4j/quarkus/web/Ddd4jQuarkusWebConsumerTest.java \
  ddd4j-quarkus-web/src/test/java/io/ddd4j/quarkus/web/Ddd4jQuarkusWebContractResource.java
git diff --cached --check
git commit -m "test(web): verify upstream Quarkus request lifecycle"
```

---

### Task 3: Finish the real License Quarkus lifecycle contract

**Files:**
- Modify: `ddd4j-quarkus-auth/ddd4j-quarkus-auth-license/pom.xml`
- Modify: `ddd4j-quarkus-auth/ddd4j-quarkus-auth-license/src/test/java/io/ddd4j/quarkus/auth/license/LicenseEnabledEndToEndQuarkusTest.java`

**Interfaces:**
- Consumes: BOM-managed `io.ddd4j:ddd4j-extension-license` and Quarkus test profile configuration.
- Produces: a signed temporary license available before `LicenseVerify` CDI construction, followed by install and verify assertions.

- [x] **Step 1: Run the candidate License test unchanged**

```bash
./mvnw -B -Denforcer.skip=true \
  -pl ddd4j-quarkus-auth/ddd4j-quarkus-auth-license -am \
  -Dtest=LicenseEnabledEndToEndQuarkusTest test
```

Expected: record whether `generateLicense`, `isInstallSuccess`, and `verify` succeed. Do not weaken assertions if the test fails.

- [x] **Step 2: Verify startup ordering in the candidate test**

The `LicenseEnabledProfile#getConfigOverrides()` method must invoke fixture preparation before returning properties:

```java
try {
    generateKeystoreAndLicense();
} catch (Exception exception) {
    throw new IllegalStateException("无法准备 Quarkus License 端到端测试夹具", exception);
}
```

The test method must contain:

```java
assertThat(licenseVerify).isNotNull();
assertThat(licenseVerify.isInstallSuccess()).isTrue();
assertThat(licenseVerify.verify()).isTrue();
```

- [x] **Step 3: Verify filesystem and secret boundaries**

Run:

```bash
rg -n 'java.io.tmpdir|license-path|store-pass|key-pass|System\.out|printStackTrace' \
  ddd4j-quarkus-auth/ddd4j-quarkus-auth-license/src/test/java
```

Expected: generated files are under `java.io.tmpdir`; passwords are test constants only; no key material or password is printed.

- [x] **Step 4: Normalize the License dependency**

The module POM must contain only the managed upstream dependency:

```xml
<dependency>
    <groupId>io.ddd4j</groupId>
    <artifactId>ddd4j-extension-license</artifactId>
</dependency>
```

Do not reintroduce a direct `truelicense-core` dependency unless a fresh dependency tree proves the upstream consumer POM is incomplete at `3.0.x.20260630-SNAPSHOT`.

- [x] **Step 5: Verify the complete License module**

```bash
./mvnw -B -Denforcer.skip=true \
  -pl ddd4j-quarkus-auth/ddd4j-quarkus-auth-license -am test
```

Expected: disabled and enabled profiles execute as designed; the enabled E2E test proves real install and verification.

- [x] **Step 6: Commit only License files**

```bash
git add ddd4j-quarkus-auth/ddd4j-quarkus-auth-license/pom.xml \
  ddd4j-quarkus-auth/ddd4j-quarkus-auth-license/src/test/java/io/ddd4j/quarkus/auth/license/LicenseEnabledEndToEndQuarkusTest.java
git diff --cached --check
git commit -m "test(auth): verify Quarkus license installation"
```

---

### Task 4: Replace source-mutating CI setup with remote resolution

**Files:**
- Create: `.github/actions/configure-maven/action.yml`
- Delete: `.github/actions/install-ddd4j/action.yml`
- Modify: `.github/workflows/ci.yml`
- Test: `.github/maven/ddd4j-remote-consumer/pom.xml`

**Interfaces:**
- Consumes: base64 `MAVEN_SETTINGS_XML` input.
- Produces: `$HOME/.m2/settings.xml` with mode 0600 and a verified empty-cache ddd4j dependency set; it never checks out or edits ddd4j source.

- [x] **Step 1: Capture the semantic CI RED checks**

Run:

```bash
rg -n 'actions/checkout|sed -i|Install ddd4j|Lint disabled|reviewdog/action-actionlint' \
  .github/actions/install-ddd4j/action.yml .github/workflows/ci.yml
```

Expected: current action checks out ddd4j and uses `sed -i`; workflow lint is an echo placeholder.

- [x] **Step 2: Create the settings-and-resolution composite action**

Create `.github/actions/configure-maven/action.yml`:

```yaml
name: 'Configure Maven and verify ddd4j snapshot'
description: >-
  Install MAVEN_SETTINGS_XML without logging credentials and verify the exact
  ddd4j snapshot from an empty Maven repository.

inputs:
  maven-settings-xml:
    description: 'Base64-encoded Maven settings.xml'
    required: true

runs:
  using: 'composite'
  steps:
    - name: Configure Maven settings
      shell: bash
      env:
        MAVEN_SETTINGS_XML: ${{ inputs.maven-settings-xml }}
      run: |
        if [ -z "$MAVEN_SETTINGS_XML" ]; then
          echo "::error::MAVEN_SETTINGS_XML organization secret is not configured"
          exit 1
        fi
        install -d -m 700 "$HOME/.m2"
        umask 077
        printf '%s' "$MAVEN_SETTINGS_XML" | base64 --decode > "$HOME/.m2/settings.xml"
        grep -q '<id>2624322-snapshot-3EoOv3</id>' "$HOME/.m2/settings.xml"
        chmod 600 "$HOME/.m2/settings.xml"

    - name: Verify remote ddd4j snapshot
      shell: bash
      run: |
        P5A_CI_REPO="$(mktemp -d "$RUNNER_TEMP/ddd4j-p5a-remote.XXXXXX")"
        ./mvnw -B -U -f .github/maven/ddd4j-remote-consumer/pom.xml \
          -Dmaven.repo.local="$P5A_CI_REPO" \
          -Dmaven.resolver.transport=wagon \
          dependency:go-offline
```

This action deliberately avoids parsing username/password with `sed` and avoids credential-bearing `curl` commands.

- [x] **Step 3: Update the workflow**

Replace the lint placeholder with:

```yaml
workflow-lint:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: reviewdog/action-actionlint@v1
```

In both Maven jobs, replace `./.github/actions/install-ddd4j` with:

```yaml
- name: Configure Maven and verify ddd4j snapshot
  uses: ./.github/actions/configure-maven
  with:
    maven-settings-xml: ${{ secrets.MAVEN_SETTINGS_XML }}
```

Remove stale comments about checking out, installing, or mutating ddd4j. Keep Java 21, all 13 broker entries, `fail-fast: false`, report upload, and the existing blocking job dependency.

- [x] **Step 4: Delete the old action after references are gone**

Remove `.github/actions/install-ddd4j/action.yml`, then run:

```bash
rg -n 'install-ddd4j|sed -i|checkout ddd4j|Lint disabled' .github
```

Expected: no matches.

- [x] **Step 5: Verify workflow syntax and security properties**

Run:

```bash
actionlint .github/workflows/ci.yml
rg -n 'MAVEN_SETTINGS_XML|configure-maven|reviewdog/action-actionlint|rocketmq|pulsar' \
  .github/actions/configure-maven/action.yml .github/workflows/ci.yml
rg -n 'username|password|Authorization|curl -u|continue-on-error|testcontainers.reuse' \
  .github/actions/configure-maven/action.yml .github/workflows/ci.yml || true
```

Expected: actionlint exits 0; required secret and critical brokers exist; forbidden credential parsing, continue-on-error, and reuse configuration are absent.

- [x] **Step 6: Commit only CI files**

```bash
git add .github/actions/configure-maven/action.yml .github/workflows/ci.yml
git add -u .github/actions/install-ddd4j/action.yml
git diff --cached --check
git commit -m "ci: verify published ddd4j snapshot without source mutation"
```

---

### Task 5: Run the P5-A completion gate and correct current documentation

**Files:**
- Modify: `README.md`
- Modify: `CONTRIBUTING.md`
- Modify: `docs/CAPABILITY-ALIGNMENT.md`
- Modify: `docs/superpowers/README.md`
- Modify: `docs/superpowers/specs/2026-09-08-quarkus-production-extension-convergence-design.md`
- Modify: `docs/superpowers/plans/2026-09-08-p5a-stable-publish-consumer-baseline.md`

**Interfaces:**
- Consumes: fresh remote-resolution, Web, License, full reactor, and actionlint results.
- Produces: an evidence-backed P5-A status that does not claim P5-B–E completion.

- [x] **Step 1: Re-run the empty-cache remote contract**

```bash
P5A_FINAL_REMOTE_REPO="$(mktemp -d /tmp/ddd4j-p5a-final.XXXXXX)"
./mvnw -B -U -f .github/maven/ddd4j-remote-consumer/pom.xml \
  -Dmaven.repo.local="$P5A_FINAL_REMOTE_REPO" \
  -Dmaven.resolver.transport=wagon \
  dependency:go-offline
```

Expected: exit 0 with the upstream artifacts resolved into the new directory.

- [x] **Step 2: Run focused behavior gates**

Run separately:

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-web -am test
./mvnw -B -Denforcer.skip=true \
  -pl ddd4j-quarkus-auth/ddd4j-quarkus-auth-license -am test
```

Expected: both commands exit 0; Web executes propagation and cleanup; License executes real installation and verification.

- [x] **Step 3: Run the complete reactor from clean output**

```bash
./mvnw -B clean verify -Denforcer.skip=true
```

Expected: all reactor modules succeed. Preserve warnings separately; `-Denforcer.skip=true` means Enforcer is not an accepted gate.

- [x] **Step 4: Count Surefire/Failsafe results from XML**

Run:

```bash
ruby -r rexml/document -e '
totals = Hash.new(0)
files = Dir.glob("**/target/{surefire,failsafe}-reports/TEST-*.xml")
files.each do |path|
  suite = REXML::Document.new(File.read(path)).root
  %w[tests failures errors skipped].each { |key| totals[key] += suite.attributes[key].to_i }
end
puts "suites=#{files.size} tests=#{totals["tests"]} failures=#{totals["failures"]} errors=#{totals["errors"]} skipped=#{totals["skipped"]}"
'
```

Expected: failures and errors are zero. Report skipped tests by class and reason; do not subtract them silently from totals.

- [x] **Step 5: Run final structural and workflow checks**

```bash
actionlint .github/workflows/ci.yml
rg -n '3\.0\.x\.20260730-SNAPSHOT|install-ddd4j|sed -i|Lint disabled' \
  pom.xml ddd4j-quarkus-bom/pom.xml ddd4j-quarkus-dependencies/pom.xml .github || true
rg -n '<subprojects>|<subproject>' \
  pom.xml ddd4j-quarkus-{auth,data,extensions,mq,samples}/pom.xml || true
git diff --check
git status --short --branch
```

Expected: stale upstream version and source mutation are absent; no subproject tags were introduced; only the protected MQTT directory may remain unrelated.

- [x] **Step 6: Update documentation from literal observed evidence**

Record:

- P5-A version matrix and exact test totals.
- Remote-consumer command and result.
- Web propagation/cleanup and License install/verify status.
- CI design now consumes published ddd4j artifacts and does not mutate source.
- P5-B–E remain pending.
- Maven 4 `subprojects` remains upstream-blocked.
- No push/deploy or GitHub Actions result exists until separately authorized and executed.

Change the P5 design status from `待规格确认` to `P5-A 本地完成，P5-B–E 待实施` only if Steps 1–5 all succeed.

- [x] **Step 7: Commit documentation and plan status**

```bash
git add README.md CONTRIBUTING.md docs/CAPABILITY-ALIGNMENT.md docs/superpowers/README.md \
  docs/superpowers/specs/2026-09-08-quarkus-production-extension-convergence-design.md \
  docs/superpowers/plans/2026-09-08-p5a-stable-publish-consumer-baseline.md
git diff --cached --check
git commit -m "docs: record P5-A consumer baseline evidence"
```

- [x] **Step 8: Prepare the authorization checkpoint**

Report local commits, exact staged/unstaged files, tests, skips, remote-resolution evidence, and known warnings. Ask separately for push/deploy authorization. Do not create a tag, push a branch, dispatch a workflow, or run Maven deploy during this task.
