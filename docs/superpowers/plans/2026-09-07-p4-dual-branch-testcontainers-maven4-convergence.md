# P4 Dual-Branch Testcontainers 2.0.5 and Maven 4 Convergence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade both maintained ddd4j-quarkus branches to Testcontainers 2.0.5 and restore truthful broker round-trip coverage. Maven 4 `subprojects` remains a tracked acceptance target, paused until Quarkus can load that model.

**Architecture:** Keep one published test-harness module that owns container lifecycle, connection properties, readiness, and shared round-trip behavior. Use official Testcontainers container classes where available and bounded GenericContainer adapters elsewhere; keep branch differences limited to ddd4j, Quarkus, Java, Maven model, and aggregator syntax.

**Tech Stack:** Java 17/21, Maven 3/Maven 4.0.0-rc-6, Quarkus 3.37.4/3.38.2, JUnit Jupiter, Testcontainers 2.0.5, Docker, GitHub Actions

**Spec:** `docs/superpowers/specs/2026-09-07-p4-dual-branch-testcontainers-maven4-convergence-design.md`

## Global Constraints

- `feature/3.3.x` stays on ddd4j `feature/2.0.x`, revision `3.3.x.20260630-SNAPSHOT`, Quarkus BOM `3.37.4`, Java 17, Maven model 4.0.0, and `modules/module`.
- `feature/4.0.x` stays on ddd4j `feature/3.0.x`, revision `4.0.x.20260630-SNAPSHOT`, Quarkus BOM `3.38.2`, Java 21, and Maven model 4.1.0.
- The `subprojects/subproject` conversion is paused after both Quarkus 3.38.2 and the authorized `maven-repo-main-2026-09-07` nightly failed to parse the workspace; current executable aggregation remains `modules/module` until quarkusio/quarkus#52190 is fixed.
- Both branches must converge on Testcontainers BOM `2.0.5`; feature/4.0.x is verified and feature/3.3.x remains to be ported.
- Do not create or use a Git worktree.
- Preserve the existing untracked MQTT runtime directory and all unrelated user changes.
- Do not print Maven settings, repository credentials, or remote URLs.
- `MAVEN_SETTINGS_XML` remains the required ddd-4-java organization secret and CI fails fast when it is absent.
- A disabled or skipped test is not counted as passed.
- The user authorized local checkpoint commits for the valid Testcontainers work. Push, deploy, branch creation, and history rewrite remain unauthorized.

## Execution Status — 2026-09-07

- feature/4.0.x: Testcontainers 2.0.5, deterministic lifecycle, official container adapters, RocketMQ/Pulsar round trips, MQTT persistence isolation, and disposable-container CI are implemented.
- Fresh gate: `./mvnw -B clean verify -Denforcer.skip=true` completed all 62 reactor modules; Surefire recorded 138 tests, 0 failures, 0 errors, and 3 skipped.
- Maven 4 probe: native reactor validation accepted `subprojects`, but Quarkus 3.38.2 and main `999-SNAPSHOT` both failed workspace bootstrap. The six aggregators were restored to `modules` and Task 6 is paused.
- feature/3.3.x: pending after the authorized feature/4.0.x checkpoint commit.

---

## File Responsibility Map

### Shared dependency and lifecycle

- `ddd4j-quarkus-dependencies/pom.xml`: Testcontainers BOM and managed module coordinates.
- `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/pom.xml`: compile dependencies of the published test-harness JAR.
- `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/AbstractTestContainerFixture.java`: deterministic start/stop lifecycle; never forces reuse.
- `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/test/java/io/ddd4j/quarkus/mq/testcontainers/AbstractTestContainerFixtureTest.java`: lifecycle regression tests.

### Official container adapters

- `ActiveMqQuarkusTestResource.java`: `ArtemisContainer`.
- `KafkaQuarkusTestResource.java`: `ConfluentKafkaContainer`.
- `RabbitMqQuarkusTestResource.java`: `RabbitMQContainer`.
- `PulsarQuarkusTestResource.java`: `PulsarContainer`.
- `SqsQuarkusTestResource.java`: `LocalStackContainer`.
- `TdmqQuarkusTestResource.java`: public Pulsar-compatible fixture with explicit commercial-service limitation.

### Generic adapters and round trips

