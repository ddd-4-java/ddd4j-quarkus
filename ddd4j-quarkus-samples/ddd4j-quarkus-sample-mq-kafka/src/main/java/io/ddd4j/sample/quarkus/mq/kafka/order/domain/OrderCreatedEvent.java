package io.ddd4j.sample.quarkus.mq.kafka.order.domain;

import io.ddd4j.core.event.MQEvent;
import lombok.Getter;

import java.util.Objects;

/**
 * 订单创建领域事件（MQ 事件）。
 *
 * <p>继承 {@link MQEvent}，与 Disruptor 示例完全一致，体现业务代码零 MQ 耦合。
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
