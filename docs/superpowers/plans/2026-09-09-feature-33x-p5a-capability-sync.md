# feature/3.3.x P5-A Capability Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在保持 ddd4j 2.0.x、Maven 3、Java 17 和 Quarkus 3.37.4 分支合约的前提下，为 `feature/3.3.x` 建立与 `feature/4.0.x` P5-A 等价的发布制品消费、Web、License 和 CI 安全能力。

**Architecture:** 以行为契约而不是 Git 历史作为同步单位。先从空 Maven 仓库确定唯一可消费的 ddd4j 2.0.x 快照，再由 Web、License、CI 三个独立任务消费该版本，最后分别在 Java 17 与 21 上完成全量验证并以 3.3.x 的真实证据更新文档。

**Tech Stack:** Java 17/21、Maven 3 / Model 4.0.0、Quarkus 3.37.4、JUnit 5、REST Assured、Testcontainers 2.0.5、GitHub Actions、actionlint。

**Spec:** `docs/superpowers/specs/2026-09-09-feature-33x-p5a-capability-sync-design.md`

## Execution Status — 2026-09-09

Tasks 1–4 已完成提交和各自审查，依据本次独立克隆中的 progress ledger 与提交记录：Task 1 `53457a1`；Web `9e6a4c2` 与 Quarkus BOM 顺序修正 `ace5097`；License `b2936f1`；CI `7c62a27`。

Task 5 已完成，本线状态为 `feature/3.3.x P5-A 本地完成`。首轮 Java 17 qrcode 失败经 easy4j 坐标修复消除，Jackson 坐标清理和审查修正均已纳入已验证代码提交 `597a5b7`。该提交的 Java 17/21 `clean verify -Denforcer.skip=true` 各为 62/62 SUCCESS、53 suites / 186 tests / 0 failures / 0 errors / 11 skipped。远端消费者、Web/License 定向验证、actionlint、分支合约与退役 groupId 零引用、diff/status 门禁均通过；准确日志对应关系见 [CAPABILITY-ALIGNMENT](../../CAPABILITY-ALIGNMENT.md)。push/deploy 与 GitHub-hosted Actions 等非目标仍未验收。

## Actions Snowflake follow-up

Run `34331450813` 的 Java 17/21 已失败；历史本地验证保持原提交范围，不能作为本次修复或 hosted 成功证明。修复契约见规格 §8.3，记录见 `.superpowers/sdd/2026-09-09-feature-33x-p5a-capability-sync/snowflake-fix-report.md`。

- [x] 先运行 IP 115/171、显式 0/31、-1/32 与 CDI 配置测试并保存 RED。
- [x] 最小修复 SnowflakeIdStrategy 与 IdGeneratorProducer。
- [x] Java 17/21 定向测试和完整 data-panache reactor 复验并自审。
- [x] 更新本地证据并提交；修复提交 `a5add95`，hosted Actions 重跑不属于本次子任务。

## Global Constraints

审查单轮修正范围：补强 Web 资源方法内 request-id/Authorization 绑定证据；License 增加嵌套部分材料异常路径和真实 false 签发路径的清理验证；修正 ci.yml、根 POM、dependencies POM 的陈旧注释。本轮不更改生产过滤器、License 实现或依赖版本。定向、受影响模块及已验证提交的双 JDK 全量门禁均已完成，结果见能力证据。

- 目标分支仅为 `feature/3.3.x`；不修改 `master` 或 `feature/4.0.x`。
- 保持 `revision=3.3.x.20260630-SNAPSHOT`、Quarkus BOM/插件 `3.37.4`、Java 基线 17。
- 保持 Maven Model `4.0.0` 与 `<modules>`；禁止引入 `<subprojects>`。
- 自有第三方组件 groupId 统一为 `io.github.easy4j`；全仓已跟踪源码、POM、注释和文档不得引用退役发布组，版本优先沿用导入的 ddd4j BOM。
- 根据新版 AGENTS.md，在普通独立克隆 `ddd4j-quarkus-33x-sync` 中执行；禁止创建、访问或修改 Git worktree。中断前 Task 5 的证据不复用，全部门禁重新运行。
- 保护现有 MQTT UUID 运行目录，不暂存、不提交其中的 `.lck`。
- 不复制 ddd4j 的生产 Web filter 或 License 实现。
- 所有 ddd4j 版本结论必须来自全新 Maven 本地仓库，不得依赖现有 `~/.m2`。
- `MAVEN_SETTINGS_XML` 不得打印、拆解用户名/密码或拼接到命令行 URL。
- `-Denforcer.skip=true` 表示 Enforcer 不属于本阶段验收门禁。
- 每项任务单独提交；未经最终授权不 push、不 deploy、不触发 workflow。

