package io.ddd4j.sample.quarkus.mq.kafka.order.application;

import io.ddd4j.sample.quarkus.mq.kafka.order.domain.Order;
import io.ddd4j.sample.quarkus.mq.kafka.order.domain.OrderCreatedEvent;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

/**
 * 订单应用服务：编排创建订单用例并发布领域事件到 Kafka。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class OrderApplicationService {

    /**
     * 创建订单并发布 OrderCreatedEvent。
     */
    public Order createOrder(String orderNo, String buyerId, String buyerName) {
        Objects.requireNonNull(orderNo, "orderNo must not be null");
        Objects.requireNonNull(buyerId, "buyerId must not be null");
        Objects.requireNonNull(buyerName, "buyerName must not be null");

        Order order = Order.create(orderNo, buyerId, buyerName);
        OrderCreatedEvent event = new OrderCreatedEvent(order.getId(), order.getOrderNo(), order.getBuyerName());

        // 发布到 Kafka（MQEvent.publish() → BaseContext 中的 MQClient 自动路由）
        event.publish();

        return order;
    }
}
