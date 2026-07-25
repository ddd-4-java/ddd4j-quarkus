package io.ddd4j.sample.quarkus.mq.rabbitmq.mq;

import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.sample.quarkus.mq.rabbitmq.order.domain.event.OrderCreatedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * 订单创建事件 RabbitMQ 消费者。
 *
 * <p>使用 ddd4j {@link MQEventListener} 注解声明订阅关系：
 * <ul>
 *   <li>{@code topic="ORDER"} —— 业务主题，与 OrderCreatedEvent.TOPIC 常量保持一致</li>
 *   <li>{@code tags="created"} —— 只消费 CREATED 标签的事件</li>
 * </ul>
 *
 * <p><b>完整投递链路：</b>
 * <pre>
 *   1. OrderApplicationService 通过 MQEventPublisher 发布 OrderCreatedEvent
 *   2. RabbitMQEventPublisher.publish → 序列化为 JSON → basicPublish 到 Topic Exchange
 *      - routing key = namespace + "." + topic + "." + tag
 *      - 例如：quarkus-rabbitmq-sample.ORDER.created
 *      - 投递到 Exchange "ddd4j.mq.exchange"
 *   3. RabbitMQConsumerEndpointRegistrar 根据 @MQEventListener 自动声明 Queue 并绑定到 Exchange
 *   4. RabbitMQ 推送消息到 Queue，Consumer 拉取消息
 *   5. QuarkusMQListenerRegistrar 反射调用本方法完成业务消费
 *   6. 成功消费后自动 ACK（basicAck）；异常时 NACK + requeue
 * </pre>
 *
 * <p><b>业务零 MQ 耦合：</b>本类不引用任何 {@code com.rabbitmq.client.*} API，
 * 仅依赖 ddd4j 的 {@link MQEventListener} 注解与 {@link OrderCreatedEvent} 抽象。
 * 切换 Kafka / Disruptor / RocketMQ 时本类完全无需修改，仅替换 pom 依赖。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class OrderCreatedMqListener {

    private static final Logger log = Logger.getLogger(OrderCreatedMqListener.class);

    /**
     * 处理 OrderCreatedEvent（从 RabbitMQ Queue 消费）。
     *
     * <p>方法参数必须是具体的事件类型，ddd4j 会通过 {@link io.ddd4j.mq.serialization.JsonMQMessageSerialization}
     * 将 RabbitMQ 消息体反序列化为 OrderCreatedEvent 实例后传入本方法。
     *
     * <p>异常处理策略：
     * <ul>
     *   <li>正常返回 —— 自动 ACK（basicAck）</li>
     *   <li>抛异常 —— NACK + requeue=true（basicNack），Broker 将重新投递</li>
     * </ul>
     *
     * @param event 订单创建事件
     */
    @MQEventListener(topic = "ORDER", tags = "created")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("==================================================");
        log.info("[RabbitMQ MQ 消费者] 收到 OrderCreatedEvent！");
        log.infof("  订单 ID   : %s", event.getOrderId());
        log.infof("  订单编号  : %s", event.getOrderNo());
        log.infof("  买家名称  : %s", event.getBuyerName());
        log.infof("  Topic     : %s", event.getTopic());
        log.infof("  Tag       : %s", event.getTag());
        log.info("==================================================");
    }
}