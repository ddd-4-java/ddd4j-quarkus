package io.ddd4j.quarkus.sample.domain.order.model;

import java.util.Objects;

/**
 * 订单状态枚举（值对象）。
 *
 * <p>订单的完整状态机：{@link #CREATED} → {@link #PAID} → {@link #SHIPPED} → {@link #COMPLETED}，
 * 任意非终态可流转到 {@link #CANCELLED}（已发货 / 已完成除外）。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public enum OrderStatus {

    /**
     * 已创建（初始态）
     */
    CREATED("CREATED", "已创建"),
    /**
     * 已支付
     */
    PAID("PAID", "已支付"),
    /**
     * 已发货
     */
    SHIPPED("SHIPPED", "已发货"),
    /**
     * 已完成（终态）
     */
    COMPLETED("COMPLETED", "已完成"),
    /**
     * 已取消（终态）
     */
    CANCELLED("CANCELLED", "已取消");

    /**
     * 状态编码（持久化到数据库的值）
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String description;

    OrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取状态编码。
     *
     * @return 状态编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取状态描述。
     *
     * @return 状态描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 校验状态流转是否合法。
     *
     * @param target 目标状态
     * @return 允许流转返回 {@code true}
     */
    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case CREATED -> target == PAID || target == CANCELLED;
            case PAID -> target == SHIPPED || target == CANCELLED;
            case SHIPPED, COMPLETED, CANCELLED -> false;
        };
    }

    /**
     * 根据状态编码解析枚举。
     *
     * @param code 状态编码（数据库存储值）
     * @return 对应的订单状态
     * @throws IllegalArgumentException 未知的状态编码
     */
    public static OrderStatus fromCode(String code) {
        Objects.requireNonNull(code, "code must not be null");
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown order status code: " + code);
    }
}
