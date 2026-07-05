package io.ddd4j.sample.quarkus.mq.rabbitmq.order.infrastructure;

import io.ddd4j.sample.quarkus.mq.rabbitmq.order.domain.Order;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单仓储（内存版实现）。
 *
 * <p>仅用于示例，生产环境应替换为 JPA / MyBatis / Redis 等持久化实现。
 * 仓储接口与持久化实现解耦，业务代码（OrderApplicationService）通过仓储抽象访问聚合根。
 *
 * <p>使用 {@link ConcurrentHashMap} 保证多线程环境下的安全性，与 Quarkus 的事件分发线程模型一致。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class InMemoryOrderRepository {

    private final Map<String, Order> storage = new ConcurrentHashMap<>();

    /**
     * 保存订单聚合根。
     *
     * @param order 待保存的订单
     * @return 已保存的订单
     */
    public Order save(Order order) {
        storage.put(order.getId(), order);
        return order;
    }

    /**
     * 按 ID 查询订单。
     *
     * @param id 订单 ID
     * @return 订单（可能为空）
     */
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}