---

### Task 1: Lock the published ddd4j 2.0.x consumer version

**Files:**
- Create: `.github/maven/ddd4j-remote-consumer/pom.xml`
- Modify: `pom.xml`
- Modify: `ddd4j-quarkus-dependencies/pom.xml`
- Modify: `ddd4j-quarkus-bom/pom.xml`

**Interfaces:**
- Consumes: Aliyun snapshot repository configured by the user's Maven settings.
- Produces: one verified `ddd4j.version` used by Tasks 2–5 and a standalone remote-consumer POM containing runtime, Web, and License coordinates.

- [x] **Step 1: Create the remote consumer with the current 20260730 candidate**

Create `.github/maven/ddd4j-remote-consumer/pom.xml` with Model 4.0.0, packaging `pom`, and:

```xml
<properties>
    <ddd4j.version>2.0.x.20260730-SNAPSHOT</ddd4j.version>
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
```

- [x] **Step 2: Test 20260730 from an empty Maven repository**

Run:

```bash
P5A_33_VERSION_REPO="$(mktemp -d /tmp/ddd4j-p5a-33-version.XXXXXX)"
./mvnw -B -U -f .github/maven/ddd4j-remote-consumer/pom.xml \
  -Dmaven.repo.local="$P5A_33_VERSION_REPO" \
  -Dmaven.resolver.transport=wagon \
  dependency:go-offline
```

Expected: either exit 0 with all three JARs and imported dependencies present, or a recorded failure naming the missing 20260730 coordinate. A cached artifact outside `$P5A_33_VERSION_REPO` is not evidence.

- [x] **Step 3: Apply the deterministic fallback only if the candidate is incomplete**

If Step 2 fails because any 20260730 parent/BOM/runtime/Web/License coordinate is absent, change only the fixture property to:

```xml
<ddd4j.version>2.0.x.20260630-SNAPSHOT</ddd4j.version>
```

Then run Step 2 again with a newly created directory, not the failed repository. Expected: exit 0. If both versions fail, stop Task 1 and report the exact missing coordinates; do not edit the reactor POMs.

- [x] **Step 4: Normalize the reactor to the verified version**

Set the root parent and `ddd4j.version` to the same verified value. In `ddd4j-quarkus-dependencies/pom.xml` and `ddd4j-quarkus-bom/pom.xml`, manage at least:

```xml
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
```

Do not change `revision`, Quarkus versions, Model 4.0.0, wrapper distribution, or `<modules>`.

- [x] **Step 5: Verify model and effective versions**

Run:

```bash
./mvnw -B -N validate -Denforcer.skip=true
./mvnw -B -pl ddd4j-quarkus-web,ddd4j-quarkus-auth/ddd4j-quarkus-auth-license \
  -am dependency:tree -Dincludes=io.ddd4j -Denforcer.skip=true
rg -n '<modelVersion>|<revision>|<ddd4j.version>|<quarkus-bom.version>|<maven-quarkus-plugin.version>' \
  pom.xml ddd4j-quarkus-dependencies/pom.xml ddd4j-quarkus-bom/pom.xml \
  .github/maven/ddd4j-remote-consumer/pom.xml
```

Expected: validation succeeds; all relevant ddd4j dependencies resolve to the verified version; Model 4.0.0 and Quarkus 3.37.4 remain unchanged.

- [x] **Step 6: Commit Task 1 files**

```bash
git add pom.xml ddd4j-quarkus-dependencies/pom.xml ddd4j-quarkus-bom/pom.xml \
  .github/maven/ddd4j-remote-consumer/pom.xml
git diff --cached --check
git commit -m "build: align Quarkus 3.3 with published ddd4j snapshot"
```

---

### Task 2: Add the Web consumer and same-thread cleanup contract

**Files:**
- Modify: `ddd4j-quarkus-web/pom.xml`
- Create: `ddd4j-quarkus-web/src/test/java/io/ddd4j/quarkus/web/Ddd4jQuarkusWebConsumerTest.java`
- Create: `ddd4j-quarkus-web/src/test/java/io/ddd4j/quarkus/web/Ddd4jQuarkusWebContractResource.java`

