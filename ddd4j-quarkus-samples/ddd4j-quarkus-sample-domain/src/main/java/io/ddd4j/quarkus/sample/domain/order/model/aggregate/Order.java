package io.ddd4j.quarkus.sample.domain.order.model.aggregate;

import io.ddd4j.core.ddd.model.AggregateRoot;
import io.ddd4j.quarkus.sample.domain.order.event.OrderCancelledEvent;
import io.ddd4j.quarkus.sample.domain.order.event.OrderCreatedEvent;
import io.ddd4j.quarkus.sample.domain.order.event.OrderPaidEvent;
import io.ddd4j.quarkus.sample.domain.order.event.OrderShippedEvent;
import io.ddd4j.quarkus.sample.domain.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 订单聚合根（充血模型）。
 *
 * <p>继承 {@link AggregateRoot}，获得充血持久化（{@code save() / update() / delete()}）、
 * 充血查询（{@code AggregateRoot.get/list/page(...)}）与领域事件注册
 * （{@code registerEvent}）能力。本类只承载领域规则，不依赖任何持久化框架。</p>
 *
 * <p>状态机：{@link OrderStatus#CREATED} → {@link OrderStatus#PAID} →
 * {@link OrderStatus#SHIPPED} → {@link OrderStatus#COMPLETED}；
 * 已创建 / 已支付订单可取消。每次状态变更注册对应领域事件，
 * 由基础设施层在持久化成功后通过 {@code publish()} 统一发布。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class Order extends AggregateRoot<Long> {

    /**
     * 无参构造函数。
     */
    public Order() {
    }

    /**
     * 订单 ID（持久化后回填）
     */
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 买家 ID
     */
    private String buyerId;

    /**
     * 买家名称
     */
    private String buyerName;

    /**
     * 订单状态
     */
    private OrderStatus status;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 创建订单（工厂方法）。
     *
     * <p>初始状态 {@link OrderStatus#CREATED}、总金额 {@link BigDecimal#ZERO}，
     * 并注册 {@link OrderCreatedEvent}（此时订单未持久化，事件中 orderId 为 null）。</p>
     *
     * @param orderNo   订单编号（非空）
     * @param buyerId   买家 ID（非空）
     * @param buyerName 买家名称（非空）
     * @return 新创建的订单
     */
    public static Order create(String orderNo, String buyerId, String buyerName) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("orderNo must not be blank");
        }
        if (buyerId == null || buyerId.isBlank()) {
            throw new IllegalArgumentException("buyerId must not be blank");
        }
        if (buyerName == null || buyerName.isBlank()) {
            throw new IllegalArgumentException("buyerName must not be blank");
        }
        Order order = new Order();
        order.orderNo = orderNo;
        order.buyerId = buyerId;
        order.buyerName = buyerName;
        order.status = OrderStatus.CREATED;
        order.totalAmount = BigDecimal.ZERO;
        order.createdTime = LocalDateTime.now();
        order.registerEvent(new OrderCreatedEvent(
                null, orderNo, buyerId, buyerName, order.totalAmount));
        return order;
    }

    /**
     * 支付订单：{@link OrderStatus#CREATED} → {@link OrderStatus#PAID}。
     */
    public void pay() {
        assertStatus(OrderStatus.CREATED, "only created order can be paid");
        this.status = OrderStatus.PAID;
        registerEvent(new OrderPaidEvent(id, orderNo));
    }

    /**
     * 发货：{@link OrderStatus#PAID} → {@link OrderStatus#SHIPPED}。
     */
    public void ship() {
        assertStatus(OrderStatus.PAID, "only paid order can be shipped");
        this.status = OrderStatus.SHIPPED;
        registerEvent(new OrderShippedEvent(id, orderNo));
    }

    /**
     * 取消订单：{@link OrderStatus#CREATED} / {@link OrderStatus#PAID} → {@link OrderStatus#CANCELLED}。
     *
     * <p>已发货、已完成、已取消的订单不允许取消。</p>
     */
    public void cancel() {
        if (this.status == OrderStatus.SHIPPED) {
            throw new IllegalStateException("shipped order cannot be cancelled");
        }
        if (this.status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("completed order cannot be cancelled");
        }
        if (this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("order already cancelled");
        }
        this.status = OrderStatus.CANCELLED;
        registerEvent(new OrderCancelledEvent(id, orderNo));
    }

    private void assertStatus(OrderStatus expected, String message) {
        if (this.status != expected) {
            throw new IllegalStateException(message + " (current: " + this.status + ")");
        }
    }

    @Override
    public Long id() {
        return id;
    }

    /**
     * 获取标识。
     * @return 标识
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置标识。
     * @param id  标识
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取订单编号。
     * @return 订单编号
     */
    public String getOrderNo() {
        return orderNo;
    }

    /**
     * 设置订单编号。
     * @param orderNo  订单编号
     */
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    /**
     * 获取买家标识。
     * @return 买家标识
     */
    public String getBuyerId() {
        return buyerId;
    }

    /**
     * 设置买家标识。
     * @param buyerId  买家标识
     */
    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    /**
     * 获取买家名称。
     * @return 买家名称
     */
    public String getBuyerName() {
        return buyerName;
    }

    /**
     * 设置买家名称。
     * @param buyerName  买家名称
     */
    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    /**
     * 获取状态。
     * @return 状态
     */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * 设置状态。
     * @param status  状态
     */
    public void setStatus(OrderStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    /**
     * 获取总数金额。
     * @return 总数金额
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /**
     * 设置总数金额。
     * @param totalAmount  总数金额
     */
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * 获取创建时间。
     * @return 创建时间
     */
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    /**
     * 设置创建时间。
     * @param createdTime  创建时间
     */
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    @Override
    public String toString() {
        return "Order{id=" + id + ", orderNo='" + orderNo + "', buyerId='" + buyerId
                + "', status=" + status + ", totalAmount=" + totalAmount + '}';
    }
}
