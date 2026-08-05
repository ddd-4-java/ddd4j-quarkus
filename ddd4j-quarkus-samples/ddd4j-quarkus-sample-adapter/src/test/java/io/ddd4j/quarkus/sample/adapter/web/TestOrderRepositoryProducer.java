package io.ddd4j.quarkus.sample.adapter.web;

import io.ddd4j.quarkus.sample.domain.order.model.aggregate.Order;
import io.ddd4j.quarkus.sample.domain.order.repository.OrderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试专用仓储装配（仅测试 classpath 生效）。
 *
 * <p>适配层模块本身不装配 {@link OrderRepository}（领域层只定义接口，
 * 实现由主应用模块 / 基础设施层提供）。本类在 @QuarkusTest 启动的
 * CDI 容器中生产内存版 {@link OrderRepository}，避免测试依赖数据库；
 * 语义与 {@code LayeredSampleConfig.InMemoryOrderRepository} 一致：
 * {@code save()} 在 ID 为空时回填自增 ID。</p>
 */
@ApplicationScoped
public class TestOrderRepositoryProducer {

    /**
     * 生产订单仓储（内存实现，单例）。
     *
     * @return 内存版订单仓储
     */
    @Produces
    @Singleton
    public OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }

    /**
     * 内存版订单仓储：与 {@code LayeredSampleConfig} 的内存实现保持相同语义。
     */
    static final class InMemoryOrderRepository implements OrderRepository {

        private final Map<Long, Order> storage = new ConcurrentHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1L);

        @Override
        public Optional<Order> findById(Long id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<Order> findByOrderNo(String orderNo) {
            return storage.values().stream()
                    .filter(order -> orderNo.equals(order.getOrderNo()))
                    .findFirst();
        }

        @Override
        public List<Order> findByBuyerId(String buyerId) {
            return storage.values().stream()
                    .filter(order -> buyerId.equals(order.getBuyerId()))
                    .toList();
        }

        @Override
        public Order save(Order aggregate) {
            Objects.requireNonNull(aggregate, "aggregate must not be null");
            if (aggregate.id() == null) {
                // 新增：回填主键（与基础设施层 Panache 实现语义一致）
                aggregate.setId(idGenerator.getAndIncrement());
            }
            storage.put(aggregate.id(), aggregate);
            return aggregate;
        }

        @Override
        public List<Order> findAll() {
            return List.copyOf(storage.values());
        }
    }
}