**Interfaces:**
- Consumes: Task 1 managed `ddd4j-web-quarkus` and `ddd4j-runtime-quarkus` plus `ThreadContext` constants available in ddd4j 2.0.x.
- Produces: `GET /ddd4j/contract` behavior and a response probe proving cleanup on the service thread.

- [x] **Step 1: Write the propagation test**

Create `Ddd4jQuarkusWebConsumerTest` with a first request that sends `X-Tenant-Id: tenant-consumer` and asserts status 200, a nonblank `X-Request-Id`, and JSON `tenantId=tenant-consumer`. Add a second request without tenant and assert `tenantId` is null as a secondary regression signal.

- [x] **Step 2: Write the deterministic cleanup test**

Add a request containing tenant, request ID, and Authorization. Assert the resource method returns all three exact bound values before checking cleanup. Assert the response request ID equals the input, the resource service-thread value equals the probe response header, and these headers are all `true`:

```java
X-Ddd4j-Test-Post-Response-Tenant-Cleared
X-Ddd4j-Test-Post-Response-Request-Id-Cleared
X-Ddd4j-Test-Post-Response-Authorization-Cleared
```

- [x] **Step 3: Run the tests before creating the resource**

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-web -am \
  -Dtest=Ddd4jQuarkusWebConsumerTest test
```

Expected: fail because `/ddd4j/contract` or its probe does not exist. Record the exact failure.

- [x] **Step 4: Create the contract resource and response probe**

Create a JAX-RS resource returning tenant ID, request ID, Authorization and current thread. Add a test-only `@ServerResponseFilter(priority = Priorities.AUTHENTICATION)` that records the current thread and uses `Objects.isNull(ThreadContext.get(...))` for tenant, request ID, and Authorization.

Before selecting constants, compile against ddd4j 2.0.x. Use the actual 2.0.x symbols; do not add compatibility copies. Verify the published upstream filter priority is `Priorities.USER` or otherwise prove the probe executes after its cleanup under Quarkus 3.37.4 response-filter ordering.

- [x] **Step 5: Remove leaf dependency versions only when Task 1 manages them**

The Web POM must consume managed dependencies:

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

Retain `ddd4j-web-core` only if a fresh dependency tree proves code in this module directly requires it and it is not supplied by `ddd4j-web-quarkus`.

- [x] **Step 6: Verify and commit Web behavior**

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-web -am \
  -Dtest=Ddd4jQuarkusWebConsumerTest test
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-web -am test
git add ddd4j-quarkus-web/pom.xml \
  ddd4j-quarkus-web/src/test/java/io/ddd4j/quarkus/web/Ddd4jQuarkusWebConsumerTest.java \
  ddd4j-quarkus-web/src/test/java/io/ddd4j/quarkus/web/Ddd4jQuarkusWebContractResource.java
git diff --cached --check
git commit -m "test(web): verify ddd4j 2.0 Quarkus request lifecycle"
```

Expected: both test runs exit 0, and the commit contains only the three Web files.

---

### Task 3: Replace the License assembly-only test with a real lifecycle contract

**Files:**
- Modify: `ddd4j-quarkus-auth/ddd4j-quarkus-auth-license/pom.xml`
- Modify: `ddd4j-quarkus-auth/ddd4j-quarkus-auth-license/src/test/java/io/ddd4j/quarkus/auth/license/LicenseEnabledEndToEndQuarkusTest.java`

**Interfaces:**
- Consumes: Task 1 managed `ddd4j-extension-license` and the ddd4j 2.0.x LicenseCreator API.
- Produces: a signed license before CDI startup plus install, verify, permission, and cleanup assertions.

- [x] **Step 1: Strengthen the behavior assertion before fixing fixture ordering**

Change `licenseVerifyBeanAssembledWhenLicenseEnabled()` to assert:

```java
assertThat(licenseVerify).isNotNull();
assertThat(licenseVerify.isInstallSuccess()).isTrue();
assertThat(licenseVerify.verify()).isTrue();
```

- [x] **Step 2: Run the focused test to establish RED**

```bash
./mvnw -B -Denforcer.skip=true \
  -pl ddd4j-quarkus-auth/ddd4j-quarkus-auth-license -am \
  -Dtest=LicenseEnabledEndToEndQuarkusTest test
```