- `RocketMqQuarkusTestResource.java`: RocketMQ namesrv/broker startup and route readiness.
- `NatsQuarkusTestResource.java`, `MqttQuarkusTestResource.java`, `MicaMqttQuarkusTestResource.java`, `RedisStreamQuarkusTestResource.java`, `OnsQuarkusTestResource.java`: bounded GenericContainer adapters.
- `AbstractMqQuarkusIntegrationTest.java`: real event round-trip contract and bounded retry.
- Each `*QuarkusIntegrationTest.java`: broker-specific configuration and assertions.

### Maven, CI, and documentation

- Root and five domain aggregator POMs: Maven 4 subprojects target on feature/4.0.x only; currently retained as modules while Task 6 is paused.
- `.github/actions/install-ddd4j/action.yml`: matching ddd4j checkout/install.
- `.github/workflows/ci.yml`: unit and per-broker gates without reusable containers.
- P4/P2 specs and plans, `docs/CAPABILITY-ALIGNMENT.md`, `README.md`, `CONTRIBUTING.md`: evidence-backed current status.

---

### Task 1: Capture the feature/4.0.x RED baseline and dependency migration failure

**Files:**
- Modify: `ddd4j-quarkus-dependencies/pom.xml`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/pom.xml`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-activemq/pom.xml`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-kafka/pom.xml`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-pulsar/pom.xml`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-rabbitmq/pom.xml`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-sqs/pom.xml`

**Interfaces:**
- Consumes: P4 version matrix and existing Maven dependency management.
- Produces: Testcontainers 2.0.5 managed coordinates available to all later tasks.

- [ ] **Step 1: Record the protected starting state**

Run:

```bash
git branch --show-current
git status --short --branch
./mvnw --version
docker info --format '{{.ServerVersion}}'
```

Expected: branch is `feature/4.0.x`; Java is 21; Maven is 4.0.0-rc-6; the existing MQTT directory remains untracked.

- [ ] **Step 2: Expose the two disabled broker failures**

Remove only the class-level `@Disabled` annotations and imports from:

```text
ddd4j-quarkus-mq/ddd4j-quarkus-mq-pulsar/src/test/java/io/ddd4j/quarkus/mq/pulsar/PulsarQuarkusIntegrationTest.java
ddd4j-quarkus-mq/ddd4j-quarkus-mq-rocketmq/src/test/java/io/ddd4j/quarkus/mq/rocket/RocketMqQuarkusIntegrationTest.java
```

- [ ] **Step 3: Run the tests to verify RED**

Run each command independently:

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-pulsar -am test
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-rocketmq -am test
```

Expected: at least one test fails for broker readiness or round-trip timeout. Save the exact failure and container logs; an unrelated dependency-resolution error must be fixed before accepting RED.

- [ ] **Step 4: Upgrade managed Testcontainers coordinates**

In `ddd4j-quarkus-dependencies/pom.xml`:

```xml
<testcontainers-bom.version>2.0.5</testcontainers-bom.version>
```

Replace old module artifactIds with:

```text
testcontainers-junit-jupiter
testcontainers-kafka
testcontainers-rabbitmq
testcontainers-pulsar
testcontainers-activemq
testcontainers-localstack
```

The shared harness keeps these dependencies at compile scope because its `src/main` publishes their types. Broker modules consume `ddd4j-quarkus-mq-testcontainers` at test scope.

- [ ] **Step 5: Verify dependency resolution**

Run:

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers -am dependency:tree -Dincludes=org.testcontainers
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers -am test-compile
```

Expected: every resolved Testcontainers artifact is 2.0.5 and the existing GenericContainer-based harness still compiles. Any package error must name an old Testcontainers 1.x import that Task 3 replaces.

- [ ] **Step 6: Commit only if authorization exists**

```bash
git add ddd4j-quarkus-dependencies/pom.xml ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/pom.xml ddd4j-quarkus-mq/*/pom.xml ddd4j-quarkus-mq/ddd4j-quarkus-mq-pulsar/src/test/java/io/ddd4j/quarkus/mq/pulsar/PulsarQuarkusIntegrationTest.java ddd4j-quarkus-mq/ddd4j-quarkus-mq-rocketmq/src/test/java/io/ddd4j/quarkus/mq/rocket/RocketMqQuarkusIntegrationTest.java
git commit -m "test(mq): expose broker readiness failures and adopt testcontainers 2"
```

Expected: skip this step unless explicit commit authorization was granted.

---

### Task 2: Make the shared lifecycle deterministic

