package io.ddd4j.quarkus.sample.app.order.command;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;
import java.util.Objects;

/**
 * 创建订单命令（应用层入参）。
 *
 * <p>遵循 CQRS 模式：命令（Command）表达「写」意图，与查询参数
 * {@link io.ddd4j.quarkus.sample.app.order.query.OrderQuery} 分离。
 * 使用普通不可变类声明命令，由适配层（Web 资源）反序列化后
 * 交给 {@link io.ddd4j.quarkus.sample.app.order.service.OrderApplicationService} 执行。</p>
 *
 * 组件 orderNo：订单编号（业务主键，必填）
 * 组件 buyerId：买家 ID（必填）
 * 组件 buyerName：买家名称（必填）
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public final class CreateOrderCommand implements Serializable {

    private final String orderNo;

    private final String buyerId;

    private final String buyerName;

    private static final long serialVersionUID = 1L;

    /**
     * 紧凑构造器：参数校验，保证命令不可为非法状态。
     */
    @JsonCreator()
    public CreateOrderCommand(@JsonProperty("orderNo") String orderNo, @JsonProperty("buyerId") String buyerId, @JsonProperty("buyerName") String buyerName) {
        Objects.requireNonNull(orderNo, "orderNo must not be null");
        Objects.requireNonNull(buyerId, "buyerId must not be null");
        Objects.requireNonNull(buyerName, "buyerName must not be null");
        this.orderNo = orderNo;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
    }

    @JsonProperty("orderNo")
    public String orderNo() {
        return orderNo;
    }

    @JsonProperty("buyerId")
    public String buyerId() {
        return buyerId;
    }

    @JsonProperty("buyerName")
    public String buyerName() {
        return buyerName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        CreateOrderCommand other = (CreateOrderCommand) obj;
        return Objects.equals(this.orderNo, other.orderNo) && Objects.equals(this.buyerId, other.buyerId) && Objects.equals(this.buyerName, other.buyerName);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(orderNo);
        result = 31 * result + Objects.hashCode(buyerId);
        result = 31 * result + Objects.hashCode(buyerName);
        return result;
    }

    @Override
    public String toString() {
        return "CreateOrderCommand[orderNo=" + orderNo + ", buyerId=" + buyerId + ", buyerName=" + buyerName + "]";
    }
}