Expected: fail because the current `@BeforeAll` runs after Quarkus profile configuration/CDI startup and does not call `LicenseCreator.generateLicense()`.

- [x] **Step 3: Move fixture preparation before CDI construction**

Implement `LicenseEnabledProfile#prepareFixture()` so `getConfigOverrides()` and `testResources()` share one unique `Files.createTempDirectory("quarkus-license-end2end-")` directory. Generate both keystores, construct `LicenseCreatorParam`, and require:

```java
if (!new LicenseCreator(param).generateLicense()) {
    throw new IllegalStateException("无法生成 Quarkus License 端到端测试许可证");
}
```

Store the directory path in a test-specific system property only long enough to bridge Quarkus profile class-loader instances.

- [x] **Step 4: Add permission and cleanup guarantees**

On POSIX filesystems set and assert:

```java
directory: OWNER_READ, OWNER_WRITE, OWNER_EXECUTE
files:     OWNER_READ, OWNER_WRITE
```

Register a `QuarkusTestResourceLifecycleManager` that removes the directory after application shutdown. Implement recursive deletion with try-with-resources around `Files.walk`; preserve per-path cleanup failures and clear the system property. The fixture preparation catch path must also delete the directory when signing fails.

The final review adds deterministic failure cases with nested placeholder material: missing main keystore triggers preparation validation failure; readable invalid keystore triggers the real `generateLicense()` false result. Both must remove partial files, the directory, and bridge property. Keep the real install/verify test intact.

- [x] **Step 5: Normalize License dependencies from fresh evidence**

Remove the explicit version from `ddd4j-extension-license`. Run:

```bash
./mvnw -B -Denforcer.skip=true \
  -pl ddd4j-quarkus-auth/ddd4j-quarkus-auth-license -am \
  dependency:tree -Dincludes=io.ddd4j:ddd4j-extension-license,de.schlichtherle.truelicense:truelicense-core
```

Keep the direct versionless `truelicense-core` dependency only if the verified ddd4j 2.0.x published POM does not supply a usable transitive version. Record this ruling in the task report.

- [x] **Step 6: Verify and commit License behavior**

```bash
./mvnw -B -Denforcer.skip=true \
  -pl ddd4j-quarkus-auth/ddd4j-quarkus-auth-license -am \
  -Dtest=LicenseEnabledEndToEndQuarkusTest test
./mvnw -B -Denforcer.skip=true \
  -pl ddd4j-quarkus-auth/ddd4j-quarkus-auth-license -am test
rg -n 'System\.out|printStackTrace|java\.io\.tmpdir|store-pass|key-pass' \
  ddd4j-quarkus-auth/ddd4j-quarkus-auth-license/src/test/java
git add ddd4j-quarkus-auth/ddd4j-quarkus-auth-license/pom.xml \
  ddd4j-quarkus-auth/ddd4j-quarkus-auth-license/src/test/java/io/ddd4j/quarkus/auth/license/LicenseEnabledEndToEndQuarkusTest.java
git diff --cached --check
git commit -m "test(auth): verify ddd4j 2.0 license installation"
```

Expected: focused and module tests pass; no password or key material is printed; only two License files are committed.

---

### Task 4: Replace source-mutating CI with published-artifact resolution

**Files:**
- Create: `.github/actions/configure-maven/action.yml`
- Delete: `.github/actions/install-ddd4j/action.yml`
- Modify: `.github/workflows/ci.yml`
- Modify: `.gitignore`
- Test: `.github/maven/ddd4j-remote-consumer/pom.xml`

**Interfaces:**
- Consumes: base64 organization secret `MAVEN_SETTINGS_XML` and Task 1 remote-consumer POM.
- Produces: secure Maven settings, an empty-cache ddd4j 2.0.x resolution gate, blocking workflow lint, and a clean Git status after MQTT tests.

- [x] **Step 1: Capture the existing CI anti-patterns**

```bash
rg -n 'actions/checkout|sed -i|Install ddd4j|Lint disabled|install-ddd4j' \
  .github/actions/install-ddd4j/action.yml .github/workflows/ci.yml
```

Expected: the old action checks out/edits ddd4j and workflow lint is a placeholder.

- [x] **Step 2: Create the secure Maven configuration action**

Create `.github/actions/configure-maven/action.yml` with one required input `maven-settings-xml`. The first step must:

