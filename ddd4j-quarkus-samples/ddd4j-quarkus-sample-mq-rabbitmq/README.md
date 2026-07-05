# ddd4j-quarkus-sample-mq-rabbitmq

> ddd4j + Quarkus + **RabbitMQ (AMQP)** 示例：演示完整"业务发布 DomainEvent → RabbitMQ 投递 → @MQEventListener 消费"链路。

## 特点

- **分布式消息队列**：基于 RabbitMQ 的 AMQP 0-9-1 协议，使用 Topic Exchange 与 routing key 路由
- **CDI 自动装配**：Quarkus CDI 容器通过 `ddd4j-quarkus-mq-rabbitmq` 自动装配 Broker Adapter、Publisher、Listener Registrar
- **业务零 MQ 耦合**：业务代码只依赖 `DomainEventPublisher` 与 `MQEventListener` 抽象，与 Disruptor / Kafka 示例完全一致
- **AMQP Topic 路由**：routing key = `<namespace>.<topic>.<tag>`，支持通配符订阅
- **自动拓扑声明**：Exchange / Queue / Binding 由 Broker Adapter 在首次连接时自动 declare
- **手动 ACK**：消费成功后通过 `RabbitMessageAcknowledgment.basicAck` 确认；失败时 `basicNack + requeue`

## 架构

```
┌─────────────────┐     ┌──────────────────┐     ┌───────────────────────┐
│  POST /orders   │────▶│ OrderAppService  │────▶│  DomainEventPublisher │
│  (JAX-RS 资源)  │     │ .createOrder()   │     │  (CdiDomainEventPublisher)│
└─────────────────┘     └──────────────────┘     └───────────┬───────────┘
                                                              │
                                                              ▼
                                              ┌───────────────────────────────────┐
                                              │  MQEventPublisher                 │
                                              │  (ddd4j-mq-rabbitmq 透明切换)      │
                                              └───────────┬───────────────────────┘
                                                          │ amqp-client
                                                          ▼
                                              ┌───────────────────────────────────┐
                                              │  RabbitMQ Broker                  │
                                              │  Topic Exchange: ddd4j.mq.exchange│
                                              │  routing key:                     │
                                              │  quarkus-rabbitmq-sample.ORDER.created│
                                              └───────────┬───────────────────────┘
                                                          │ push message
                                                          ▼
                                              ┌───────────────────────────────────┐
                                              │  Queue (auto-declared, durable)   │
                                              │  Binding: routing key → queue     │
                                              └───────────┬───────────────────────┘
                                                          │ basicConsume
                                                          ▼
                                              ┌───────────────────────────────────┐
                                              │ RabbitMQConsumerEndpointRegistrar │
                                              │ → QuarkusMQListenerRegistrar      │
                                              │ → @MQEventListener                │
                                              │ → OrderCreatedMqListener          │
                                              │   .onOrderCreated(event)          │
                                              └───────────────────────────────────┘
```

## 前置条件

本示例需要本地 RabbitMQ Broker（AMQP 5672 端口）：

```bash
# 启动 RabbitMQ（使用 Docker，最简单）
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management

# 访问管理界面：http://localhost:15672  (guest / guest)
```

或通过环境变量指定外部 Broker：

```bash
export DDD4J_MQ_RABBIT_HOST=your-rabbit-broker.example.com
export DDD4J_MQ_RABBIT_PORT=5672
export DDD4J_MQ_RABBIT_USERNAME=your-user
export DDD4J_MQ_RABBIT_PASSWORD=your-password
export DDD4J_MQ_RABBIT_VHOST=your-vhost
```

## 运行

```bash
# 开发模式（热重载）
mvn -pl ddd4j-quarkus/ddd4j-quarkus-samples/ddd4j-quarkus-sample-mq-rabbitmq quarkus:dev

# 或打包运行
mvn -pl ddd4j-quarkus/ddd4j-quarkus-samples/ddd4j-quarkus-sample-mq-rabbitmq package
java -jar target/quarkus-app/quarkus-run.jar
```

应用启动后，监听 `8080` 端口，并通过 `5672` 端口连接 RabbitMQ Broker。

## 测试

```bash
# 创建订单（触发 OrderCreatedEvent → RabbitMQ → @MQEventListener）
curl -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderNo":"ORD-001","buyerId":"B001","buyerName":"张三"}'
```

期望日志输出：

```
[RabbitMQ MQ 消费者] 收到 OrderCreatedEvent！
  订单 ID   : <uuid>
  订单编号  : ORD-001
  买家名称  : 张三
  Topic     : ORDER
  Tag       : created
```

可在 RabbitMQ 管理界面（`http://localhost:15672`）查看：

- Exchange `ddd4j.mq.exchange`（Topic 类型，durable）
- Queue `quarkus-rabbitmq-sample.ORDER.created`（durable，自动 declare）
- 消息流转统计

