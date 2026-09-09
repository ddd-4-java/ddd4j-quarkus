# P5-B0 Dual-Branch Release and Testcontainers Convergence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `feature/3.3.x` 与 `feature/4.0.x` 收敛到 Testcontainers 2.0.5 和同一 broker 行为矩阵，完成双分支发布及发布后空缓存消费。

**Architecture:** `feature/4.0.x` 是已验证的 Testcontainers 2.0.5 行为参考，`feature/3.3.x` 按 Java 17/Quarkus 3.37.4/ddd4j 2.0.x 重新适配。专用 Testcontainers Java 模块只负责容器细节，共享 Harness 继续统一生命周期、配置注入和 round-trip；发布严格串行并用独立 consumer 验证。

**Tech Stack:** Java 17/21、Maven 3/Maven 4.0.0-rc-6、Quarkus 3.37.4/3.38.2、Testcontainers 2.0.5、JUnit 5、Docker、GitHub Actions、阿里云 Maven。

**Spec:** `docs/superpowers/specs/2026-09-10-p5-b0-dual-branch-release-testcontainers-convergence-design.md`

## Global Constraints

- `feature/3.3.x`: revision `3.3.x.20260630-SNAPSHOT`, ddd4j `2.0.x.20260630-SNAPSHOT`, Quarkus `3.37.4`, Java 17, Model 4.0.0 + `<modules>`。
- `feature/4.0.x`: revision `4.0.x.20260630-SNAPSHOT`, ddd4j `3.0.x.20260630-SNAPSHOT`, Quarkus `3.38.2`, Java 21, Model 4.1.0 + `<modules>`。
- 不引入活动 `<subprojects>`/`<subproject>`。
- 两分支 Testcontainers effective version 必须为 `2.0.5`。
- ActiveMQ、Kafka、LocalStack、Pulsar、RabbitMQ 使用 Testcontainers 2.x Java 专用模块/类；没有合适 Java 官方模块的 broker 使用受控 `GenericContainer`。
- 已退役的前序扩展 group 坐标零引用；用 `git grep -E 'io[.]github[.]hiwepy'` 执行门禁，扩展统一使用 `io.github.easy4j`。
- 任何 skip 必须记录类、方法、原因和恢复条件。
- 不把 ONS/TDMQ fallback 或本地模拟称为云服务验收。
- 不把 Maven upload、BUILD SUCCESS、Actions 或 consumer 中任意单项独立称为发布完成。
- 每个分支 deploy 后必须从新的 `maven.repo.local` 消费 BOM、dependencies、parent 与代表模块。
- P5-B productization 和 P3 历史 backlog 不与 P5-B0 实现混合。

---

### Task 1: Reconcile historical task status from evidence

**Files:**
- Modify: `docs/superpowers/plans/2026-08-06-p1-extensions.md`
- Modify: `docs/superpowers/plans/2026-08-08-p3-auth-samples-ci.md`
- Modify: `docs/superpowers/plans/2026-09-10-p3-auth-samples-completion.md`
- Modify: `docs/superpowers/plans/2026-09-07-p4-dual-branch-testcontainers-maven4-convergence.md`

**Interfaces:**
- Consumes: Git history, current trees, P4/P5 reports, successful Actions runs.
- Produces: truthful completed/open/cancelled task ledger used by later tasks.

- [ ] **Step 1: Prove P1 deferred items**

Verify on both branches that `ddd4j-quarkus-extension-pf4j` is absent and both data-jpa/data-external contain `src/main` production sources. Record the commits that introduced/remediated them.

- [ ] **Step 2: Prove P3 deferred items**

Verify pf4j deletion as complete. Record the proposed `ddd4j-quarkus-auth-testcontainers` module as cancelled/replaced by direct Quarkus runtime integration: it is absent from ddd4j-boot and both Quarkus branches, and the four auth backends have no common container dependency. Do not mark that cancelled proposal complete or keep it as an open P5-B0 deliverable. Route auth behavior to `docs/superpowers/plans/2026-09-10-p3-auth-samples-completion.md`; inspect sample-auth/sample-mq source and tests, marking them complete only when each retained sample has runnable behavior evidence, otherwise keep them open with the exact missing list.

- [ ] **Step 3: Reconcile P4 steps**

