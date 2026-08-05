package io.ddd4j.quarkus.sample.domain.order.repository;

import io.ddd4j.core.ddd.repository.Repository;
import io.ddd4j.quarkus.sample.domain.order.model.aggregate.Order;

import java.util.List;
import java.util.Optional;

/**
 * 订单仓储接口（领域层）。
 *
 * <p>继承 {@link Repository} 获得完整 CRUD / 批量 / 分页契约
 * （{@code findById / save / updateById / deleteById / page ...}），
 * 并补充订单领域特有的查询方法。实现位于基础设施层
 * {@code ddd4j-quarkus-sample-infrastructure}。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public interface OrderRepository extends Repository<Order, Long> {

    /**
     * 根据订单编号查询订单。
     *
     * @param orderNo 订单编号
     * @return 订单（可能为空）
     */
    Optional<Order> findByOrderNo(String orderNo);

    /**
     * 根据买家 ID 查询订单列表（按创建时间倒序）。
     *
     * @param buyerId 买家 ID
     * @return 订单列表
     */
    List<Order> findByBuyerId(String buyerId);
}
