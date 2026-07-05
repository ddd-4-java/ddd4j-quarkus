package io.ddd4j.sample.quarkus.mq.kafka.mq;

import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.sample.quarkus.mq.kafka.order.domain.OrderCreatedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单创建事件 Kafka MQ 消费者。
 *
 * <p>使用 {@link MQEventListener} 声明订阅关系，与 Disruptor 示例的监听器代码完全一致。
 * 切换 MQ 时本类无需修改。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Slf4j
@ApplicationScoped
public class OrderCreatedMqListener {

    /**
     * 处理 OrderCreatedEvent（从 Kafka Topic 消费）。
     *
     * @param event 订单创建事件
     */
    @MQEventListener(topic = "ORDER", tags = "CREATED")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("==================================================");
        log.info("[Kafka MQ 消费者] 收到 OrderCreatedEvent！");
        log.info("  订单 ID  : {}", event.getOrderId());
        log.info("  订单编号 : {}", event.getOrderNo());
        log.info("  买家名称 : {}", event.getBuyerName());
        log.info("  Topic    : {}", event.getTopic());
        log.info("  Tag      : {}", event.getTag());
        log.info("==================================================");
    }
}