**Files:**
- Create: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/test/java/io/ddd4j/quarkus/mq/testcontainers/AbstractTestContainerFixtureTest.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/AbstractTestContainerFixture.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/Ddd4jQuarkusTestContainersExtension.java`

**Interfaces:**
- Consumes: `container()`, `waitStrategy()`, `exposedProperties()`.
- Produces: `start(): Map<String,String>` and `stop(): void` with owned-container cleanup and no forced reuse.

- [ ] **Step 1: Write the failing lifecycle tests**

Create a package-private fake container that records lifecycle calls:

```java
@Test
void shouldStopTheContainerStartedByTheFixture() {
    RecordingContainer container = new RecordingContainer();
    TestFixture fixture = new TestFixture(container);

    fixture.start();
    fixture.stop();

    assertThat(container.startCount()).isEqualTo(1);
    assertThat(container.stopCount()).isEqualTo(1);
}

@Test
void shouldNotForceReusableContainers() {
    RecordingContainer container = new RecordingContainer();

    new TestFixture(container).start();

    assertThat(container.reuseConfigurationCount()).isZero();
}
```

Implement `RecordingContainer` by overriding `start()`, `stop()`, and `withReuse(boolean)` to increment counters without contacting Docker. The production mutation caught by these tests is restoring the current no-op `stop()` or unconditional `withReuse(true)`.

- [ ] **Step 2: Run the lifecycle tests to verify RED**

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers -Dtest=AbstractTestContainerFixtureTest test
```

Expected: the stop-count and reuse assertions fail against the current implementation.

- [ ] **Step 3: Implement owned-container lifecycle**

Store the created container in the base class:

```java
private GenericContainer<?> runningContainer;

public Map<String, String> start() {
    runningContainer = container();
    WaitStrategy strategy = waitStrategy();
    if (Objects.nonNull(strategy)) {
        runningContainer.waitingFor(strategy);
    }
    runningContainer.start();
    return Map.copyOf(exposedProperties());
}

public void stop() {
    if (Objects.nonNull(runningContainer)) {
        runningContainer.stop();
        runningContainer = null;
    }
}
```

Use imported `java.util.Objects`; do not add a test-only production method.

- [ ] **Step 4: Run the lifecycle and extension tests**

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers -Dtest=AbstractTestContainerFixtureTest test
```

Expected: all selected tests pass and no container is left running by the lifecycle test.

- [ ] **Step 5: Commit only if authorization exists**

```bash
git add ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/AbstractTestContainerFixture.java ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/Ddd4jQuarkusTestContainersExtension.java ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/test/java/io/ddd4j/quarkus/mq/testcontainers/AbstractTestContainerFixtureTest.java
git commit -m "testcontainers: make fixture lifecycle deterministic"
```

---

### Task 3: Migrate official broker containers

**Files:**
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/ActiveMqQuarkusTestResource.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/KafkaQuarkusTestResource.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/RabbitMqQuarkusTestResource.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/PulsarQuarkusTestResource.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/SqsQuarkusTestResource.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/TdmqQuarkusTestResource.java`
- Test: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-activemq/src/test/java/io/ddd4j/quarkus/mq/activemq/ActiveMqQuarkusIntegrationTest.java`
- Test: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-kafka/src/test/java/io/ddd4j/quarkus/mq/kafka/KafkaQuarkusIntegrationTest.java`
- Test: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-rabbitmq/src/test/java/io/ddd4j/quarkus/mq/rabbit/RabbitMqQuarkusIntegrationTest.java`
- Test: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-pulsar/src/test/java/io/ddd4j/quarkus/mq/pulsar/PulsarQuarkusIntegrationTest.java`
- Test: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-sqs/src/test/java/io/ddd4j/quarkus/mq/sqs/SqsQuarkusIntegrationTest.java`
- Test: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-tdmq/src/test/java/io/ddd4j/quarkus/mq/tdmq/TdmqQuarkusIntegrationTest.java`

**Interfaces:**
- Consumes: official Testcontainers 2.0.5 APIs and shared lifecycle.
- Produces: the existing `ddd4j.mq.*` property keys with endpoints returned by official container APIs.

- [ ] **Step 1: Migrate Pulsar first to satisfy the existing RED test**

Use:

