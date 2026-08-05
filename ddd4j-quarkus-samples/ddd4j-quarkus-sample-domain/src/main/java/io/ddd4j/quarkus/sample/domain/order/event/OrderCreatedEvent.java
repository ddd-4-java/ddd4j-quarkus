package io.ddd4j.quarkus.sample.domain.order.event;

import io.ddd4j.core.ddd.event.DomainEvent;
import io.ddd4j.core.ddd.event.StringEntityId;

import java.math.BigDecimal;

/**
 * 订单创建事件。
 *
 * <p>新订单创建（{@code Order.create(...)}）时触发。此时订单尚未持久化，
 * {@code orderId} 可能为 {@code null}，基础设施层在 {@code save()} 回填主键后
 * 会重建携带真实 {@code orderId} 的事件再发布。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class OrderCreatedEvent extends DomainEvent<StringEntityId> {

    /**
     * 事件主题（MQ 路由用）
     */
    public static final String TOPIC = "ORDER_CREATED";

    /**
     * 订单 ID（持久化前为 null）
     */
    private final Long orderId;

    /**
     * 订单编号
     */
    private final String orderNo;

    /**
     * 买家 ID
     */
    private final String buyerId;

    /**
     * 买家名称
     */
    private final String buyerName;

    /**
     * 订单总金额
     */
    private final BigDecimal totalAmount;

    /**
     * 构造订单创建事件。
     *
     * @param orderId     订单 ID（未持久化时传 null）
     * @param orderNo     订单编号
     * @param buyerId     买家 ID
     * @param buyerName   买家名称
     * @param totalAmount 订单总金额
     */
    public OrderCreatedEvent(Long orderId, String orderNo, String buyerId, String buyerName, BigDecimal totalAmount) {
        // 订单未持久化时以 orderNo 充当事件源标识，持久化后由仓储重建事件
        super(orderId != null ? String.valueOf(orderId) : orderNo);
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.totalAmount = totalAmount;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