Map Tasks 1–5 and 7–8 to current 4.0.x commits/tests. Mark only proven steps complete. Mark Task 6 as `取消：用户确认保持 Model 4.1.0 + <modules>` rather than completed. Keep Task 9/10 portions open until Tasks 2–5 below finish.

- [ ] **Step 4: Commit status corrections**

```bash
git add docs/superpowers/plans/2026-08-06-p1-extensions.md \
  docs/superpowers/plans/2026-08-08-p3-auth-samples-ci.md \
  docs/superpowers/plans/2026-09-07-p4-dual-branch-testcontainers-maven4-convergence.md
git diff --cached --check
git commit -m "docs: reconcile verified Superpowers task status"
```

---

### Task 2: Upgrade feature/3.3.x Testcontainers dependency model

**Files:**
- Modify: `ddd4j-quarkus-dependencies/pom.xml`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/pom.xml`
- Modify: affected broker POMs only when they directly declare old artifacts.

**Interfaces:**
- Consumes: ddd4j 2.0.x BOM and Quarkus 3.37.4 dependency management.
- Produces: Testcontainers 2.0.5-only effective tree and compile-ready harness.

- [ ] **Step 1: Capture RED dependency evidence**

```bash
./mvnw -B -Denforcer.skip=true \
  -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers -am \
  dependency:tree -Dincludes=org.testcontainers
```

Expected before change: effective Testcontainers `1.20.4` and old artifact IDs.

- [ ] **Step 2: Put Testcontainers 2.0.5 BOM first**

Set `<testcontainers-bom.version>2.0.5</testcontainers-bom.version>` and import this BOM before ddd4j dependencies. Replace old artifact names with:

```text
testcontainers-junit-jupiter
testcontainers-activemq
testcontainers-kafka
testcontainers-localstack
testcontainers-pulsar
testcontainers-rabbitmq
```

- [ ] **Step 3: Verify effective dependency tree**

Run dependency tree and `test-compile` from Step 1. Expected: all Testcontainers artifacts are 2.0.5; compile failures identify source imports owned by Task 3.

- [ ] **Step 4: Commit dependency migration**

```bash
git add ddd4j-quarkus-dependencies/pom.xml ddd4j-quarkus-mq
git diff --cached --check
git commit -m "build(3.3.x): manage Testcontainers 2.0.5 modules"
```

---

### Task 3: Port official-container and lifecycle behavior to feature/3.3.x

**Files:**
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/AbstractTestContainerFixture.java`
- Modify: `.../ActiveMqQuarkusTestResource.java`
- Modify: `.../KafkaQuarkusTestResource.java`
- Modify: `.../PulsarQuarkusTestResource.java`
- Modify: `.../RabbitMqQuarkusTestResource.java`
- Modify: `.../SqsQuarkusTestResource.java`
- Modify: GenericContainer fixture files only for 2.0.5 API/readiness compatibility.
- Test: `.../AbstractTestContainerFixtureTest.java`
- Test: affected `*QuarkusIntegrationTest.java` files.

**Interfaces:**
- Consumes: Task 2 managed modules.
- Produces: deterministic lifecycle and the same 13-row behavior matrix as 4.0.x.

- [ ] **Step 1: Add lifecycle/adapter contract tests before source migration**

Tests must fail if owned containers are not stopped, reuse is forced, or a supposed official fixture returns only generic hand-built endpoints instead of its dedicated API.

- [ ] **Step 2: Run focused RED tests**

Run harness tests plus ActiveMQ/Kafka/Pulsar/RabbitMQ/SQS modules. Accept only missing 2.x types or contract assertions as RED.

- [ ] **Step 3: Port reviewed 4.0.x behavior**

Use `ArtemisContainer`, `KafkaContainer`/the 2.0.5 compatible Kafka dedicated class, `LocalStackContainer`, `PulsarContainer`, `RabbitMQContainer`. Preserve the exact `ddd4j.mq.*` property keys and shared Harness ownership.

- [ ] **Step 4: Verify every broker row independently**

Run all 13 modules. Public broker round-trips must execute; method-scoped ONS/TDMQ/platform skips remain explicit. Repeat RocketMQ three times to check readiness stability.

- [ ] **Step 5: Commit behavior migration**

```bash
git add ddd4j-quarkus-mq
git diff --cached --check
git commit -m "test(3.3.x): align Testcontainers 2 broker harness"
```

---

