package io.ddd4j.sample.quarkus.mq.rabbitmq.order.application;

import io.ddd4j.sample.quarkus.mq.rabbitmq.order.domain.Order;
import io.ddd4j.sample.quarkus.mq.rabbitmq.order.domain.event.OrderCreatedEvent;
import io.ddd4j.sample.quarkus.mq.rabbitmq.order.infrastructure.InMemoryOrderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Objects;

/**
 * 订单应用服务：编排创建订单用例、保存聚合根并发布 OrderCreatedEvent 领域事件。
 *
 * <p>核心流程：
 * <ol>
 *   <li>调用 {@link Order#create} 创建订单聚合根（充血模型）</li>
 *   <li>通过仓储持久化聚合根（示例使用 {@link InMemoryOrderRepository}）</li>
 *   <li>构造 OrderCreatedEvent（继承自 MQEvent，已设置 topic / tag）</li>
 *   <li>通过 MQEvent.publish() 发布事件到 MQ Broker</li>
 * </ol>
 *
 * <p>本类<b>不引用任何 RabbitMQ / Kafka / Disruptor 的具体 API</b>，
 * 完全通过 ddd4j 抽象（{@code MQEvent}）实现"业务零 MQ 框架耦合"。
 * 切换底层 MQ 只需修改 {@code pom.xml} 依赖与 {@code application.properties} 中的
 * {@code ddd4j.mq.broker} 配置。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class OrderApplicationService {

    private static final Logger log = Logger.getLogger(OrderApplicationService.class);

    private final InMemoryOrderRepository orderRepository;

    @Inject
    public OrderApplicationService(InMemoryOrderRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
    }

    /**
     * 创建订单：保存聚合 + 发布 OrderCreatedEvent。
     *
     * @param orderNo   订单编号（业务主键）
     * @param buyerId   买家 ID
     * @param buyerName 买家名称
     * @return 已持久化的订单聚合根
     */
    public Order createOrder(String orderNo, String buyerId, String buyerName) {
        Objects.requireNonNull(orderNo, "orderNo must not be null");
        Objects.requireNonNull(buyerId, "buyerId must not be null");
        Objects.requireNonNull(buyerName, "buyerName must not be null");

        // 1. 充血模型：调用 Order.create() 创建聚合
        Order order = Order.create(orderNo, buyerId, buyerName);

        // 2. 仓储持久化
        orderRepository.save(order);

        // 3. 构建领域事件（OrderCreatedEvent 构造时已设置 topic="ORDER" / tag="created"）
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getOrderNo(),
                order.getBuyerName());

        // 4. 发布到 MQ（MQEvent.publish() → BaseContext 中的 MQClient 自动路由）
        event.publish();

        log.infof("Order created: id=%s, orderNo=%s, buyerName=%s",
                order.getId(), order.getOrderNo(), order.getBuyerName());

        return order;
    }
}