```bash
install -d -m 700 "$HOME/.m2"
umask 077
P5A_SETTINGS_TMP="$(mktemp "$HOME/.m2/settings.xml.XXXXXX")"
```

Decode into the temporary file, validate `<id>2624322-snapshot-3EoOv3</id>`, `chmod 600`, then `mv -f` atomically to `$HOME/.m2/settings.xml`. Use an EXIT trap to remove the temporary file on all failures without printing its contents.

The second step must create `$RUNNER_TEMP/ddd4j-p5a-remote.XXXXXX` and run the Task 1 consumer with Wagon `dependency:go-offline`.

- [x] **Step 3: Prove success and failure settings paths locally**

Using temporary HOME directories, exercise the shell body with:

1. an existing mode-0644 settings file plus valid base64: final file must be 0600;
2. invalid base64: old target remains and no temp credential file remains;
3. valid XML without the required server id: old target remains and no temp file remains.

Do not commit any temporary HOME or decoded settings file.

- [x] **Step 4: Update workflow Java setup and action usage**

Replace the lint placeholder with checkout plus:

```yaml
- uses: reviewdog/action-actionlint@v1
  with:
    fail_level: error
    filter_mode: nofilter
```

In `unit-and-contract`, add `actions/setup-java@v4` using `${{ matrix.java }}` before the local composite action. In `broker-integration`, retain Java 17 setup and replace the old install action with the same configure action. Both calls pass only `${{ secrets.MAVEN_SETTINGS_XML }}`.

- [x] **Step 5: Ignore MQTT client lock directories**

Add to the root `.gitignore`:

```gitignore
**/ddd4j-mq-*-tcplocalhost*/
```

Run `git check-ignore -v` against both existing UUID directories and confirm neither appears in `git status`.

- [x] **Step 6: Verify workflow semantics**

```bash
actionlint .github/workflows/ci.yml
printf 'name: invalid\non:\n  workflow_dispatch:\njobs:\n  bad:\n    runs-on: ${{ invalid.context }}\n    steps: []\n' \
  | actionlint -
```

Expected: real workflow exits 0; invalid input exits nonzero. Also run:

```bash
rg -n 'install-ddd4j|sed -i|Lint disabled|checkout.*ddd4j' .github || true
```

Expected: no active source-mutating install reference.

- [x] **Step 7: Commit CI and ignore changes**

```bash
git add .github/actions/configure-maven/action.yml .github/workflows/ci.yml \
  .github/maven/ddd4j-remote-consumer/pom.xml .gitignore
git rm .github/actions/install-ddd4j/action.yml
git diff --cached --check
git commit -m "ci: resolve published ddd4j 2.0 snapshot remotely"
```

---

### Task 5: Run dual-JDK completion gates and record 3.3.x evidence

**Files:**
- Fix round 1 authorized modification: `ddd4j-quarkus-dependencies/pom.xml` (remove retired ZXing/Jackson management and comments)
- Fix round 1 authorized modification: `ddd4j-quarkus-extensions/ddd4j-quarkus-extension-qrcode/pom.xml` (versionless easy4j ZXing groupId only)
- Coordinate-rule follow-up: `ddd4j-quarkus-extensions/ddd4j-quarkus-extension-jackson/pom.xml` (current description)
- Coordinate-rule follow-up: `ddd4j-quarkus-extensions/ddd4j-quarkus-extension-jackson/src/main/java/io/ddd4j/quarkus/jackson/ser/NullTolerantBeanSerializerModifier.java` (Javadoc only)
- Modify: `README.md`
- Modify: `CONTRIBUTING.md`
- Create or modify: `docs/CAPABILITY-ALIGNMENT.md`
- Modify: `docs/superpowers/README.md`
- Modify: `docs/superpowers/specs/2026-09-09-feature-33x-p5a-capability-sync-design.md`
- Modify: `docs/superpowers/plans/2026-09-09-feature-33x-p5a-capability-sync.md`

**Interfaces:**
- Consumes: Tasks 1–4 commits and their reports.
- Produces: final Java 17/21 evidence, exact XML totals, scoped documentation, and a push/deploy authorization checkpoint.

Fix round 1 retained the first Java17 qrcode compiler failure as RED and used the Java17-compatible easy4j ZXing artifact managed by imported ddd4j BOM. Its focused and dual-JDK full gates passed. The later coordinate-rule cleanup passed dependency trees, affected-module tests and the zero-reference tracked-source scan. ci.yml was outside that fix's ownership; its subsequent review comment correction was separately authorized. Historical logs remain separate; verified commit `597a5b7` has passed both full completion gates.

