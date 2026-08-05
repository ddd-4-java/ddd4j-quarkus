package io.ddd4j.quarkus.sample.layered.config;

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

/**
 * 分层示例 CDI 装配（主应用模块）。
 *
 * <p>Quarkus Arc 自动发现各分层模块中的 CDI Bean：</p>
 * <ul>
 *   <li>{@code app} 模块的 {@code OrderApplicationService}（{@code @ApplicationScoped}）</li>
 *   <li>{@code app} 模块 MapStruct 生成的 {@code OrderMapper}（{@code @Mapper(componentModel = "jakarta")}）</li>
 *   <li>{@code adapter} 模块的 {@code OrderResource}（JAX-RS 资源）与异常映射器</li>
 * </ul>
 *
 * <p>唯一需要显式装配的是<b>仓储实现</b>（领域层只定义接口 {@link OrderRepository}）：
 * 本类通过 {@link Produces} 提供内存版 {@link InMemoryOrderRepository}，
 * 保证示例开箱即跑（无需数据库）。生产环境可引入
 * {@code ddd4j-quarkus-sample-infrastructure} 模块（其
 * {@code OrderInfrastructureConfig} 已 {@code @Produces} Panache 版
 * {@code OrderRepositoryImpl}），并移除本类中的 {@code orderRepository()} 方法，
 * 避免出现两个 {@link OrderRepository} Bean。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class LayeredSampleConfig {

    /**
     * 装配订单仓储（内存实现）。
     *
     * <p>约定：{@link Order#create(String, String, String)} 创建聚合根时
     * ID 为 {@code null}（持久化后回填），因此 {@code save} 在 ID 为空时
     * 通过 {@code setId} 回填自增 ID（与 Panache 版实现回填主键的语义一致）。</p>
     *
     * @return 订单仓储（单例）
     */
    @Produces
    @Singleton
    public OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }

    /**
     * 内存版订单仓储：仅用于分层示例演示。
     *
     * <p>使用 {@link ConcurrentHashMap} 保证多线程安全；实现 {@code findAll()}
     * 供应用层 {@code pageOrders} 做内存过滤分页（生产环境由数据库下推）。</p>
     */
    static final class InMemoryOrderRepository implements OrderRepository {

        private final Map<Long, Order> storage = new ConcurrentHashMap<>();
        private final java.util.concurrent.atomic.AtomicLong idGenerator = new java.util.concurrent.atomic.AtomicLong(1L);

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
