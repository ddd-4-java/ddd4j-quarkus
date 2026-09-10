# ddd4j-quarkus-sample-mq-disruptor

Disruptor 进程内订单示例：`POST /orders` 创建订单，应用服务调用
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

依赖 `io.ddd4j.quarkus:ddd4j-quarkus-mq-disruptor`，通过显式依赖索引发现
框架 CDI 生产者，保留 listener，并由框架启动注册器初始化真实客户端。
Disruptor 无需外部服务；配置使用 1024 个槽位和 yielding 等待策略。

```bash
./mvnw -pl ddd4j-quarkus-samples/ddd4j-quarkus-sample-mq-disruptor quarkus:dev
```

样例继承 `quarkus.build.skip=true`；普通 package 不代表生成可部署应用。
当前测试证据是 Quarkus 测试运行时行为，不代表部署或生产验收。

## 端到端测试

从仓库根目录运行：

```bash
./mvnw -DskipTests=false -pl ddd4j-quarkus-samples/ddd4j-quarkus-sample-mq-disruptor clean verify
```

测试使用真实 Disruptor RingBuffer，无外部容器。
每次测试清空消费投影，发送唯一订单，最多等待 30 秒断言异步消费结果。
