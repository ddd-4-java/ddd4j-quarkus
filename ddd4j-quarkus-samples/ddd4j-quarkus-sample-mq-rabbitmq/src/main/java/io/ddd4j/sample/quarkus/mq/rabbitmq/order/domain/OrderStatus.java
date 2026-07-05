package io.ddd4j.sample.quarkus.mq.rabbitmq.order.domain;

/**
 * 订单状态枚举（充血模型中的状态字段类型）。
 *
 * <p>体现 DDD 中"值对象/枚举"的语义约束：状态机的合法迁移由聚合根方法控制，
 * 不允许外部直接 set 任意字符串。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public enum OrderStatus {

    /** 已创建（初始状态） */
    CREATED,
    /** 已支付 */
    PAID,
    /** 已发货 */
    SHIPPED,
    /** 已完成 */
    COMPLETED,
    /** 已取消 */
    CANCELLED
}