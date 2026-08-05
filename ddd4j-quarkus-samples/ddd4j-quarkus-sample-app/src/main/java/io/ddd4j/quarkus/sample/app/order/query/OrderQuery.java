package io.ddd4j.quarkus.sample.app.order.query;

import io.ddd4j.quarkus.sample.domain.order.model.OrderStatus;

/**
 * 订单查询参数（应用层查询条件）。
 *
 * <p>遵循 CQRS 模式：查询（Query）表达「读」意图，与创建命令
 * {@link io.ddd4j.quarkus.sample.app.order.command.CreateOrderCommand} 分离。
 * 所有条件均可为空，为空表示不参与过滤。</p>
 *
 * @param orderNo 订单编号（精确匹配）
 * @param buyerId 买家 ID（精确匹配）
 * @param status  订单状态（精确匹配）
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public record OrderQuery(String orderNo, String buyerId, OrderStatus status) {

    /**
     * 是否为空查询条件（不参与任何过滤）。
     *
     * @return {@code true} 表示无条件过滤，返回全部订单
     */
    public boolean isEmpty() {
        return orderNo == null && buyerId == null && status == null;
    }
}
