# P3 Auth and MQ Samples Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 关闭 P3 中真实未完成的 auth/sample backlog，使保留的 Quarkus auth 与 MQ samples 都具备可运行端到端行为证据。

**Architecture:** 不创建没有外部容器依赖的 auth-testcontainers 空壳；认证后端在各模块/样例中用真实 Quarkus runtime 验证。MQ samples 通过共享 Testcontainers Harness 启动 Kafka/RabbitMQ，并让 listener 更新可注入的内存投影，从 HTTP 创建订单一直断言到消费副作用。

**Tech Stack:** Quarkus 3.37.4/3.38.2、Java 17/21、JUnit 5、REST Assured、Testcontainers 2.0.5、Awaitility。

**Spec:** `docs/superpowers/specs/2026-08-08-p3-auth-samples-ci-design.md`

**Task status:** Tasks 1–2 completed on feature/4.0.x on 2026-09-10. The proposed shared
`ddd4j-quarkus-auth-testcontainers` module is cancelled as an obsolete
abstraction, not completed. Auth behavior is routed to direct Quarkus runtime
integration in the auth samples/modules. Task 2 now verifies Sa-Token/Shiro
HTTP journeys and removes the deprecated Security sample on feature/4.0.x;
MQ sample behavior and feature/3.3.x parity remain open for Tasks 3–4.

## Global Constraints

- 两分支保持各自 ddd4j、Quarkus、Java、Maven Model 和 `<modules>` 合约。
- `ddd4j-quarkus-auth-testcontainers` 取消：Sa-Token、Shiro、Security、License 没有统一容器依赖，ddd4j-boot 也无对应模块。
- License 继续使用真实文件签发/安装/验签；JWT 使用真实密钥；不以容器数量衡量 auth 完成度。
- `auth-security` 已废弃，删除误导性的 sample-auth-security，而不是为废弃后端制造绿色样例。
- sample-auth-satoken 与 sample-auth-shiro 必须验证 login/status/me/role/permission/logout 的 HTTP 生命周期。
- sample-mq-disruptor/kafka/rabbitmq 必须验证 HTTP 创建订单、实际发布、listener 消费和可观察投影。
- Kafka/RabbitMQ tests 复用已发布的 Quarkus MQ Testcontainers Harness；不复制容器启动逻辑。
- 所有新行为先 RED 后 GREEN；不接受仅启动、Bean 可注入或 HTTP 200 作为完成证明。

---

### Task 1: Correct the obsolete auth-testcontainers task

**Files:**
- Modify: `docs/superpowers/specs/2026-08-08-p3-auth-samples-ci-design.md`
- Modify: `docs/superpowers/plans/2026-08-08-p3-auth-samples-ci.md`
- Modify: `docs/superpowers/plans/2026-09-10-p5-b0-dual-branch-release-testcontainers-convergence.md`
- Modify: this plan's Task 1 status ledger

- [x] Verify ddd4j-boot and both Quarkus branches have no common auth container dependency.
- [x] Mark auth-testcontainers as cancelled/replaced by direct runtime integration, not complete.
- [x] Keep sample completion open until Tasks 2–4 pass.
- [x] Commit: `docs: replace obsolete auth-testcontainers backlog`.

Task 1 evidence: neither `ddd4j-boot` nor the Quarkus `feature/3.3.x` and
`feature/4.0.x` trees contain an auth-testcontainers module. The Quarkus
Sa-Token, Shiro, Security, and License modules share no container dependency;
License/JWT retain direct runtime fixtures. The remaining sample gaps are
unchanged: Sa-Token/Shiro need HTTP lifecycle assertions (the deprecated
Security sample is to be removed), and Disruptor/Kafka/RabbitMQ need
observable order-publication-to-listener-consumption assertions.

### Task 2: Complete feature/4.0.x auth samples

**Files:**
- Test: `ddd4j-quarkus-samples/ddd4j-quarkus-sample-auth-satoken/src/test/**`
- Test: `ddd4j-quarkus-samples/ddd4j-quarkus-sample-auth-shiro/src/test/**`
- Delete: `ddd4j-quarkus-samples/ddd4j-quarkus-sample-auth-security/**`
- Modify: `ddd4j-quarkus-samples/pom.xml`
- Modify: `ddd4j-quarkus-bom/pom.xml`

- [x] Add HTTP lifecycle tests for Sa-Token and Shiro: unauthenticated status/me, login returns nonblank token, authenticated status/me, role/permission result, logout and post-logout state.
- [x] Run tests before required sample wiring/config to record RED.
- [x] Add minimal sample wiring/config; use real backend Subject through `SubjectKit`.
- [x] Remove deprecated Security sample from reactor/BOM and document replacement by Sa-Token/Shiro.
- [x] Run both sample modules and full samples reactor on Java 21.
- [x] Commit: `test(samples): complete Quarkus auth journeys`.

Task 2 evidence: both initial HTTP journeys failed on `/auth/status` returning
500 from the default CDI Subject provider. Both independent sample
`clean verify` runs now pass. The final Java 21 / Maven 4 samples reactor
`-DskipTests=false -pl ddd4j-quarkus-samples -am clean verify` passed all 26
selected modules, with 107 tests across 27 suites, 0 failures/errors/skips
(50 tests in samples). The explicit flag overrides the upstream dependencies
BOM's skipped-test default. Evidence is recorded in
`.superpowers/sdd/2026-09-10-p3-auth-samples-completion/task-2-report.md`.
The BOM had no Security sample coordinate to remove; the library's existing
deprecated coordinate remains unchanged. Existing nonfatal Javadoc/model
diagnostics are not cleared by this result, and packaging/publication/CI remain
separate gates.

### Task 3: Complete feature/4.0.x MQ samples

**Files:**
- Modify: three sample MQ listener/application files.
- Create: one projection/state class per sample when no observable consumer state exists.
- Modify: three sample MQ tests and POM/test resources.

- [ ] Write failing tests that POST `/orders`, capture returned order/event identity, and await the listener projection containing the same order data.
- [ ] For Kafka/RabbitMQ, use shared `KafkaQuarkusTestResource`/`RabbitMqQuarkusTestResource`; Disruptor remains in-process.
- [ ] Implement minimal application-scoped projection updated only by the real `@MQEventListener` method.
- [ ] Run each module independently and the samples reactor; record real broker logs.
- [ ] Commit: `test(samples): verify Quarkus MQ end-to-end flows`.

### Task 4: Port reviewed sample behavior to feature/3.3.x

- [ ] Port Tasks 1–3 behavior while keeping Java17/Quarkus3.37.4/ddd4j2/Maven3.
- [ ] Run auth and MQ sample modules under Java17, then Java21 compatibility.
- [ ] Compare public endpoints and observable assertions across branches.
- [ ] Commit version-specific conflict resolutions only.

### Task 5: Full dual-branch completion gate

- [ ] 3.3.x: Java17 and Java21 full `clean verify`.
- [ ] 4.0.x: Java21/Maven4 full `clean verify`.
- [ ] Aggregate XML counts/skips and update P3/P5-B0 docs.
- [ ] Mark sample backlog complete only from these results.
- [ ] Push both branches and require target-head GitHub Actions success.

### Task 6: Return to P5-B0 publication

- [ ] Resume P5-B0 deploy Tasks 6–8 only after Task 5 succeeds.
- [ ] Publish every maintained branch serially and run clean-cache consumers.
