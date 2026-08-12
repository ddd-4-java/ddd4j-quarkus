# P2 — MQ Testcontainers 设计（14 broker fixtures + 共享基座）

- 日期: 2026-08-07
- 作者: ddd-4-java
- 状态: 设计已确认 / 实施已对齐
- 范围: ddd4j-quarkus-mq-testcontainers + 14 broker 子模块集成测试
- 涉及模块: mq-testcontainers（共享） + mq-{14 broker} 子模块

## 1. 背景与问题

14 个 broker 子模块（ActiveMQ/Kafka/RabbitMQ/Redis-Stream/RocketMQ/Pulsar/Nats/ONS/TDMQ/SQS/MQTT/MQTT-Mica/Disruptor）每个需要 testcontainers 容器支持集成测试，但各 broker 镜像版本、暴露端口、等待策略差异大，重复编写浪费维护成本。

## 2. 目标

1. **共享 fixture 模块** `ddd4j-quarkus-mq-testcontainers`：提供 14 broker 容器生命周期管理 + 抽象基类
2. **每个 broker 子模块至少 1 个集成测试**：`@QuarkusTest` + `@ExtendWith({ *QuarkusTestResource.class })` + 端到端 produce/consume 验证
3. **Docker reuse 启用**：`testcontainers.properties` 复用容器，CI 拉镜像一次复用
4. **真实环境验证**：所有 broker 在 Docker daemon 可用时真实运行通过

## 3. 总体架构

### 3.1 共享模块结构

```
ddd4j-quarkus-mq/ddd4j-quarkus-mq-testcontainers/
├── pom.xml
├── src/main/java/io/ddd4j/quarkus/mq/testcontainers/
│   ├── AbstractTestContainerFixture.java         # 基类（容器生命周期模板）
│   ├── KafkaQuarkusTestResource.java             # confluentinc/cp-kafka:7.6.1 + KRaft
│   ├── RabbitMqQuarkusTestResource.java          # rabbitmq:3.13-management-alpine
│   ├── RocketMqQuarkusTestResource.java          # apache/rocketmq:5.3.0 (withCommand("sh","mqnamesrv"))
│   ├── OnsQuarkusTestResource.java               # 复用 RocketMQ
│   ├── PulsarQuarkusTestResource.java            # apachepulsar/pulsar:3.2.0 (bin/pulsar standalone)
│   ├── TdmqQuarkusTestResource.java              # 复用 Pulsar
│   ├── ActiveMqQuarkusTestResource.java          # apache/activemq-classic:5.18.3
│   ├── NatsQuarkusTestResource.java              # nats:2.10-alpine + -m 8222 monitoring
│   ├── MqttQuarkusTestResource.java              # eclipse-mosquitto:2.0
│   ├── RedisStreamQuarkusTestResource.java       # redis:7.4-alpine + notify-keyspace-events
│   ├── SqsQuarkusTestResource.java               # localstack/localstack:3.4 + sqs
│   ├── DisruptorQuarkusTestResource.java         # 无容器，本地内存
│   └── MicaMqttQuarkusTestResource.java          # 复用 MqttQuarkusTestResource
└── src/main/resources/META-INF/services/
    └── org.junit.jupiter.api.extension.Extension  # SPI 注册
```

### 3.2 集成测试模式

```java
@QuarkusTest
@ExtendWith({ KafkaQuarkusTestResource.class })
class KafkaQuarkusIntegrationTest {
    @Inject MQEventPublisher publisher;
    @Inject KafkaTestConsumer consumer;
    
    @Test
    void shouldProduceAndConsume() {
        publisher.publish("test-topic", new TestEvent("hello"));
        await().atMost(10, SECONDS).untilAsserted(() -> {
            assertThat(consumer.received()).hasSize(1);
        });
    }
}
```

### 3.3 Broker 镜像版本与启动策略

| Broker | 镜像 | 关键配置 |
|---|---|---|
| Kafka | confluentinc/cp-kafka:7.6.1 | KRaft 模式 |
| RabbitMQ | rabbitmq:3.13-management-alpine | Wait.forListeningPorts(5672, 15672)（TLS 端口不监听，跳过） |
| RocketMQ | apache/rocketmq:5.3.0 | withCommand("sh","mqnamesrv")（修复 exit 127：镜像 Cmd=dummy） |
| ONS | （复用 RocketMQ） | 同上 |
| Pulsar | apachepulsar/pulsar:3.2.0 | withCommand("bin/pulsar","standalone")（修复镜像 Cmd=/bin/bash） |
| TDMQ | （复用 Pulsar） | 同上 |
| ActiveMQ | apache/activemq-classic:5.18.3 | 默认端口 61616 |
| NATS | nats:2.10-alpine | -m 8222 monitoring 端口（修复 8222 监控未监听超时） |
| MQTT | eclipse-mosquitto:2.0 | 默认端口 1883 |
| Redis Stream | redis:7.4-alpine | CONFIG SET notify-keyspace-events Ex |
| SQS | localstack/localstack:3.4 | SERVICES=sqs |
| Disruptor | （无容器） | 内存模式 |
| MQTT-Mica | （复用 MQTT） | 同上 |

