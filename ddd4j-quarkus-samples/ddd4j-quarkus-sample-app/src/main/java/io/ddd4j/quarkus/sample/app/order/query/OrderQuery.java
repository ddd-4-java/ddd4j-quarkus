package io.ddd4j.quarkus.sample.app.order.query;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

import io.ddd4j.quarkus.sample.domain.order.model.OrderStatus;

/**
 * 订单查询参数（应用层查询条件）。
 *
 * <p>遵循 CQRS 模式：查询（Query）表达「读」意图，与创建命令
 * {@link io.ddd4j.quarkus.sample.app.order.command.CreateOrderCommand} 分离。
 * 所有条件均可为空，为空表示不参与过滤。</p>
 *
 * 组件 orderNo：订单编号（精确匹配）
 * 组件 buyerId：买家 ID（精确匹配）
 * 组件 status：订单状态（精确匹配）
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public final class OrderQuery {

    private static final long serialVersionUID = 0L;

    private final String orderNo;

    private final String buyerId;

    private final OrderStatus status;

    /**
     * 是否为空查询条件（不参与任何过滤）。
     *
     * @return {@code true} 表示无条件过滤，返回全部订单
     */
    public boolean isEmpty() {
        return orderNo == null && buyerId == null && status == null;
    }

    @JsonCreator()
    public OrderQuery(@JsonProperty("orderNo") String orderNo, @JsonProperty("buyerId") String buyerId, @JsonProperty("status") OrderStatus status) {
        this.orderNo = orderNo;
        this.buyerId = buyerId;
        this.status = status;
    }

    @JsonProperty("orderNo")
    public String orderNo() {
        return orderNo;
    }

    @JsonProperty("buyerId")
    public String buyerId() {
        return buyerId;
    }

    @JsonProperty("status")
    public OrderStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        OrderQuery other = (OrderQuery) obj;
        return Objects.equals(this.orderNo, other.orderNo) && Objects.equals(this.buyerId, other.buyerId) && Objects.equals(this.status, other.status);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(orderNo);
        result = 31 * result + Objects.hashCode(buyerId);
        result = 31 * result + Objects.hashCode(status);
        return result;
    }

    @Override
    public String toString() {
        return "OrderQuery[orderNo=" + orderNo + ", buyerId=" + buyerId + ", status=" + status + "]";
    }
}