```java
import org.testcontainers.pulsar.PulsarContainer;

private PulsarContainer container;

protected GenericContainer<?> container() {
    container = new PulsarContainer(IMAGE);
    return container;
}

protected Map<String, String> exposedProperties() {
    return Map.of(
            "ddd4j.mq.pulsar.service-url", container.getPulsarBrokerUrl(),
            "ddd4j.mq.broker", "PULSAR");
}
```

Remove the hand-written command and port composition.

- [ ] **Step 2: Run Pulsar to verify GREEN**

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-pulsar -am test
```

Expected: `PulsarQuarkusIntegrationTest` runs rather than skips and its real round-trip passes.

- [ ] **Step 3: Migrate the other official containers**

Use these exact types and endpoint APIs:

```java
org.testcontainers.activemq.ArtemisContainer
org.testcontainers.kafka.ConfluentKafkaContainer#getBootstrapServers
org.testcontainers.rabbitmq.RabbitMQContainer#getAmqpUrl
org.testcontainers.localstack.LocalStackContainer#getEndpoint
org.testcontainers.pulsar.PulsarContainer#getPulsarBrokerUrl
```

Keep existing `ddd4j.mq.*` keys so production MQ properties remain unchanged. The resulting property builders are:

```java
// Artemis
Map.of("ddd4j.mq.activemq.broker-url", container.getBrokerUrl(),
        "ddd4j.mq.activemq.host", container.getHost(),
        "ddd4j.mq.activemq.port", String.valueOf(container.getMappedPort(61616)),
        "ddd4j.mq.activemq.username", USERNAME,
        "ddd4j.mq.activemq.password", PASSWORD,
        "ddd4j.mq.broker", "ACTIVEMQ");

// Kafka
Map.of("ddd4j.mq.kafka.bootstrap-servers", container.getBootstrapServers(),
        "ddd4j.mq.broker", "KAFKA");

// RabbitMQ
Map.of("ddd4j.mq.rabbitmq.host", container.getHost(),
        "ddd4j.mq.rabbitmq.port", String.valueOf(container.getAmqpPort()),
        "ddd4j.mq.rabbitmq.username", container.getAdminUsername(),
        "ddd4j.mq.rabbitmq.password", container.getAdminPassword(),
        "ddd4j.mq.rabbitmq.virtual-host", "/",
        "ddd4j.mq.broker", "RABBIT");

// SQS
Map.of("ddd4j.mq.sqs.endpoint", container.getEndpoint().toString(),
        "ddd4j.mq.broker", "SQS");
```

- [ ] **Step 4: Run each official-container broker**

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-activemq -am test
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-kafka -am test
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-rabbitmq -am test
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-sqs -am test
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-tdmq -am test
```

Expected: public-image tests pass; an intentionally disabled commercial TDMQ method is reported as skipped and is not counted as passed. If LocalStack requires authentication, report that exact external prerequisite rather than silently disabling the test.

- [ ] **Step 5: Commit only if authorization exists**

```bash
git add ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/{ActiveMq,Kafka,RabbitMq,Pulsar,Sqs,Tdmq}QuarkusTestResource.java ddd4j-quarkus-mq/ddd4j-quarkus-mq-{activemq,kafka,rabbitmq,pulsar,sqs,tdmq}/pom.xml
git commit -m "testcontainers: use official broker container modules"
```

---

### Task 4: Stabilize RocketMQ readiness without disabling round-trip

**Files:**
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/RocketMqQuarkusTestResource.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/resources/rocketmq/broker.conf`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-rocketmq/src/test/java/io/ddd4j/quarkus/mq/rocket/RocketMqQuarkusIntegrationTest.java`

**Interfaces:**
- Consumes: `apache/rocketmq:5.3.2`, host-accessible namesrv and broker ports.
- Produces: a route-ready `ddd4j.mq.rocketmq.namesrv-addr` before Quarkus creates the publisher.

- [ ] **Step 1: Preserve the RED failure as the readiness contract**

The test must fail if namesrv is reachable but no broker route is registered. Add a bounded precondition assertion in the test resource start path that executes:

```bash
sh mqadmin clusterList -n 127.0.0.1:9876
```

Expected before the fix: non-zero result or output without the configured broker cluster.

- [ ] **Step 2: Replace fixed sleep with route readiness**

Start namesrv and broker, then use a bounded JDK polling loop that:

1. checks the broker boot-success log;
2. executes `mqadmin clusterList`;
3. returns only when the configured broker appears;
4. throws an exception containing the final command output and container logs on timeout.