## 4. 核心抽象

### 4.1 AbstractTestContainerFixture

```java
public abstract class AbstractTestContainerFixture<C extends GenericContainer<?>>
        implements QuarkusTestResourceLifecycleManager {
    
    protected C container;
    
    @Override
    public Map<String, String> start() {
        container = createContainer();
        container.start();
        return getConfigProperties();
    }
    
    @Override
    public void stop() {
        if (container != null) container.stop();
    }
    
    protected abstract C createContainer();
    protected abstract Map<String, String> getConfigProperties();
}
```

### 4.2 QuarkusTestResourceLifecycleManager 模式

注意：本项目用 Quarkus 风格的 `QuarkusTestResourceLifecycleManager` 而非 javalin 风格的 `JunitJupiterQuarkusTestContainers` 注解。原因：Quarkus devservices 已绑定 `org.junit.rules.TestRule`（junit4），可直接复用。

## 5. 关键文件

```
ddd4j-quarkus/ddd4j-quarkus-mq/
├── ddd4j-quarkus-mq-testcontainers/                                          # 共享 fixture 模块
│   ├── pom.xml
│   └── src/main/java/io/ddd4j/quarkus/mq/testcontainers/                     # 14 fixture + AbstractTestContainerFixture
└── ddd4j-quarkus-mq-{broker}/
    ├── pom.xml                                                                # 依赖 testcontainers + mq-core
    └── src/test/java/io/ddd4j/quarkus/mq/{broker}/{Broker}QuarkusIntegrationTest.java
```

测试 resources：
```
ddd4j-quarkus/ddd4j-quarkus-mq/ddd4j-quarkus-mq-{broker}/src/test/resources/
└── application.properties                                                     # ddd4j.mq.broker / bootstrap-servers / consumer.group-id
```

## 6. 测试策略

每个 broker 测试类遵循：
1. `@QuarkusTest` 启动应用
2. `@ExtendWith({ *QuarkusTestResource.class })` 启动容器
3. `application.properties` 读取容器提供的动态端口/地址
4. `@Inject MQEventPublisher` 发布事件
5. `@Inject {Broker}TestConsumer` 消费事件
6. `await().atMost(10, SECONDS)` 异步断言
7. 验证 manual ack / requeue / dead letter 三种场景

**Docker reuse**：`~/.testcontainers.properties` 设置 `testcontainers.reuse.enable=true`，CI 拉镜像一次复用。

## 7. 风险与缓解

| 风险 | 缓解 |
|---|---|
| RocketMQ/ONS 镜像 `Cmd=dummy` 占位符导致 exit 127 | `withCommand("sh","mqnamesrv")` 显式指定 |
| Pulsar/TDMQ 镜像 `Cmd=/bin/bash` 立即退出 | `withCommand("bin/pulsar","standalone")` 显式指定 |
| NATS 8222 monitoring 端口未监听超时 | 添加 `-m 8222` 参数 |
| RabbitMQ TLS 端口超时（5671/15671） | `Wait.forListeningPorts(5672, 15672)` 显式列出 |
| Testcontainers devservices 缺 junit4 `org.junit.rules.TestRule` | pom 添加 `junit:junit` test 依赖 |
| Docker 镜像拉取慢（CI 冷启动） | `withReuse(true)` + 阿里云镜像加速 |
| 阿里云 snapshot 仓库 metadata 陷阱 | 主仓依赖本地 install + offline build |

## 8. 验收标准

- [x] 14 个 broker fixture 真实运行通过
- [x] 13 个 broker 子模块（mq-core / testcontainers 除外）各有 1 个 `@QuarkusTest` 集成测试
- [x] Disruptor（无容器）内存模式集成测试通过
- [x] Docker reuse 启用（CI 复用容器）
- [x] `mvn verify -Pintegration` 全量通过（318 测试 0 失败）

## 9. 相关文档

- 总览 spec: [`./2026-08-05-quarkus-alignment-overview-design.md`](./2026-08-05-quarkus-alignment-overview-design.md)
- 实施计划: [`../plans/2026-08-07-p2-mq-testcontainers.md`](../plans/2026-08-07-p2-mq-testcontainers.md)
