package io.ddd4j.sample.quarkus.mq.disruptor.order.application;

import io.ddd4j.sample.quarkus.mq.disruptor.order.domain.Order;
import io.ddd4j.sample.quarkus.mq.disruptor.order.domain.OrderCreatedEvent;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

/**
 * 订单应用服务：编排创建订单用例并发布领域事件。
 *
 * <p>核心流程：
 * <ol>
 *   <li>创建 Order 聚合</li>
 *   <li>构建 OrderCreatedEvent</li>
 *   <li>通过 MQEvent.publish() 投递到 MQ Broker</li>
 *   <li>Broker → @MQEventListener 消费</li>
 * </ol>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class OrderApplicationService {

    /**
     * 创建订单并发布 OrderCreatedEvent。
     *
     * @param orderNo   订单编号
     * @param buyerId   买家 ID
     * @param buyerName 买家名称
     * @return 创建的订单
     */
    public Order createOrder(String orderNo, String buyerId, String buyerName) {
        Objects.requireNonNull(orderNo, "orderNo must not be null");
        Objects.requireNonNull(buyerId, "buyerId must not be null");
        Objects.requireNonNull(buyerName, "buyerName must not be null");

        // 1. 创建订单聚合
        Order order = Order.create(orderNo, buyerId, buyerName);

        // 2. 构建领域事件
        OrderCreatedEvent event = new OrderCreatedEvent(order.getId(), order.getOrderNo(), order.getBuyerName());

        // 3. 发布到 MQ（MQEvent.publish() → BaseContext 中的 MQClient 自动路由）
        event.publish();

        return order;
    }
}
