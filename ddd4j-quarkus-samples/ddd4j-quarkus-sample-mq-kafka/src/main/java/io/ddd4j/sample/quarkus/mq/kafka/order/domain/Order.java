package io.ddd4j.sample.quarkus.mq.kafka.order.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 订单聚合根（简化版）。
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

    public Order(String id, String orderNo, String buyerId, String buyerName, String status, BigDecimal totalAmount) {
        this.id = id;
        this.orderNo = orderNo;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    public static Order create(String orderNo, String buyerId, String buyerName) {
        return new Order(UUID.randomUUID().toString(), orderNo, buyerId, buyerName, "CREATED", BigDecimal.ZERO);
    }

    public String getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public String getBuyerId() { return buyerId; }
    public String getBuyerName() { return buyerName; }
    public String getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}