### Task 4: Run dual-branch local and structural gates

**Files:**
- Modify: branch capability/status docs only after evidence.

**Interfaces:**
- Consumes: final 3.3.x and 4.0.x implementation heads.
- Produces: XML totals, skip ledger, effective-version matrix and clean push candidates.

#### 4.0.x Easy4J structural precheck evidence

此预检查仅关闭 4.0.x 的扩展坐标结构阻塞，不得将 Task 4、双分支验证、CI、发布或空缓存消费标为完成。

- `ddd4j-data-crypto` 已不再消费 Easy4J Jackson extension；它通过 `ddd4j-core` 获得 `tools.jackson`。
- `io.ddd4j:ddd4j-dependencies:3.0.x.20260630-SNAPSHOT` 管理 `io.github.easy4j:jackson-extension` 与 `io.github.easy4j:zxing-extension`，版本均为 `3.0.x.20260630-SNAPSHOT`。
- 两个 Quarkus 叶子必须直接声明 Easy4J group 和相应 artifact，但不得声明 `<version>`；实际版本只能由 ddd4j 3.0.x BOM 提供。

在 Java 21 / Maven 4 下执行下列门禁。`MAVEN4` 必须指向 Maven `4.0.0-rc-6` 可执行文件：

```bash
set -euo pipefail
: "${MAVEN4:?set MAVEN4 to the Maven 4.0.0-rc-6 executable}"

retired_group_regex='io[.]github[.]hiwepy'
set +e
git grep -n -E "$retired_group_regex"
retired_group_status=$?
set -e
if [ "$retired_group_status" -eq 0 ]; then
  echo "retired extension group remains in tracked source" >&2
  exit 1
fi
if [ "$retired_group_status" -ne 1 ]; then
  exit "$retired_group_status"
fi

assert_versionless_easy4j_leaf() {
  pom=$1
  artifact=$2
  rg -U -q "<groupId>io[.]github[.]easy4j</groupId>[[:space:]]*<artifactId>${artifact}</artifactId>[[:space:]]*</dependency>" "$pom"
}

assert_versionless_easy4j_leaf ddd4j-quarkus-extensions/ddd4j-quarkus-extension-jackson/pom.xml jackson-extension
assert_versionless_easy4j_leaf ddd4j-quarkus-extensions/ddd4j-quarkus-extension-qrcode/pom.xml zxing-extension

"$MAVEN4" -B -Denforcer.skip=true \
  -pl ddd4j-quarkus-extensions/ddd4j-quarkus-extension-jackson -am \
  dependency:tree '-Dincludes=io.github.easy4j:jackson-extension' -Dverbose \
  | rg -F 'io.github.easy4j:jackson-extension:jar:3.0.x.20260630-SNAPSHOT:compile'
"$MAVEN4" -B -Denforcer.skip=true \
  -pl ddd4j-quarkus-extensions/ddd4j-quarkus-extension-qrcode -am \
  dependency:tree '-Dincludes=io.github.easy4j:zxing-extension' -Dverbose \
  | rg -F 'io.github.easy4j:zxing-extension:jar:3.0.x.20260630-SNAPSHOT:compile'
```

- [x] **Step 1: Verify feature/3.3.x**

Run Java 17 and Java 21 `clean verify -Denforcer.skip=true`, actionlint, effective Testcontainers tree, Model 4.0/modules scan and zero-retired-group scan.

- [x] **Step 2: Verify feature/4.0.x**

Run Java 21 `clean verify -Denforcer.skip=true`, actionlint, effective Testcontainers tree, Model 4.1/modules scan and zero-retired-group scan.

- [x] **Step 3: Aggregate XML evidence**

For each run, total suites/tests/failures/errors/skipped and list every skipped method/reason. Do not reuse earlier totals after tests change.

- [x] **Step 4: Compare broker matrix**

Produce a 13-row comparison showing fixture type, image, readiness, round-trip and skip category on both branches. Differences require an explicit version/API justification.

- [x] **Step 5: Commit evidence docs**

```bash
git add README.md CONTRIBUTING.md docs
git diff --cached --check
git commit -m "docs: record dual-branch Testcontainers 2 verification"
```

