package io.ddd4j.quarkus.sample.client.order;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单客户端 DTO（对外 API 契约）。
 *
 * <p>客户端模块（Client Module）是分层架构中的接口契约层，本 DTO 定义了
 * 订单服务对外暴露的数据结构，供调用方（前端、网关、其他服务）使用。
 * 对外 DTO 与内部领域模型解耦：不暴露 {@code AggregateRoot} 的任何内部细节，
 * 状态以字符串形式返回（{@link #status()} 为 {@code OrderStatus} 枚举名称）。</p>
 *
 * <p>使用普通不可变类声明 DTO，显式保留 Jackson 属性与构造器映射，并通过 {@link Schema}
 * 注解生成 OpenAPI 文档。</p>
 *
 * 组件 id：订单 ID（领域标识）
 * 组件 orderNo：订单编号（业务主键）
 * 组件 buyerId：买家 ID
 * 组件 buyerName：买家名称
 * 组件 status：订单状态（CREATED/PAID/SHIPPED/COMPLETED/CANCELLED）
 * 组件 totalAmount：订单总金额
 * 组件 createdTime：创建时间
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Schema(description = "订单客户端 DTO（对外 API 契约）")
public final class OrderClientDTO implements Serializable {

    @Schema(description = "订单ID", example = "1")
    private final Long id;

    @Schema(description = "订单编号", example = "ORD202608040001")
    private final String orderNo;

    @Schema(description = "买家ID", example = "buyer-1001")
    private final String buyerId;

    @Schema(description = "买家名称", example = "张三")
    private final String buyerName;

    @Schema(description = "订单状态", example = "CREATED", allowableValues = { "CREATED", "PAID", "SHIPPED", "COMPLETED", "CANCELLED" })
    private final String status;

    @Schema(description = "订单总金额", example = "199.00")
    private final BigDecimal totalAmount;

    @Schema(description = "创建时间", example = "2026-08-04T10:00:00")
    private final LocalDateTime createdTime;

    private static final long serialVersionUID = 1L;

    @JsonCreator()
    public OrderClientDTO(@Schema(description = "订单ID", example = "1") @JsonProperty("id") Long id, @Schema(description = "订单编号", example = "ORD202608040001") @JsonProperty("orderNo") String orderNo, @Schema(description = "买家ID", example = "buyer-1001") @JsonProperty("buyerId") String buyerId, @Schema(description = "买家名称", example = "张三") @JsonProperty("buyerName") String buyerName, @Schema(description = "订单状态", example = "CREATED", allowableValues = { "CREATED", "PAID", "SHIPPED", "COMPLETED", "CANCELLED" }) @JsonProperty("status") String status, @Schema(description = "订单总金额", example = "199.00") @JsonProperty("totalAmount") BigDecimal totalAmount, @Schema(description = "创建时间", example = "2026-08-04T10:00:00") @JsonProperty("createdTime") LocalDateTime createdTime) {
        this.id = id;
        this.orderNo = orderNo;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdTime = createdTime;
    }

    @Schema(description = "订单ID", example = "1")
    @JsonProperty("id")
    public Long id() {
        return id;
    }

    @Schema(description = "订单编号", example = "ORD202608040001")
    @JsonProperty("orderNo")
    public String orderNo() {
        return orderNo;
    }

    @Schema(description = "买家ID", example = "buyer-1001")
    @JsonProperty("buyerId")
    public String buyerId() {
        return buyerId;
    }

    @Schema(description = "买家名称", example = "张三")
    @JsonProperty("buyerName")
    public String buyerName() {
        return buyerName;
    }

    @Schema(description = "订单状态", example = "CREATED", allowableValues = { "CREATED", "PAID", "SHIPPED", "COMPLETED", "CANCELLED" })
    @JsonProperty("status")
    public String status() {
        return status;
    }

    @Schema(description = "订单总金额", example = "199.00")
    @JsonProperty("totalAmount")
    public BigDecimal totalAmount() {
        return totalAmount;
    }

    @Schema(description = "创建时间", example = "2026-08-04T10:00:00")
    @JsonProperty("createdTime")
    public LocalDateTime createdTime() {
        return createdTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        OrderClientDTO other = (OrderClientDTO) obj;
        return Objects.equals(this.id, other.id) && Objects.equals(this.orderNo, other.orderNo) && Objects.equals(this.buyerId, other.buyerId) && Objects.equals(this.buyerName, other.buyerName) && Objects.equals(this.status, other.status) && Objects.equals(this.totalAmount, other.totalAmount) && Objects.equals(this.createdTime, other.createdTime);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(orderNo);
        result = 31 * result + Objects.hashCode(buyerId);
        result = 31 * result + Objects.hashCode(buyerName);
        result = 31 * result + Objects.hashCode(status);
        result = 31 * result + Objects.hashCode(totalAmount);
        result = 31 * result + Objects.hashCode(createdTime);
        return result;
    }

    @Override
    public String toString() {
        return "OrderClientDTO[id=" + id + ", orderNo=" + orderNo + ", buyerId=" + buyerId + ", buyerName=" + buyerName + ", status=" + status + ", totalAmount=" + totalAmount + ", createdTime=" + createdTime + "]";
    }
}
