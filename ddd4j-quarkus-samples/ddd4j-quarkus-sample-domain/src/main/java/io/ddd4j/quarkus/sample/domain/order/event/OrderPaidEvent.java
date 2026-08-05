package io.ddd4j.quarkus.sample.domain.order.event;

import io.ddd4j.core.ddd.event.DomainEvent;
import io.ddd4j.core.ddd.event.StringEntityId;

/**
 * 订单支付事件。
 *
 * <p>订单从 {@code CREATED} 流转到 {@code PAID}（{@code Order.pay()}）时触发。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class OrderPaidEvent extends DomainEvent<StringEntityId> {

    /**
     * 事件主题（MQ 路由用）
     */
    public static final String TOPIC = "ORDER_PAID";

    /**
     * 订单 ID
     */
    private final Long orderId;

    /**
     * 订单编号
     */
    private final String orderNo;

    /**
     * 构造订单支付事件。
     *
     * @param orderId 订单 ID
     * @param orderNo 订单编号
     */
    public OrderPaidEvent(Long orderId, String orderNo) {
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