Task 4 本地验证证据（2026-09-10）：3.3.x `c37c843` 的 Java 17/21 各为 62/62 模块、
57 suites / 211 tests / 0 failures / 0 errors / 3 skips；4.0.x `702a1db` 的 Java 21 为
62/62 模块、51 suites / 170 tests / 0 failures / 0 errors / 3 skips。结构、Testcontainers 2.0.5
和 Easy4J 门禁通过。完整矩阵及限制见 [本地运行记录](../../P5-B0-LOCAL-VERIFICATION.md)。
Task 5–8 与 P5-B0 发布闭环仍未完成；本地门禁不替代 hosted CI 或发布消费。

---

### Task 5: Push and verify GitHub Actions

**Files:** none.

**Interfaces:**
- Consumes: reviewed, locally green branch heads.
- Produces: matching GitHub/Codeup SHAs and completed Actions results.

- [ ] **Step 1: Fast-forward push both branches**

Push `feature/3.3.x` and `feature/4.0.x` to GitHub and Codeup without force.

- [ ] **Step 2: Verify four remote refs**

Compare each local head with GitHub and Codeup `ls-remote` results.

- [ ] **Step 3: Observe Actions to completion**

Require target-head success for lint, branch Java matrices and broker matrices. On failure, diagnose and fix before publication.

---

### Task 6: Deploy and independently consume feature/3.3.x

**Files:**
- Create: `.github/maven/ddd4j-quarkus-remote-consumer/pom.xml` if a reusable fixture does not already exist.

**Interfaces:**
- Consumes: successful Task 5 head and configured Maven settings.
- Produces: complete 3.3.x snapshot publication and clean-cache consumer evidence.

- [ ] **Step 1: Deploy with Java 17/Maven 3 line**

Run clean deploy with the repository settings and Wagon transport where required. Save the complete reactor summary; partial uploads are not success.

- [ ] **Step 2: Resolve from a new Maven repository**

Consume BOM, dependencies, parent, Web, data-panache, auth-license, mq-testcontainers, one official-container broker and one GenericContainer broker. Reactor `-am` and relative paths are forbidden.

- [ ] **Step 3: Record publication metadata**

Capture timestamped snapshot versions and SHA checks where available.

---

### Task 7: Deploy and independently consume feature/4.0.x

**Files:**
- Modify: the same reusable remote-consumer fixture only for the 4.0.x coordinate set.

**Interfaces:**
- Consumes: successful 3.3.x publication/consumer and 4.0.x Actions head.
- Produces: complete 4.0.x snapshot publication and Maven 4 clean-cache consumer evidence.

- [ ] **Step 1: Deploy with Java 21/Maven 4 line**

Run clean deploy with Wagon/repository handling required by Aliyun metadata. If deploy fails after uploads, inspect and resume from the recorded failed module; do not restart blindly or call it successful.

- [ ] **Step 2: Resolve from a new Maven repository**

Consume the same artifact categories as Task 6 using Maven 4/Java 21 and no reactor paths.

- [ ] **Step 3: Verify Model 4.1/modules publication**

Confirm downloaded POMs retain the documented Model 4.1-compatible `<modules>` boundary without claiming `<subprojects>` acceptance.

---

### Task 8: Close P5-B0 and route genuine historical backlog

**Files:**
- Modify: P1/P3/P4/P5-B0 plans/specs and capability documentation.

**Interfaces:**
- Consumes: Tasks 1–7 evidence.
- Produces: final truthful completion state and next executable plan boundary.

- [ ] **Step 1: Mark P4/P5-B0 only from final evidence**

Mark Testcontainers, matrix, Actions, deploy and consumer items complete only when their proving commands succeeded.

- [ ] **Step 2: Preserve genuine P3 open items**

Keep only the concretely incomplete sample-auth/sample-mq behavior open with exact acceptance criteria, routed to `docs/superpowers/plans/2026-09-10-p3-auth-samples-completion.md` Tasks 2–4. The proposed auth-testcontainers module is cancelled/replaced by direct Quarkus runtime integration and must not be reopened or counted as a P5-B0 deliverable.

- [ ] **Step 3: Commit final status**

```bash
git add docs README.md CONTRIBUTING.md
git diff --cached --check
git commit -m "docs: close P5-B0 dual-branch release convergence"
```

## Final Review Gate

Every implementation task receives an independent spec/quality review. Fix Critical/Important findings and re-review before proceeding. After Task 8, perform one whole-range review and rerun the final consumer/status checks before claiming completion.
