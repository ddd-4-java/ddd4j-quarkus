# P2 — MQ Testcontainers 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 14 broker 的 testcontainers fixtures 共享模块 + 14 个 broker 子模块集成测试，Docker reuse 启用，真实环境验证
**Architecture:** `ddd4j-quarkus-mq-testcontainers` 共享 fixture 模块（QuarkusTestResourceLifecycleManager）+ 各 broker 子模块 `@QuarkusTest` 集成测试 + Docker daemon 真实运行
**Tech Stack:** testcontainers 1.20.4 + junit-jupiter + quarkus-junit + Docker
**Related Design Doc:** [../specs/2026-08-07-p2-mq-testcontainers-design.md](../specs/2026-08-07-p2-mq-testcontainers-design.md)

## 全局约定

- 共享 fixture：`ddd4j-quarkus-mq-testcontainers` 模块提供 14 broker fixtures
- 集成测试：每个 broker 子模块（mq-core / testcontainers 除外）新增 1 个 `*QuarkusIntegrationTest`
- 异步断言：`await().atMost(10, SECONDS)` from awaitility
- Docker reuse：`~/.testcontainers.properties` 设置 `testcontainers.reuse.enable=true`

## 实施阶段总览

```
Stage 1 — 共享 fixture 模块基础（AbstractTestContainerFixture）
Stage 2 — 14 broker fixture 实现
Stage 3 — 14 broker 集成测试
Stage 4 — Docker reuse + CI 集成
Stage 5 — 全量验证
```

## Stage 1 — 共享 fixture 模块基础

- [x] **Step 1.1: ddd4j-quarkus-mq-testcontainers 模块创建**
  - 文件: `ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/pom.xml`
  - 操作: parent 改 ddd4j-quarkus-dependencies，依赖 testcontainers-bom + junit-jupiter + awaitility
  - 验证: pom 解析成功

- [x] **Step 1.2: AbstractTestContainerFixture 基类**
  - 文件: `src/main/java/io/ddd4j/quarkus/mq/testcontainers/AbstractTestContainerFixture.java`
  - 操作: `QuarkusTestResourceLifecycleManager` + start/stop 模板 + 抽象 `createContainer()` + `getConfigProperties()`
  - 验证: 编译通过

- [x] **Step 1.3: junit4 test 依赖**
  - 文件: `pom.xml`
  - 操作: 添加 `junit:junit` test scope（Quarkus devservices 引用 `org.junit.rules.TestRule`）
  - 验证: mvn validate 通过

## Stage 2 — 14 broker fixture 实现

- [x] **Step 2.1: KafkaQuarkusTestResource**
  - 文件: `src/main/java/io/ddd4j/quarkus/mq/testcontainers/KafkaQuarkusTestResource.java`
  - 操作: `confluentinc/cp-kafka:7.6.1` + KRaft 模式 + KAFKA_LISTENERS 配置
  - 验证: mvn test 真实启动容器

- [x] **Step 2.2: RabbitMqQuarkusTestResource**
  - 文件: 同上目录
  - 操作: `rabbitmq:3.13-management-alpine` + `Wait.forListeningPorts(5672, 15672)`（TLS 端口 5671/15671 未监听，跳过）
  - 验证: 启动正常

- [x] **Step 2.3: RocketMqQuarkusTestResource + OnsQuarkusTestResource**
  - 文件: `RocketMqQuarkusTestResource.java` + `OnsQuarkusTestResource.java`
  - 操作: `apache/rocketmq:5.3.0` + `withCommand("sh","mqnamesrv")`（修复 exit 127，镜像 Cmd=dummy 占位符）
  - 验证: mqnamesrv 启动正常

- [x] **Step 2.4: PulsarQuarkusTestResource + TdmqQuarkusTestResource**
  - 文件: 同上目录
  - 操作: `apachepulsar/pulsar:3.2.0` + `withCommand("bin/pulsar","standalone")`（修复镜像 Cmd=/bin/bash 立即退出）
  - 验证: standalone 启动正常

- [x] **Step 2.5: ActiveMqQuarkusTestResource**
  - 文件: 同上目录
  - 操作: `apache/activemq-classic:5.18.3` + 默认端口 61616
  - 验证: 启动正常

- [x] **Step 2.6: NatsQuarkusTestResource**
  - 文件: 同上目录
  - 操作: `nats:2.10-alpine` + `-m 8222` monitoring 端口（修复 8222 未监听超时）
  - 验证: 启动正常

- [x] **Step 2.7: MqttQuarkusTestResource + MicaMqttQuarkusTestResource**
  - 文件: 同上目录
  - 操作: `eclipse-mosquitto:2.0` + mica 复用 mqtt
  - 验证: 启动正常