## 项目结构

```
ddd4j-quarkus-sample-mq-rabbitmq/
├── pom.xml                                              # Maven 配置（继承 ddd4j-quarkus-samples）
├── README.md                                            # 本文档
└── src/main/
    ├── java/io/ddd4j/sample/quarkus/mq/rabbitmq/
    │   ├── order/
    │   │   ├── domain/
    │   │   │   ├── Order.java                           # 充血聚合根（含 markPaid 状态机）
    │   │   │   ├── OrderStatus.java                     # 订单状态枚举
    │   │   │   └── event/
    │   │   │       └── OrderCreatedEvent.java           # extends DomainEvent<String>
    │   │   ├── application/
    │   │   │   └── OrderApplicationService.java         # 保存订单 + 发布事件
    │   │   ├── infrastructure/
    │   │   │   └── InMemoryOrderRepository.java         # 内存版仓储（ConcurrentHashMap）
    │   │   └── web/
    │   │       └── OrderResource.java                   # JAX-RS POST /orders 端点
    │   └── mq/
    │       ├── OrderCreatedMqListener.java              # @MQEventListener 消费者
    │       └── config/
    │           └── RabbitMqConfig.java                  # RabbitMQProperties CDI Producer
    └── resources/
        └── application.properties                       # Quarkus + RabbitMQ 配置
```

## 切换 MQ

仅需修改 `pom.xml` 中的依赖与 `application.properties` 中的 `ddd4j.mq.broker`：

| MQ 类型        | 依赖 artifactId                | 配置键值       | 外部依赖       |
|----------------|--------------------------------|----------------|----------------|
| Disruptor      | `ddd4j-mq-disruptor`           | `disruptor`    | 无             |
| Kafka          | `ddd4j-mq-kafka`               | `kafka`        | Kafka Broker   |
| **RabbitMQ**   | **`ddd4j-mq-rabbitmq`**        | **`rabbit`**   | **RabbitMQ**   |
| RocketMQ       | `ddd4j-mq-rocketmq`            | `rocket`       | RocketMQ       |
| ActiveMQ       | `ddd4j-mq-activemq`            | `activemq`     | ActiveMQ       |

业务代码（`OrderApplicationService` / `OrderResource` / `OrderCreatedMqListener`）完全无需修改。

## RabbitMQ 拓扑说明

应用启动时，RabbitMQ Broker Adapter 会自动声明以下拓扑（仅当 `auto-declare=true`）：

| 组件            | 名称                                       | 属性             |
|-----------------|--------------------------------------------|------------------|
| Exchange（交换机） | `ddd4j.mq.exchange`                        | Topic, durable   |
| Queue（队列）      | 由 `@MQEventListener` 注解自动生成             | durable          |
| Routing Key     | `<namespace>.<topic>.<tag>`                | -                |
| Binding（绑定）    | Queue ↔ Exchange，按 routing key 绑定        | -                |

本示例的 routing key 实际值为 `quarkus-rabbitmq-sample.ORDER.created`：
- `namespace` 来自 `ddd4j.mq.namespace=quarkus-rabbitmq-sample`
- `topic` 来自事件 `OrderCreatedEvent.TOPIC="ORDER"`
- `tag` 来自事件 `OrderCreatedEvent.TAG="created"`

如需在 RabbitMQ 管理界面手动查看队列与消息：

1. 访问 `http://localhost:15672`
2. 使用 `guest / guest` 登录
3. 进入 "Queues" 或 "Exchanges" 标签页即可看到 `ddd4j.mq.exchange` 与对应的 Queue

## 关键设计点

1. **充血模型**：`Order` 聚合根通过 `Order.create()` 工厂创建，通过 `markPaid()` 等方法封装状态机迁移，不暴露 setter
2. **MQ 领域事件**：`OrderCreatedEvent` 继承 `MQEvent`（与 Kafka/Disruptor sample 一致），构造时自动设置 `topic="ORDER"` / `tag="created"`；通过 `TOPIC` / `TAG` 常量保证发布/消费两端一致
3. **应用服务编排**：`OrderApplicationService` 只依赖 `MQEventPublisher` 与仓储，不引用任何 RabbitMQ / Kafka / Disruptor API
4. **CDI Producer**：`RabbitMqConfig` 从 MicroProfile Config 读取 `ddd4j.mq.rabbit.*` 配置并构建 `RabbitMQProperties`，覆盖 ddd4j-quarkus-mq-rabbitmq 默认 Bean
5. **声明式消费**：`OrderCreatedMqListener` 仅通过 `@MQEventListener(topic="ORDER", tags="created")` 声明订阅关系；Exchange / Queue / Binding 由 `RabbitMQConsumerEndpointRegistrar` 自动完成
6. **手动 ACK**：消费成功后由 `RabbitMessageAcknowledgment.basicAck` 确认；异常时 `basicNack + requeue`，Broker 重新投递