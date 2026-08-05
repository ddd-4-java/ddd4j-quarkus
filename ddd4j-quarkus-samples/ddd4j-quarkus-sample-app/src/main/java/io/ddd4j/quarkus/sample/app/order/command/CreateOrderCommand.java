package io.ddd4j.quarkus.sample.app.order.command;

import java.io.Serializable;
import java.util.Objects;

/**
 * 创建订单命令（应用层入参）。
 *
 * <p>遵循 CQRS 模式：命令（Command）表达「写」意图，与查询参数
 * {@link io.ddd4j.quarkus.sample.app.order.query.OrderQuery} 分离。
 * 使用 {@code record} 声明不可变命令，由适配层（Web 资源）反序列化后
 * 交给 {@link io.ddd4j.quarkus.sample.app.order.service.OrderApplicationService} 执行。</p>
 *
 * @param orderNo   订单编号（业务主键，必填）
 * @param buyerId   买家 ID（必填）
 * @param buyerName 买家名称（必填）
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public record CreateOrderCommand(String orderNo, String buyerId, String buyerName) implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 紧凑构造器：参数校验，保证命令不可为非法状态。
     */
    public CreateOrderCommand {
        Objects.requireNonNull(orderNo, "orderNo must not be null");
        Objects.requireNonNull(buyerId, "buyerId must not be null");
        Objects.requireNonNull(buyerName, "buyerName must not be null");
    }
}