- [x] **Step 1: Repeat empty-cache remote resolution**

```bash
P5A_33_FINAL_REPO="$(mktemp -d /tmp/ddd4j-p5a-33-final.XXXXXX)"
./mvnw -B -U -f .github/maven/ddd4j-remote-consumer/pom.xml \
  -Dmaven.repo.local="$P5A_33_FINAL_REPO" \
  -Dmaven.resolver.transport=wagon dependency:go-offline
```

Expected: exit 0 with runtime, Web, License and imported dependency artifacts in the new directory.

- [x] **Step 2: Run focused behavior gates**

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-web -am test
./mvnw -B -Denforcer.skip=true \
  -pl ddd4j-quarkus-auth/ddd4j-quarkus-auth-license -am test
```

Expected: both commands exit 0 with Web cleanup and real License verification tests executed.

- [x] **Step 3: Run the Java 17 full reactor**

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 17)" PATH="$JAVA_HOME/bin:$PATH" \
  ./mvnw -B clean verify -Denforcer.skip=true
```

Expected: all reactor modules succeed. Save the log before running Java 21 because the next clean rebuild replaces reports.

- [x] **Step 4: Run the Java 21 compatibility reactor**

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 21)" PATH="$JAVA_HOME/bin:$PATH" \
  ./mvnw -B clean verify -Denforcer.skip=true
```

Expected: all reactor modules succeed under Java 21. If the host lacks either JDK, stop and report the missing runtime rather than treating the matrix as complete.

- [x] **Step 5: Count final XML and enumerate skips**

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

Read every XML with `skipped > 0` and record class, method, and original reason. Do not subtract skips or reuse 4.0.x totals.

- [x] **Step 6: Run final structural checks**

```bash
actionlint .github/workflows/ci.yml
rg -n '<modelVersion>4\.1\.0|<subprojects>|<subproject>|3\.0\.x\.20260630-SNAPSHOT' \
  pom.xml ddd4j-quarkus-{auth,data,extensions,mq,samples}/pom.xml \
  ddd4j-quarkus-{bom,dependencies}/pom.xml .github || true
rg -n 'install-ddd4j|sed -i|Lint disabled' .github || true
git diff --check
git status --short --branch
```

Expected: no Maven 4, ddd4j 3.0.x, source mutation, or lint-placeholder content; tracked working directory clean; MQTT UUID directories ignored.

- [x] **Step 7: Update documentation from literal 3.3.x evidence**

Record the verified version, Java 17/21 results, exact XML totals, skip reasons, remote-consumer result, Web/License behavior and CI design. State explicitly:

- GitHub-hosted Actions remain unverified until pushed and executed.
- Enforcer, Native, Dev Mode, deploy, cloud services, P5-B–E and master remain outside this completion claim.
- 4.0.x evidence is separate and is not reused.

Only when Steps 1–6 pass, change the spec status to `feature/3.3.x P5-A 本地完成` and mark plan tasks complete.

- [x] **Step 8: Commit documentation**

```bash
git add README.md CONTRIBUTING.md docs/CAPABILITY-ALIGNMENT.md docs/superpowers/README.md \
  docs/superpowers/specs/2026-09-09-feature-33x-p5a-capability-sync-design.md \
  docs/superpowers/plans/2026-09-09-feature-33x-p5a-capability-sync.md
git diff --cached --check
git commit -m "docs: record feature 3.3.x P5-A evidence"
```

- [x] **Step 9: Prepare the authorization checkpoint**

Report every local commit, exact status, remote resolution, both JDK reactor results, XML totals/skips, warnings and review findings. Do not push GitHub/Codeup or run Maven deploy until separately authorized.

---

## Cross-task Review Gate

After every task, dispatch an independent reviewer against that task's base/head range. Fix all Critical and Important findings, then use a fresh scoped re-reviewer. After Task 5, perform a complete implementation-range review and rerun the full completion gate on the resulting verified commit before claiming success.

Completion record: review corrections are committed at `597a5b7`, whose Java17 and Java21 full gates both passed with 53 suites / 186 tests / 0 failures / 0 errors / 11 skipped and 62/62 reactor modules. The local P5-A scope is complete; publication and hosted execution are not included.
