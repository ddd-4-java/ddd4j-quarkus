package io.ddd4j.sample.quarkus.mq.disruptor.mq;

import io.ddd4j.sample.quarkus.mq.disruptor.order.domain.OrderCreatedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 订单消费投影：保存真实 MQ listener 收到的订单，演示异步读模型。 */
@ApplicationScoped
public class ConsumedOrderProjection {
    private final Map<String, OrderCreatedEvent> orders = new ConcurrentHashMap<>();

    /** 由 MQ listener 记录已消费的订单事件。 */
    public void record(OrderCreatedEvent event) {
        orders.put(event.getOrderId(), event);
    }

    /** 按订单 ID 查询消费结果。 */
    public Optional<OrderCreatedEvent> find(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    /** 清空演示投影，供重新演示与测试隔离使用。 */
    public void clear() {
        orders.clear();
    }
}
