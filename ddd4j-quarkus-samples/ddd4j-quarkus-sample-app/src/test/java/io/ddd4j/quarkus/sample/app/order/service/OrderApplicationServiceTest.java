package io.ddd4j.quarkus.sample.app.order.service;

import io.ddd4j.core.api.Page;
import io.ddd4j.quarkus.sample.app.order.command.CreateOrderCommand;
import io.ddd4j.quarkus.sample.app.order.dto.OrderDTO;
import io.ddd4j.quarkus.sample.app.order.mapper.OrderMapperImpl;
import io.ddd4j.quarkus.sample.app.order.query.OrderQuery;
import io.ddd4j.quarkus.sample.domain.order.model.OrderStatus;
import io.ddd4j.quarkus.sample.domain.order.model.aggregate.Order;
import io.ddd4j.quarkus.sample.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link OrderApplicationService} 单元测试。
 *
 * <p>纯 JUnit 5（不启动 Quarkus）：手工构造应用服务（内存版 {@link OrderRepository} +
 * MapStruct 生成的 {@code OrderMapperImpl}），验证用例编排：创建 → 查询 → 支付 → 取消 → 分页。</p>
 */
class OrderApplicationServiceTest {

    private InMemoryOrderRepository repository;
    private OrderApplicationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        service = new OrderApplicationService(repository, new OrderMapperImpl());
    }

    @Test
    void createOrderShouldPersistAndReturnDto() {
        OrderDTO dto = service.createOrder(
                new CreateOrderCommand("ORD-APP-001", "buyer-2001", "张三"));

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getOrderNo()).isEqualTo("ORD-APP-001");
        assertThat(dto.getBuyerId()).isEqualTo("buyer-2001");
        assertThat(dto.getBuyerName()).isEqualTo("张三");
        assertThat(dto.getStatus()).isEqualTo(OrderStatus.CREATED);

        // 仓储可见（save 回填自增 ID）
        Order persisted = repository.findById(1L).orElseThrow();
        assertThat(persisted.getOrderNo()).isEqualTo("ORD-APP-001");
        assertThat(persisted.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void getOrderShouldReturnPersistedOrder() {
        OrderDTO created = service.createOrder(
                new CreateOrderCommand("ORD-APP-002", "buyer-2001", "张三"));

        OrderDTO found = service.getOrder(created.getId());

        assertThat(found.getOrderNo()).isEqualTo("ORD-APP-002");
        assertThat(found.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void payOrderShouldTransitionStatusToPaid() {
        OrderDTO created = service.createOrder(
                new CreateOrderCommand("ORD-APP-003", "buyer-2001", "张三"));

        OrderDTO paid = service.payOrder(created.getId());

        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(repository.findById(created.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PAID);
    }

    @Test
    void cancelOrderShouldTransitionStatusToCancelled() {
        OrderDTO created = service.createOrder(
                new CreateOrderCommand("ORD-APP-004", "buyer-2001", "张三"));

        OrderDTO cancelled = service.cancelOrder(created.getId());

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void getOrderShouldThrowWhenOrderMissing() {
        assertThatThrownBy(() -> service.getOrder(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }

    @Test
    void payOrderShouldThrowWhenOrderMissing() {
        assertThatThrownBy(() -> service.payOrder(999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void payOrderShouldThrowWhenStatusIllegal() {
        OrderDTO created = service.createOrder(
                new CreateOrderCommand("ORD-APP-005", "buyer-2001", "张三"));
        service.payOrder(created.getId());

        // 已支付订单重复支付 → 状态流转非法
        assertThatThrownBy(() -> service.payOrder(created.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pageOrdersShouldFilterAndPaginate() {
        service.createOrder(new CreateOrderCommand("ORD-APP-101", "buyer-3001", "张三"));
        service.createOrder(new CreateOrderCommand("ORD-APP-102", "buyer-3002", "李四"));
        service.createOrder(new CreateOrderCommand("ORD-APP-103", "buyer-3002", "王五"));

        // 无条件分页：total=3，第一页 2 条
        Page<OrderDTO> firstPage = service.pageOrders(new OrderQuery(null, null, null), 1, 2);
        assertThat(firstPage.getTotal()).isEqualTo(3);
        assertThat(firstPage.getRecords()).hasSize(2);

        // 第二页剩余 1 条
        Page<OrderDTO> secondPage = service.pageOrders(new OrderQuery(null, null, null), 2, 2);
        assertThat(secondPage.getTotal()).isEqualTo(3);
        assertThat(secondPage.getRecords()).hasSize(1);

        // 按买家过滤
        Page<OrderDTO> filtered = service.pageOrders(new OrderQuery(null, "buyer-3002", null), 1, 10);
        assertThat(filtered.getTotal()).isEqualTo(2);

        // 按订单编号精确过滤
        Page<OrderDTO> byOrderNo = service.pageOrders(new OrderQuery("ORD-APP-101", null, null), 1, 10);
        assertThat(byOrderNo.getRecords()).extracting(OrderDTO::getOrderNo)
                .containsExactly("ORD-APP-101");
    }

    /**
     * 内存版订单仓储（测试用）：语义与 {@code LayeredSampleConfig.InMemoryOrderRepository}
     * 一致 —— {@code save()} 在 ID 为空时回填自增 ID，供 {@code findAll()} 内存过滤分页。
     */
    private static final class InMemoryOrderRepository implements OrderRepository {

        private final Map<Long, Order> storage = new ConcurrentHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1L);

        @Override
        public Optional<Order> findById(Long id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Order save(Order aggregate) {
            Objects.requireNonNull(aggregate, "aggregate must not be null");
            if (aggregate.id() == null) {
                aggregate.setId(idGenerator.getAndIncrement());
            }
            storage.put(aggregate.id(), aggregate);
            return aggregate;
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
        public List<Order> findAll() {
            return List.copyOf(storage.values());
        }
    }
}
