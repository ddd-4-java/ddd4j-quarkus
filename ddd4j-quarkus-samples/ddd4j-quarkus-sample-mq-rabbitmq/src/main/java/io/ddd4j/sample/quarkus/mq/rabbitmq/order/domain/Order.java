package io.ddd4j.sample.quarkus.mq.rabbitmq.order.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 订单聚合根（充血模型）。
 *
 * <p>本类体现 DDD 聚合根的设计原则：
 * <ul>
 *   <li>通过静态工厂方法 {@link #create(String, String, String)} 创建订单，对外屏蔽 ID 生成与状态初始化</li>
 *   <li>状态由 {@link OrderStatus} 枚举约束，状态机迁移通过方法（如 {@link #markPaid}）而非 setter</li>
 *   <li>对外只暴露必要的 getter，禁止 setter 破坏封装</li>
 * </ul>
 *
 * <p>本示例的 create() 不在聚合内注册领域事件（事件由应用服务显式构造并发布），
 * 这与 ddd4j-mq 设计一致：{@code OrderCreatedEvent} 由 {@code OrderApplicationService}
 * 通过 {@code MQEventPublisher} 发布到 RabbitMQ Topic Exchange，与 Disruptor / Kafka 示例完全相同。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Order {

    private final String id;
    private final String orderNo;
    private final String buyerId;
    private final String buyerName;
    private OrderStatus status;
    private BigDecimal totalAmount;

    /**
     * 全量构造器（用于从仓储加载聚合）。
     */
    public Order(String id, String orderNo, String buyerId, String buyerName, OrderStatus status, BigDecimal totalAmount) {
        this.id = id;
        this.orderNo = orderNo;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    /**
     * 静态工厂方法：创建新订单（聚合根的"业务行为入口"）。
     *
     * <p>ID 使用 UUID 自动生成，初始状态为 {@link OrderStatus#CREATED}，
     * 订单金额默认为 {@link BigDecimal#ZERO}，由后续添加 SKU 时累加。
     *
     * @param orderNo   订单编号（业务主键）
     * @param buyerId   买家 ID
     * @param buyerName 买家名称
     * @return 新创建的订单聚合根
     */
    public static Order create(String orderNo, String buyerId, String buyerName) {
        return new Order(
                UUID.randomUUID().toString(),
                orderNo,
                buyerId,
                buyerName,
                OrderStatus.CREATED,
                BigDecimal.ZERO
        );
    }

    /**
     * 标记订单已支付（状态机迁移：CREATED -> PAID）。
     *
     * <p>充血模型：业务行为由聚合根方法承担，不暴露 setter 让外部随意修改。
     */
    public void markPaid() {
        this.status = OrderStatus.PAID;
    }

    public String getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public String getBuyerId() { return buyerId; }
    public String getBuyerName() { return buyerName; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}