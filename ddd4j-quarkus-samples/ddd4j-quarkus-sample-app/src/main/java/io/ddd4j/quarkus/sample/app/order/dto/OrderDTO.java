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

    /**
     * 无参构造函数，供 MapStruct/JSON 转换框架实例化。
     */
    public OrderDTO() {
    }

    /**
     * 获取订单 ID（领域标识）。
     *
     * @return 订单 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置订单 ID（领域标识）。
     *
     * @param id 订单 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取订单编号（业务主键）。
     *
     * @return 订单编号
     */
    public String getOrderNo() {
        return orderNo;
    }

    /**
     * 设置订单编号（业务主键）。
     *
     * @param orderNo 订单编号
     */
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    /**
     * 获取买家 ID。
     *
     * @return 买家 ID
     */
    public String getBuyerId() {
        return buyerId;
    }

    /**
     * 设置买家 ID。
     *
     * @param buyerId 买家 ID
     */
    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    /**
     * 获取买家名称。
     *
     * @return 买家名称
     */
    public String getBuyerName() {
        return buyerName;
    }

    /**
     * 设置买家名称。
     *
     * @param buyerName 买家名称
     */
    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    /**
     * 获取订单状态。
     *
     * @return 订单状态
     */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * 设置订单状态。
     *
     * @param status 订单状态
     */
    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    /**
     * 获取订单总金额。
     *
     * @return 订单总金额
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /**
     * 设置订单总金额。
     *
     * @param totalAmount 订单总金额
     */
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    /**
     * 设置创建时间。
     *
     * @param createdTime 创建时间
     */
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}
