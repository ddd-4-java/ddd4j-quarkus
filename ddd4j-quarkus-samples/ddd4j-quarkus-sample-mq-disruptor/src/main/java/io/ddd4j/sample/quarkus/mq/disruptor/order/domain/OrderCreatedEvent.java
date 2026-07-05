package io.ddd4j.sample.quarkus.mq.disruptor.order.domain;

import io.ddd4j.core.event.MQEvent;
import lombok.Getter;

import java.util.Objects;

/**
 * 订单创建领域事件（MQ 事件）。
 *
 * <p>继承 {@link MQEvent}，业务方法 {@code publish()} 同时承担
 * "本地事件分发"与"MQ 投递"两种语义。
 *
 * <p>构造时自动设置 topic="ORDER"、tag="CREATED"，
 * {@code @MQEventListener(topic="ORDER", tags="CREATED")} 消费端会自动匹配。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Getter
public class OrderCreatedEvent extends MQEvent {

    private static final long serialVersionUID = 1L;

    private final String orderId;
    private final String orderNo;
    private final String buyerName;

    public OrderCreatedEvent(String orderId, String orderNo, String buyerName) {
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.orderNo = Objects.requireNonNull(orderNo, "orderNo");
        this.buyerName = Objects.requireNonNull(buyerName, "buyerName");
        setTopic("ORDER");
        setTag("CREATED");
    }
}
