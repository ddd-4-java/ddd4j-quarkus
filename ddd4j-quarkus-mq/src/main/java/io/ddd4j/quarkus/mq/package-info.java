/**
 * ddd4j 消息队列在 Quarkus 轨道的桥接层。
 *
 * <p>本包提供 ddd4j {@code io.ddd4j.mq.*} 契约与无 Spring 强耦合的本地实现
 * （ddd4j-mq-disruptor / ddd4j-mq-sqs / ddd4j-mq-nats / ddd4j-mq-ons）在 Quarkus
 * CDI 容器下的整合入口。业务项目引入 {@code io.ddd4j.quarkus:ddd4j-quarkus-mq}
 * 即可使用全部 ddd4j MQ 能力，无需在自身 pom 中重复声明三方组件版本。</p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 业务代码：使用 ddd4j-core 的 @MQEventListener 标注方法
 * @ApplicationScoped
 * public class OrderEventHandler {
 *     @MQEventListener(destination = "order.created", group = "order-service")
 *     public void onOrderCreated(MQMessage<OrderCreatedEvent> message) {
 *         // 处理订单创建事件
 *     }
 * }
 *
 * // 业务代码：使用 ddd4j-mq 的 MQEventPublisher 发布事件
 * @Inject
 * private MQEventPublisher publisher;
 *
 * public void publishOrder(OrderCreatedEvent event) {
 *     publisher.publish("order.created", event, Map.of("traceId", traceId));
 * }
 * }</pre>
 *
 * <h2>broker 选择</h2>
 * <ul>
 *   <li>本地内存队列：默认包含 ddd4j-mq-disruptor</li>
 *   <li>AWS SQS：在业务项目 pom 中加入 {@code io.ddd4j:ddd4j-mq-sqs}</li>
 *   <li>NATS：在业务项目 pom 中加入 {@code io.ddd4j:ddd4j-mq-nats}</li>
 *   <li>阿里云 ONS：在业务项目 pom 中加入 {@code io.ddd4j:ddd4j-mq-ons}</li>
 * </ul>
 *
 * <p>需要 Kafka / RabbitMQ / RocketMQ / Pulsar / Redis Stream / ActiveMQ / MQTT 的项目，
 * 请使用 Quarkus 官方的 SmallRye Reactive Messaging（{@code quarkus-messaging-kafka} 等），
 * 并通过 ddd4j 的 {@code MQBrokerAdapter} SPI 编写自定义适配器。</p>
 */
package io.ddd4j.quarkus.mq;
