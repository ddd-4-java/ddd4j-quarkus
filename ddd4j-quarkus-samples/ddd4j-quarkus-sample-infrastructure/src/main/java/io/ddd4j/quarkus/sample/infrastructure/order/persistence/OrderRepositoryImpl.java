package io.ddd4j.quarkus.sample.infrastructure.order.persistence;

import io.ddd4j.core.ddd.event.DomainEvent;
import io.ddd4j.quarkus.sample.domain.order.event.OrderCreatedEvent;
import io.ddd4j.quarkus.sample.domain.order.model.OrderStatus;
import io.ddd4j.quarkus.sample.domain.order.model.aggregate.Order;
import io.ddd4j.quarkus.sample.domain.order.repository.OrderRepository;
import io.ddd4j.quarkus.sample.infrastructure.order.persistence.entity.OrderEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 订单仓储实现（基础设施层）。
 *
 * <p>实现领域层 {@link OrderRepository} 契约，内部使用 Panache
 * {@link OrderEntity} 的静态方法 {@code find / list / persist / deleteById} 完成
 * 数据访问；写操作标注 {@link Transactional}。</p>
 *
 * <p>聚合根与持久化实体的映射（{@code toEntity / copyTo / toDomain}）内聚在本类，
 * 实体 {@code status} 字段存储 {@link OrderStatus#getCode()} 编码值。</p>
 *
 * <p>本类为纯 Java 类（非 CDI Bean），由
 * {@link io.ddd4j.quarkus.sample.infrastructure.order.config.OrderInfrastructureConfig}
 * 的 {@code @Produces} 方法装配并注册到 {@code RepositoryRegistry}，
 * 与 ddd4j-quarkus-sample-rich-model 的装配模式保持一致。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class OrderRepositoryImpl implements OrderRepository {

    @Override
    @Transactional
    public Order save(Order aggregate) {
        if (aggregate.id() == null) {
            // 新增：插入实体后回填主键
            OrderEntity entity = toEntity(aggregate);
            OrderEntity.persist(entity);
            aggregate.setId(entity.id);
        } else {
            // 更新：复制字段到托管实体，Panache 脏检查在事务提交时自动 flush
            Optional<OrderEntity> optional = OrderEntity.findByIdOptional(aggregate.id());
            OrderEntity entity = optional.orElseThrow(
                    () -> new IllegalArgumentException("Order not found: " + aggregate.id()));
            copyTo(entity, aggregate);
            entity.persist();
        }
        // 持久化成功后发布领域事件
        publishEvents(aggregate);
        return aggregate;
    }

    @Override
    public Optional<Order> findById(Long id) {
        Optional<OrderEntity> optional = OrderEntity.findByIdOptional(id);
        return optional.map(this::toDomain);
    }

    @Override
    public Optional<Order> findByOrderNo(String orderNo) {
        PanacheQuery<OrderEntity> query = OrderEntity.find("orderNo", orderNo);
        Optional<OrderEntity> optional = query.firstResultOptional();
        return optional.map(this::toDomain);
    }

    @Override
    public List<Order> findByBuyerId(String buyerId) {
        List<OrderEntity> entities = OrderEntity.list("buyerId", buyerId);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public List<Order> findAll() {
        List<OrderEntity> entities = OrderEntity.listAll();
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        OrderEntity.deleteById(id);
    }

    /**
     * 发布聚合根上未提交的领域事件。
     *
     * <p>订单创建事件在持久化前 {@code orderId} 为 null，
     * 此处回填主键后重建携带真实 ID 的事件再发布。</p>
     *
     * @param aggregate 已持久化的订单聚合根
     */
    private void publishEvents(Order aggregate) {
        for (DomainEvent<?> event : aggregate.pullDomainEvents()) {
            if (event instanceof OrderCreatedEvent created && created.getOrderId() == null) {
                new OrderCreatedEvent(aggregate.id(), aggregate.getOrderNo(), aggregate.getBuyerId(),
                        aggregate.getBuyerName(), aggregate.getTotalAmount()).publish();
            } else {
                event.publish();
            }
        }
    }

    /**
     * 聚合根 → 持久化实体（新增场景）。
     */
    private OrderEntity toEntity(Order aggregate) {
        OrderEntity entity = new OrderEntity();
        entity.orderNo = aggregate.getOrderNo();
        entity.buyerId = aggregate.getBuyerId();
        entity.buyerName = aggregate.getBuyerName();
        entity.status = aggregate.getStatus().getCode();
        entity.totalAmount = aggregate.getTotalAmount();
        entity.createdTime = aggregate.getCreatedTime();
        return entity;
    }

    /**
     * 聚合根 → 托管实体（更新场景，逐字段拷贝）。
     */
    private void copyTo(OrderEntity entity, Order aggregate) {
        entity.orderNo = aggregate.getOrderNo();
        entity.buyerId = aggregate.getBuyerId();
        entity.buyerName = aggregate.getBuyerName();
        entity.status = aggregate.getStatus().getCode();
        entity.totalAmount = aggregate.getTotalAmount();
        entity.createdTime = aggregate.getCreatedTime();
    }

    /**
     * 持久化实体 → 聚合根（通过默认构造器 + setter 还原，不触发创建事件）。
     */
    private Order toDomain(OrderEntity entity) {
        Order order = new Order();
        order.setId(entity.id);
        order.setOrderNo(entity.orderNo);
        order.setBuyerId(entity.buyerId);
        order.setBuyerName(entity.buyerName);
        order.setStatus(OrderStatus.fromCode(entity.status));
        order.setTotalAmount(entity.totalAmount);
        order.setCreatedTime(entity.createdTime);
        return order;
    }
}
