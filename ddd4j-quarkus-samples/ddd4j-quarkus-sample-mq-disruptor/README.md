# ddd4j-quarkus-sample-mq-disruptor

> ddd4j + Quarkus + **Disruptor 本地 MQ** 示例：演示完整"业务发布 DomainEvent → MQ 投递 → @MQEventListener 消费"链路。

## 特点

- **零外部依赖**：基于 LMAX Disruptor RingBuffer，纯进程内内存 MQ，无需 Kafka / RabbitMQ
- **CDI 自动装配**：Quarkus CDI 容器自动发现 Disruptor 组件并注入
- **业务零 MQ 耦合**：业务代码只依赖 `MQEventPublisher` 接口，切换 MQ 只需替换 pom 依赖
- **完整链路**：`Order.create()` → `OrderCreatedEvent` → `MQEventPublisher.publish()` → RingBuffer → `@MQEventListener`

## 架构

```
┌─────────────────┐     ┌──────────────────┐     ┌───────────────────────┐
│  POST /orders   │────▶│ OrderAppService  │────▶│  MQEventPublisher     │
│  (JAX-RS 资源)  │     │ .createOrder()   │     │  (Disruptor 实现)     │
└─────────────────┘     └──────────────────┘     └───────────┬───────────┘
                                                            │
                                                            ▼
                                                  ┌───────────────────────┐
                                                  │  Disruptor RingBuffer │
                                                  │  (本地内存队列)       │
                                                  └───────────┬───────────┘
                                                            │
                                                            ▼
                                                  ┌───────────────────────┐
                                                  │ DisruptorMQEventDispatcher │
                                                  │ → @MQEventListener    │
                                                  └───────────────────────┘
```

## 运行

```bash
# 开发模式（热重载）
mvn -pl ddd4j-quarkus/ddd4j-quarkus-samples/ddd4j-quarkus-sample-mq-disruptor quarkus:dev

# 或打包运行
mvn -pl ddd4j-quarkus/ddd4j-quarkus-samples/ddd4j-quarkus-sample-mq-disruptor package
java -jar target/quarkus-app/quarkus-run.jar
```

## 测试

```bash
curl -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderNo":"ORD-001","buyerId":"B001","buyerName":"张三"}'
```

## 切换 MQ

仅需修改 `pom.xml` 中的依赖：

| MQ 类型 | 依赖 artifactId | 外部依赖 |
|---------|-----------------|---------|
| Disruptor（当前） | `ddd4j-mq-disruptor` | 无 |
| Kafka | `ddd4j-mq-kafka` | Kafka Broker |
| RabbitMQ | `ddd4j-mq-rabbitmq` | RabbitMQ Broker |
