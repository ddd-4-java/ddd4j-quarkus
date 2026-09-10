# ddd4j-quarkus-sample-mq-rabbitmq

真实 RabbitMQ broker订单示例：`POST /orders` 创建订单，应用服务调用
`OrderCreatedEvent.publish()`，框架注册的 `@MQEventListener` 消费后更新
`ConsumedOrderProjection`。投影使用应用级内存状态，仅用于演示异步消费结果，
重启后丢失。

## HTTP 接口

```bash
curl -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderNo":"ORD-001","buyerId":"B001","buyerName":"张三"}'
```

返回订单 ID、订单编号、买家信息和状态。HTTP 成功表示发布调用返回；
测试还会等待消费投影，并验证同一个订单 ID、编号、买家名称、topic 与 tag。

## 配置与运行

依赖 `io.ddd4j.quarkus:ddd4j-quarkus-mq-rabbitmq`，通过显式依赖索引发现
框架 CDI 生产者，保留 listener，并由框架启动注册器初始化真实客户端。
开发运行前启动 RabbitMQ；连接环境变量为 `DDD4J_MQ_RABBIT_HOST`、
`DDD4J_MQ_RABBIT_PORT`、`DDD4J_MQ_RABBIT_USERNAME`、`DDD4J_MQ_RABBIT_PASSWORD`
和 `DDD4J_MQ_RABBIT_VHOST`。规范属性前缀是 `ddd4j.mq.rabbitmq.*`，保留
原 `ddd4j.mq.rabbit.*` 连接、交换机及 durable 配置作为回退。

样例显式声明具名 topic exchange（默认 `ddd4j.mq.exchange`），通用路由与
Rabbit 专用属性使用同一对象，客户端据此声明队列、绑定 routing key 和收发。
禁止使用默认交换机作为绑定目标。应用关闭时 CDI 回收原生连接及其 channels。

```bash
./mvnw -pl ddd4j-quarkus-samples/ddd4j-quarkus-sample-mq-rabbitmq quarkus:dev
```

样例继承 `quarkus.build.skip=true`；普通 package 不代表生成可部署应用。
当前测试证据是 Quarkus 测试运行时行为，不代表部署或生产验收。

## 端到端测试

从仓库根目录运行：

```bash
./mvnw -DskipTests=false -pl ddd4j-quarkus-samples/ddd4j-quarkus-sample-mq-rabbitmq clean verify
```

需要可用的 Docker。测试通过薄生命周期适配器复用共享
`RabbitMqQuarkusTestResource`，由夹具启动容器并注入动态端口；
测试不复制容器实现，也不手动注册或调用 listener。
每次测试清空消费投影，发送唯一订单，最多等待 30 秒断言异步消费结果。
