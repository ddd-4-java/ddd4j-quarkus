package io.ddd4j.sample.quarkus.mq.disruptor.order.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 订单聚合根（简化版）。
 *
 * <p>演示 DDD 聚合根的创建与事件注册：
 * <ul>
 *   <li>通过静态工厂方法 {@link #create} 创建订单</li>
 *   <li>构造时生成 OrderCreatedEvent</li>
 *   <li>应用服务在事务提交后通过 MQEventPublisher 发布事件</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Order {

    private final String id;
    private final String orderNo;
    private final String buyerId;
    private final String buyerName;
    private final String status;
    private final BigDecimal totalAmount;

    /**
     * 构造 Order 实例。
     * @param id  标识
     * @param orderNo  订单编号
     * @param buyerId  买家标识
     * @param buyerName  买家名称
     * @param status  状态
     * @param totalAmount  总数金额
     */
    public Order(String id, String orderNo, String buyerId, String buyerName, String status, BigDecimal totalAmount) {
        this.id = id;
        this.orderNo = orderNo;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    /**
     * 静态工厂方法：创建新订单。
     * @param buyerId  买家标识
     * @param buyerName  买家名称
     * @param orderNo  订单编号
     * @return 创建
     */
    public static Order create(String orderNo, String buyerId, String buyerName) {
        return new Order(
                UUID.randomUUID().toString(),
                orderNo,
                buyerId,
                buyerName,
                "CREATED",
                BigDecimal.ZERO
        );
    }

    /**
     * 获取标识。
     * @return 标识
     */
    public String getId() { return id; }
    /**
     * 获取订单编号。
     * @return 订单编号
     */
    public String getOrderNo() { return orderNo; }
    /**
     * 获取买家标识。
     * @return 买家标识
     */
    public String getBuyerId() { return buyerId; }
    /**
     * 获取买家名称。
     * @return 买家名称
     */
    public String getBuyerName() { return buyerName; }
    /**
     * 获取状态。
     * @return 状态
     */
    public String getStatus() { return status; }
    /**
     * 获取总数金额。
     * @return 总数金额
     */
    public BigDecimal getTotalAmount() { return totalAmount; }
}