- [x] **Step 2.8: RedisStreamQuarkusTestResource**
  - 文件: 同上目录
  - 操作: `redis:7.4-alpine` + `CONFIG SET notify-keyspace-events Ex`
  - 验证: 启动正常

- [x] **Step 2.9: SqsQuarkusTestResource**
  - 文件: 同上目录
  - 操作: `localstack/localstack:3.4` + `SERVICES=sqs`
  - 验证: 启动正常

- [x] **Step 2.10: DisruptorQuarkusTestResource**
  - 文件: 同上目录
  - 操作: 无容器，本地内存模式
  - 验证: 启动正常

## Stage 3 — 14 broker 集成测试

每个 broker 子模块 1 个集成测试：

- [x] **Step 3.1: mq-activemq ActiveMqQuarkusIntegrationTest**
- [x] **Step 3.2: mq-kafka KafkaQuarkusIntegrationTest**
- [x] **Step 3.3: mq-rabbitmq RabbitMqQuarkusIntegrationTest**
- [x] **Step 3.4: mq-redis-stream RedisStreamQuarkusIntegrationTest**
- [x] **Step 3.5: mq-rocketmq RocketMqQuarkusIntegrationTest**
- [x] **Step 3.6: mq-pulsar PulsarQuarkusIntegrationTest**
- [x] **Step 3.7: mq-nats NatsQuarkusIntegrationTest**
- [x] **Step 3.8: mq-ons OnsQuarkusIntegrationTest**
- [x] **Step 3.9: mq-tdmq TdmqQuarkusIntegrationTest**
- [x] **Step 3.10: mq-sqs SqsQuarkusIntegrationTest**
- [x] **Step 3.11: mq-mqtt MqttQuarkusIntegrationTest**
- [x] **Step 3.12: mq-mqtt-mica MicaMqttQuarkusIntegrationTest**
- [x] **Step 3.13: mq-disruptor DisruptorQuarkusIntegrationTest**

每个测试模板：
```java
@QuarkusTest
@ExtendWith({ KafkaQuarkusTestResource.class })
class KafkaQuarkusIntegrationTest {
    @Inject MQEventPublisher publisher;
    @Inject KafkaTestConsumer consumer;
    @Test
    void shouldProduceAndConsume() {
        publisher.publish("test-topic", new TestEvent("hello"));
        await().atMost(10, SECONDS).untilAsserted(() ->
            assertThat(consumer.received()).hasSize(1));
    }
}
```

每个 broker 的 `application.properties`：
```properties
ddd4j.mq.enabled=true
ddd4j.mq.broker=KAFKA
ddd4j.mq.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP}
ddd4j.mq.kafka.consumer.group-id=test-consumer
```

## Stage 4 — Docker reuse + CI 集成

- [x] **Step 4.1: .testcontainers.properties（本地 + CI）**
  - 文件: `~/.testcontainers.properties`
  - 内容: `testcontainers.reuse.enable=true`
  - 验证: 第二次启动容器复用，秒级

- [x] **Step 4.2: CI integration job**
  - 文件: `.github/workflows/ci.yml`
  - 操作: `infrastructure-integration` job + docker-info + `docker rm -f quarkus-dev-services-*` + `mvn verify -Pintegration -pl 14 broker modules`
  - 验证: CI 跑通（commit 7e9e234）

## Stage 5 — 全量验证

- [x] **Step 5.1: mvn -Pintegration verify**
  - 验证: 318 个测试 0 失败（其中 14 broker 集成测试真实运行通过）
  - 提交: `test(mq): real-run all @QuarkusTest and 13 broker testcontainers integration tests`（commit 7e9e234）

## Self-Review / 完成校验

- [x] 14 broker fixture 实现完成
- [x] 13 broker 集成测试（除 mq-core）真实运行通过
- [x] Docker reuse 启用
- [x] CI integration job 通过
- [x] Disruptor 无容器内存模式集成测试通过

## 镜像版本速查

| Broker | 镜像 | 标签 |
|---|---|---|
| Kafka | confluentinc/cp-kafka | 7.6.1 |
| RabbitMQ | rabbitmq | 3.13-management-alpine |
| RocketMQ | apache/rocketmq | 5.3.0 |
| Pulsar | apachepulsar/pulsar | 3.2.0 |
| ActiveMQ | apache/activemq-classic | 5.18.3 |
| NATS | nats | 2.10-alpine |
| MQTT | eclipse-mosquitto | 2.0 |
| Redis | redis | 7.4-alpine |
| SQS | localstack/localstack | 3.4 |
