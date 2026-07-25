package io.ddd4j.sample.quarkus.mq.rabbitmq.order.domain.event;

import io.ddd4j.mq.event.MQEvent;
import lombok.Getter;

import java.util.Objects;

/**
 * 订单创建领域事件（MQ 事件）。
 *
 * <p>继承自 ddd4j 的 {@link MQEvent}，与 ddd4j-quarkus-sample-mq-disruptor / kafka 示例保持完全一致，
 * 以保证 {@code @MQEventListener} 能通过反射反序列化消费。
 *
 * <p><b>MQ 投递约定：</b>
 * <ul>
 *   <li>topic = {@code "ORDER"} —— 与 @MQEventListener(topic="ORDER") 一致</li>
 *   <li>tag = {@code "created"} —— 与 @MQEventListener(tags="created") 一致</li>
 * </ul>
 * 应用服务在发布前会显式调用 {@link #setTopic(String)} 与 {@link #setTag(String)}，
 * RabbitMQ Broker Adapter 会按 {@code namespace + "." + topic + "." + tag} 拼成 routing key
 * 投递到 Topic Exchange。
 *
 * <p><b>业务零 MQ 耦合：</b>本类不引用任何 {@code com.rabbitmq.client.*} API，
 * 切换底层 Broker（Kafka / Disruptor / RocketMQ）时本类无需任何修改。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Getter
public class OrderCreatedEvent extends MQEvent {

    private static final long serialVersionUID = 1L;

    /** MQ 业务主题（与 RabbitMQ Topic Exchange 的 routing key 一致） */
    public static final String TOPIC = "ORDER";

    /** MQ 标签（业务侧理解为二级路由，小写形式） */
    public static final String TAG = "created";

    /** 订单 ID（与 MQEvent 的 source 字段对齐） */
    private final String orderId;

    /** 订单编号（业务主键，用于消费者日志与下游业务） */
    private final String orderNo;

    /** 买家名称 */
    private final String buyerName;

    /**
     * 构造订单创建事件。
     *
     * @param orderId   订单 ID
     * @param orderNo   订单编号
     * @param buyerName 买家名称
     */
    public OrderCreatedEvent(String orderId, String orderNo, String buyerName) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.orderNo = Objects.requireNonNull(orderNo, "orderNo must not be null");
        this.buyerName = Objects.requireNonNull(buyerName, "buyerName must not be null");
        setTopic(TOPIC);
        setTag(TAG);
    }
}