Use this shape so the shared main source does not gain an Awaitility dependency:

```java
private void awaitBrokerRoute(GenericContainer<?> container) {
    long deadline = System.nanoTime() + Duration.ofMinutes(2).toNanos();
    Container.ExecResult result = null;
    while (System.nanoTime() < deadline) {
        result = container.execInContainer(
                "sh", "-c", "sh mqadmin clusterList -n 127.0.0.1:9876");
        if (result.getExitCode() == 0
                && StrKit.isNotBlank(result.getStdout())
                && result.getStdout().contains("DefaultCluster")) {
            return;
        }
        LockSupport.parkNanos(Duration.ofSeconds(1).toNanos());
    }
    String output = Objects.isNull(result) ? "no probe result"
            : result.getStdout() + result.getStderr();
    throw new IllegalStateException("RocketMQ broker route not ready: " + output
            + System.lineSeparator() + container.getLogs());
}
```

Import `io.ddd4j.kit.lang.StrKit`, `java.util.Objects`, and `java.util.concurrent.locks.LockSupport`; all waits remain bounded.

- [ ] **Step 3: Run RocketMQ to verify GREEN**

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-rocketmq -am test
```

Expected: no class-level Disabled; real publish-consume round-trip passes.

- [ ] **Step 4: Repeat the test to detect startup flakiness**

```bash
for run in 1 2 3; do ./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-rocketmq -am test || exit 1; done
```

Expected: three consecutive passes.

- [ ] **Step 5: Commit only if authorization exists**

```bash
git add ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/RocketMqQuarkusTestResource.java ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/resources/rocketmq/broker.conf ddd4j-quarkus-mq/ddd4j-quarkus-mq-rocketmq/src/test/java/io/ddd4j/quarkus/mq/rocket/RocketMqQuarkusIntegrationTest.java
git commit -m "test(rocketmq): wait for broker route before round trip"
```

---

### Task 5: Revalidate GenericContainer brokers and skipped-test semantics

**Files:**
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/NatsQuarkusTestResource.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/MqttQuarkusTestResource.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/MicaMqttQuarkusTestResource.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/RedisStreamQuarkusTestResource.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers/OnsQuarkusTestResource.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-nats/src/test/java/io/ddd4j/quarkus/mq/nats/NatsQuarkusIntegrationTest.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-mqtt/src/test/java/io/ddd4j/quarkus/mq/mqtt/MqttQuarkusIntegrationTest.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-mqtt-mica/src/test/java/io/ddd4j/quarkus/mq/mqttmica/MicaMqttQuarkusIntegrationTest.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-redis-stream/src/test/java/io/ddd4j/quarkus/mq/redisstream/RedisStreamQuarkusIntegrationTest.java`
- Modify: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-ons/src/test/java/io/ddd4j/quarkus/mq/ons/OnsQuarkusIntegrationTest.java`
- Modify: `AbstractMqQuarkusIntegrationTest.java`

**Interfaces:**
- Consumes: deterministic lifecycle from Task 2.
- Produces: consistent real-event assertions and explicit public/commercial/platform result categories.

- [ ] **Step 1: Run all GenericContainer-backed tests**

```bash
for broker in nats mqtt mqtt-mica redis-stream ons; do
  ./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-${broker} -am test || exit 1
done
```

Expected: public protocols run; ONS commercial round-trip remains explicitly skipped; MQTT-Mica platform skip remains method-scoped rather than hiding injection/startup tests.

- [ ] **Step 2: Add a failing assertion for truthful round-trip results**

In each public-protocol test, assert a literal event ID/payload produced by the test and received through the real broker. The production mutation caught is an empty consumer result or a publisher that never reaches the listener.

- [ ] **Step 3: Prevent MQTT persistence from writing into the repository**

In `MqttQuarkusTestResource`, save the previous `user.dir`, point it at a module target directory before Quarkus constructs Paho, and restore it during stop:

```java
private String previousUserDir;

public Map<String, String> start() {
    previousUserDir = System.getProperty("user.dir");
    Path persistenceDir = Path.of("target", "mqtt-persistence").toAbsolutePath();
    Files.createDirectories(persistenceDir);
    System.setProperty("user.dir", persistenceDir.toString());
    return super.start();
}

