package io.ddd4j.sample.quarkus.mq.kafka.order.application;

import io.ddd4j.core.event.MQEventPublisher;
import io.ddd4j.sample.quarkus.mq.kafka.order.domain.Order;
import io.ddd4j.sample.quarkus.mq.kafka.order.domain.OrderCreatedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * 订单应用服务：编排创建订单用例并发布领域事件到 Kafka。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class OrderApplicationService {

    private final MQEventPublisher mqEventPublisher;

    @Inject
    public OrderApplicationService(MQEventPublisher mqEventPublisher) {
        this.mqEventPublisher = Objects.requireNonNull(mqEventPublisher, "mqEventPublisher must not be null");
    }

    /**
     * 创建订单并发布 OrderCreatedEvent。
     */
    public Order createOrder(String orderNo, String buyerId, String buyerName) {
        Order order = Order.create(orderNo, buyerId, buyerName);
        OrderCreatedEvent event = new OrderCreatedEvent(order.getId(), order.getOrderNo(), order.getBuyerName());

        // 发布到 Kafka（MQEventPublisher 实现透明切换）
        mqEventPublisher.publish(event);

        return order;
    }
}
