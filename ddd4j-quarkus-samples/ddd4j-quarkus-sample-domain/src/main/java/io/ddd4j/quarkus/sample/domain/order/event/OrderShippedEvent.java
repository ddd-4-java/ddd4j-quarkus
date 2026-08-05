package io.ddd4j.quarkus.sample.domain.order.event;

import io.ddd4j.core.ddd.event.DomainEvent;
import io.ddd4j.core.ddd.event.StringEntityId;

/**
 * 订单发货事件。
 *
 * <p>订单从 {@code PAID} 流转到 {@code SHIPPED}（{@code Order.ship()}）时触发。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class OrderShippedEvent extends DomainEvent<StringEntityId> {

    /**
     * 事件主题（MQ 路由用）
     */
    public static final String TOPIC = "ORDER_SHIPPED";

    /**
     * 订单 ID
     */
    private final Long orderId;

    /**
     * 订单编号
     */
    private final String orderNo;

    /**
     * 构造订单发货事件。
     *
     * @param orderId 订单 ID
     * @param orderNo 订单编号
     */
    public OrderShippedEvent(Long orderId, String orderNo) {
        super(String.valueOf(orderId));
        this.orderId = orderId;
        this.orderNo = orderNo;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }
}