public void stop() {
    try {
        super.stop();
    } finally {
        if (Objects.nonNull(previousUserDir)) {
            System.setProperty("user.dir", previousUserDir);
        }
    }
}
```

Add a test that runs the resource and asserts no new `ddd4j-mq-*-tcplocalhost*` directory appears under the module root. Do not delete the pre-existing user-owned directory.

- [ ] **Step 4: Run each changed test to verify RED, then GREEN**

Run the relevant single module before and after the minimal adapter/configuration change. Expected RED is a payload mismatch or timeout; expected GREEN is the exact event payload.

- [ ] **Step 5: Verify no class-level broker disable remains**

```bash
rg -n '@Disabled' ddd4j-quarkus-mq/*/src/test/java
```

Expected: only method-scoped, documented commercial-service or platform limitation skips remain.

- [ ] **Step 6: Commit only if authorization exists**

```bash
git add ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/src/main/java/io/ddd4j/quarkus/mq/testcontainers ddd4j-quarkus-mq/ddd4j-quarkus-mq-{nats,mqtt,mqtt-mica,redis-stream,ons}/src/test
git commit -m "test(mq): enforce truthful generic broker round trips"
```

---

### Task 6: Convert feature/4.0.x aggregators to Maven 4 subprojects — PAUSED

**Files:**
- Modify: `pom.xml`
- Modify: `ddd4j-quarkus-auth/pom.xml`
- Modify: `ddd4j-quarkus-data/pom.xml`
- Modify: `ddd4j-quarkus-extensions/pom.xml`
- Modify: `ddd4j-quarkus-mq/pom.xml`
- Modify: `ddd4j-quarkus-samples/pom.xml`

**Interfaces:**
- Consumes: Maven model 4.1.0 and Maven 4 wrapper.
- Produces: a Maven 4 reactor containing the same projects in the same order.

- [ ] **Step 1: Capture the structural RED check**

```bash
rg -n '<modules>|<module>' pom.xml ddd4j-quarkus-{auth,data,extensions,mq,samples}/pom.xml
```

Expected: the command reports all six aggregators.

- [ ] **Step 2: Replace aggregation syntax**

For each of the six POMs:

```xml
<subprojects>
    <subproject>child-artifact-directory</subproject>
</subprojects>
```

Preserve child order and comments exactly; change only `modules/module` to `subprojects/subproject`.

- [ ] **Step 3: Verify structural GREEN**

```bash
rg -n '<modules>|<module>' pom.xml ddd4j-quarkus-{auth,data,extensions,mq,samples}/pom.xml
rg -n '<subprojects>|<subproject>' pom.xml ddd4j-quarkus-{auth,data,extensions,mq,samples}/pom.xml
```

Expected: the first command returns no matches; the second lists six aggregators and every child.

- [ ] **Step 4: Verify the Maven 4 reactor**

```bash
./mvnw -B -N validate -Denforcer.skip=true
./mvnw -B validate -Denforcer.skip=true
./mvnw -B -DskipTests -Denforcer.skip=true install
```

Expected: reactor discovery and install succeed. Record all Maven model warnings rather than suppressing them.

- [ ] **Step 5: Verify Quarkus tests through root and leaf gates**

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-cache test
./mvnw -B -f ddd4j-quarkus-cache/pom.xml -Denforcer.skip=true test
```

Expected: both execute the actual `@QuarkusTest`. The probe established that WorkspaceLoader rejects model 4.1.0/subprojects, so the executable branch was restored to modules. Keep this task open until quarkusio/quarkus#52190 is fixed and a working non-skip invocation exists.

- [ ] **Step 6: Commit only if authorization exists**

```bash
git add pom.xml ddd4j-quarkus-{auth,data,extensions,mq,samples}/pom.xml
git commit -m "build: use Maven 4 subprojects on the 4.0 line"
```

---

### Task 7: Align CI with branch versions and disposable containers

**Files:**
- Modify: `pom.xml`
- Modify: `.github/actions/install-ddd4j/action.yml`
- Modify: `.github/workflows/ci.yml`

**Interfaces:**
- Consumes: matching branch matrix and broker Maven commands.
- Produces: fail-fast settings bootstrap, unit gate, and blocking broker matrix.

- [ ] **Step 1: Write the expected workflow behavior**

The workflow must:

1. fail before Maven if `secrets.MAVEN_SETTINGS_XML` is empty;
2. install ddd4j `feature/3.0.x` with Java 21 on feature/4.0.x;
3. run all broker matrix entries without `continue-on-error`;
4. remove the root `integration` profile whose only behavior is enabling reusable containers;
5. omit creation of `~/.testcontainers.properties`;
6. upload Surefire/Failsafe reports for every matrix entry.

- [ ] **Step 2: Run actionlint before editing**

```bash
actionlint .github/workflows/ci.yml
```

Expected: current YAML syntax passes; the semantic reuse check from Step 1 fails because the workflow still enables reusable containers. If `command -v actionlint` fails, report the missing local validator and do not install it without authorization.

- [ ] **Step 3: Remove reusable-container CI configuration**

Delete the root `integration` profile and the workflow's `Enable Testcontainers reuse` step. Remove `-Pintegration` from broker commands because the tests already use Surefire naming and the profile never activates tests. Keep Docker discovery and report upload. Ensure RocketMQ and Pulsar remain blocking matrix entries.

- [ ] **Step 4: Verify workflow and secret hygiene**

```bash
actionlint .github/workflows/ci.yml
rg -n 'MAVEN_SETTINGS_XML|continue-on-error|testcontainers.reuse|rocketmq|pulsar' .github/actions/install-ddd4j/action.yml .github/workflows/ci.yml
```

Expected: settings secret and both brokers are present; `continue-on-error` and reuse configuration are absent; no secret value is printed.

- [ ] **Step 5: Commit only if authorization exists**

```bash
git add pom.xml .github/actions/install-ddd4j/action.yml .github/workflows/ci.yml
git commit -m "ci: gate disposable broker integration tests"
```

---

### Task 8: Complete feature/4.0.x verification and documentation

**Files:**
- Modify: `docs/superpowers/README.md`
- Modify: `docs/superpowers/specs/2026-08-07-p2-mq-testcontainers-design.md`
- Modify: `docs/superpowers/plans/2026-08-07-p2-mq-testcontainers.md`
- Modify: `docs/CAPABILITY-ALIGNMENT.md`
- Modify: `README.md`
- Modify: `CONTRIBUTING.md`
- Modify: P4 spec/plan checkboxes and status only after evidence exists

**Interfaces:**
- Consumes: fresh Surefire/Failsafe results.
- Produces: current, separated passed/failed/skipped evidence.

- [ ] **Step 1: Run the complete 4.0.x verification**

```bash
./mvnw -B clean verify -Denforcer.skip=true
./mvnw -B verify -Denforcer.skip=true
git diff --check
```

Expected: record exit codes and exact passed/failed/skipped counts. Enforcer remains a separately reported unresolved gate when skipped.

- [ ] **Step 2: Audit test reports**

```bash
rg -n 'Tests run:|Failures:|Errors:|Skipped:' --glob '*.txt' --glob '*.xml' . | rg 'target/(surefire|failsafe)-reports'
```

Expected: RocketMQ and Pulsar have executed results; no documentation derives counts from Maven BUILD SUCCESS alone.

- [ ] **Step 3: Update documentation with literal observed counts**

Replace obsolete claims such as `318 tests, 0 failures` and `all broker fixtures passed` with the results from Step 2. Keep ONS/TDMQ commercial validation and MQTT-Mica platform constraints separate.

- [ ] **Step 4: Verify version and Maven syntax**

```bash
rg -n '<revision>|<ddd4j.version>|<quarkus-bom.version>|<testcontainers-bom.version>' pom.xml ddd4j-quarkus-dependencies/pom.xml
rg -n '<modules>|<module>' pom.xml ddd4j-quarkus-{auth,data,extensions,mq,samples}/pom.xml
git diff --check
```

Expected for the current executable checkpoint: 4.0.x/3.0.x/3.38.2/2.0.5 with the documented temporary module aggregation; the subprojects acceptance item remains open.

- [ ] **Step 5: Commit only if authorization exists**

```bash
git add docs README.md CONTRIBUTING.md
git commit -m "docs: record 4.0 broker and Maven 4 verification"
```

---

### Task 9: Port the verified harness to feature/3.3.x

**Files:**
- Modify: the same Testcontainers, broker-test, CI, and documentation files as Tasks 1-5 and 7-8.
- Do not modify: Maven model 4.0.0 or modules/module aggregation.

**Interfaces:**
- Consumes: reviewed feature/4.0.x commits or an explicitly approved patch series.
- Produces: behavior-equivalent Java 17/Testcontainers 2.0.5 harness on ddd4j 2.0.x.

- [ ] **Step 1: Reach the branch-switch checkpoint**

Before switching, run:

```bash
git status --short --branch
git log -1 --oneline
```

Expected: all feature/4.0.x tracked changes are committed with authorization. If they are not committed, stop and request commit authorization; do not stash user files or discard changes.

- [ ] **Step 2: Switch to the existing feature/3.3.x branch**

```bash
git switch feature/3.3.x
```

Expected: no worktree command is used and the protected MQTT directory remains untouched.

- [ ] **Step 3: Apply the reviewed behavioral commits**

Cherry-pick only the Testcontainers/lifecycle/broker/CI/documentation commits from Tasks 1-5 and 7-8. Resolve expected version-line conflicts so:

```text
revision = 3.3.x.20260630-SNAPSHOT
ddd4j.version = 2.0.x.20260730-SNAPSHOT
quarkus-bom.version = 3.37.4
java.version = 17
modelVersion = 4.0.0
aggregation = modules/module
```

- [ ] **Step 4: Verify branch-specific dependency resolution**

```bash
./mvnw --version
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers -am dependency:tree -Dincludes=org.testcontainers
```

Expected: Java 17/Maven 3-compatible wrapper and Testcontainers 2.0.5 only.

- [ ] **Step 5: Run target RED/GREEN history and full tests**

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-pulsar -am test
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-rocketmq -am test
./mvnw -B clean verify -Denforcer.skip=true
./mvnw -B verify -Denforcer.skip=true
```

Expected: executed RocketMQ/Pulsar results and explicit passed/failed/skipped counts.

- [ ] **Step 6: Verify Maven 3 aggregation remains intact**

```bash
rg -n '<subprojects>|<subproject>' pom.xml ddd4j-quarkus-{auth,data,extensions,mq,samples}/pom.xml
rg -n '<modules>|<module>' pom.xml ddd4j-quarkus-{auth,data,extensions,mq,samples}/pom.xml
```

Expected: no subproject tags and six aggregators using module tags.

- [ ] **Step 7: Commit conflict resolutions only if authorization exists**

```bash
git add ddd4j-quarkus-dependencies/pom.xml pom.xml .github docs README.md CONTRIBUTING.md ddd4j-quarkus-mq
git commit -m "build(3.3.x): align testcontainers 2 with the Java 17 line"
```

---

### Task 10: Cross-branch consistency and completion gate

**Files:**
- No new implementation files.
- Update: P4 plan/spec status and checkboxes only from verified evidence.

**Interfaces:**
- Consumes: final heads of feature/3.3.x and feature/4.0.x.
- Produces: an evidence-backed dual-branch completion report.

- [ ] **Step 1: Compare the two branches**

```bash
git diff --stat feature/3.3.x..feature/4.0.x
git diff --name-status feature/3.3.x..feature/4.0.x
git diff --unified=1 feature/3.3.x..feature/4.0.x -- pom.xml ddd4j-quarkus-dependencies/pom.xml .github
```

Expected: differences are limited to approved version/JDK/Maven-model compatibility and documented 3.0.x API adaptations.

- [ ] **Step 2: Verify exact branch and remote state**

```bash
for branch in feature/3.3.x feature/4.0.x; do
  git rev-parse "$branch"
  git rev-parse "origin/$branch"
  git rev-parse "github/$branch"
done
```

Expected: report local, origin, and GitHub SHAs separately. Do not claim publication when SHAs differ.

- [ ] **Step 3: Run final hygiene checks on both branches**

On each branch:

```bash
git status --short --branch
git diff --check
rg -n '@Disabled' ddd4j-quarkus-mq/*/src/test/java
```

Expected: only the pre-existing protected MQTT directory is unrelated; no whitespace errors; skips are method-scoped and categorized.

- [ ] **Step 4: Update the P4 status**

Mark an acceptance checkbox complete only when its proving command succeeded in this execution. If Maven 4/Quarkus workspace loading, Docker, LocalStack authentication, or a broker remains blocked, leave the checkbox open and record the exact failing command.

- [ ] **Step 5: Prepare the completion report**

Report:

```text
对应规格或变更:
已实现内容:
未完成内容:
测试和验证证据:
兼容性与剩余风险:
当前规格状态:
本地/远端 SHA:
后续动作:
```

Do not push or deploy unless separately authorized.
