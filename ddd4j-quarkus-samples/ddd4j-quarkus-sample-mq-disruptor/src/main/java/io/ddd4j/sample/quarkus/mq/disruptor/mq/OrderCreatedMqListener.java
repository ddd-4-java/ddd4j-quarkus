package io.ddd4j.sample.quarkus.mq.disruptor.mq;

import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.sample.quarkus.mq.disruptor.order.domain.OrderCreatedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单创建事件 MQ 消费者。
 *
 * <p>使用 ddd4j {@link MQEventListener} 注解声明订阅关系：
 * <ul>
 *   <li>{@code topic="ORDER"}：业务主题（与 OrderCreatedEvent 的 topic 一致）</li>
 *   <li>{@code tags="CREATED"}：只消费 CREATED 标签的事件</li>
 * </ul>
 *
 * <p>切换为 Kafka / RabbitMQ 时，本类代码完全无需修改，
 * 仅需替换 pom 依赖与 application.yml 配置。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Slf4j
@ApplicationScoped
public class OrderCreatedMqListener {

    /**
     * 处理 OrderCreatedEvent。
     *
     * <p>方法参数必须是具体的事件类型，ddd4j 会通过反射调用并将反序列化后的载荷传入。
     *
     * @param event 订单创建事件
     */
    @MQEventListener(topic = "ORDER", tags = "CREATED")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("==================================================");
        log.info("[MQ 消费者] 收到 OrderCreatedEvent！");
        log.info("  订单 ID  : {}", event.getOrderId());
        log.info("  订单编号 : {}", event.getOrderNo());
        log.info("  买家名称 : {}", event.getBuyerName());
        log.info("  Topic    : {}", event.getTopic());
        log.info("  Tag      : {}", event.getTag());
        log.info("==================================================");
    }
}
