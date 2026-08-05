package io.ddd4j.quarkus.sample.app.order.dto;

import io.ddd4j.quarkus.sample.domain.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单应用层 DTO。
 *
 * <p>应用层（Application Layer）通过 DTO 与适配层交互：
 * 领域对象（聚合根）在应用服务内部完成状态流转后，由
 * {@link io.ddd4j.quarkus.sample.app.order.mapper.OrderMapper} 转换为
 * 本 DTO 返回给上层，避免领域内部结构泄漏到适配层。</p>
 *
 * <p>对外发布时再由适配层转换为客户端契约
 * {@code io.ddd4j.quarkus.sample.client.order.OrderClientDTO}。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class OrderDTO {

    /** 订单 ID（领域标识） */
    private Long id;
    /** 订单编号（业务主键） */
    private String orderNo;
    /** 买家 ID */
    private String buyerId;
    /** 买家名称 */
    private String buyerName;
    /** 订单状态 */
    private OrderStatus status;
    /** 订单总金额 */
    private BigDecimal totalAmount;
    /** 创建时间 */
    private LocalDateTime createdTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}
