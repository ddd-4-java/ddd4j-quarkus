package io.ddd4j.quarkus.mq.disruptor;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.disruptor.DisruptorMQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.quarkus.mq.testcontainers.AbstractMqQuarkusIntegrationTest;
import io.ddd4j.quarkus.mq.testcontainers.DisruptorQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * disruptor MQ 集成测试。
 *
 * <p>验证：
 * <ul>
 *   <li>{@link MQClient} Bean 被 CDI 正确解析，且 impl() = "disruptor"</li>
 *   <li>{@link MQProperties} Bean 存在且 broker = "DISRUPTOR"</li>
 *   <li>{@link MQEventSerialization} Bean 存在且可注入</li>
 *   <li>进程内 round-trip：{@code OrderCreatedEvent.publish()} → LMAX RingBuffer →
 *       {@code @MQEventListener} 监听器收到事件（继承 {@link AbstractMqQuarkusIntegrationTest}
 *       round-trip 骨架；Disruptor 无容器，不走 Docker 守护探测）</li>
 * </ul>
 *
 * <p>测试使用内嵌 {@link DisruptorTestResource}（委托 {@link DisruptorQuarkusTestResource} 的 start/stop），
 * 并通过 {@code @QuarkusTestResource} 自动注入配置到 Quarkus 运行时。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusTest
@QuarkusTestResource(DisruptorQuarkusIntegrationTest.DisruptorTestResource.class)
class DisruptorQuarkusIntegrationTest extends AbstractMqQuarkusIntegrationTest<DisruptorMQProperties> {

    @Inject
    DisruptorMQProperties disruptorProperties;

    @Override
    protected DisruptorMQProperties mqPropertiesExtension() {
        return disruptorProperties;
    }

    @Override
    protected void applyContainerProperties(DisruptorMQProperties properties) {
        // 进程内环形队列：无容器连接信息需要填充
    }

    @Test
    void shouldInjectMQClient() {
        Assertions.assertThat(mqClient).isNotNull();
        Assertions.assertThat(mqClient.impl()).isEqualTo("disruptor");
    }

    @Test
    void shouldInjectMQProperties() {
        Assertions.assertThat(mqProperties).isNotNull();
        Assertions.assertThat(mqProperties.isEnabled()).isTrue();
        Assertions.assertThat(mqProperties.getBroker()).isEqualTo("DISRUPTOR");
    }

    @Test
    void shouldInjectSerialization() {
        Assertions.assertThat(serialization).isNotNull();
        // 验证序列化 round-trip
        String json = serialization.serialize(Map.of("key", "value"));
        Assertions.assertThat(json).contains("key");
    }

    /**
     * 进程内端到端：OrderCreatedEvent 发布 → Disruptor RingBuffer → 监听器消费。
     *
     * <p><b>route 模型差异</b>（主仓 ddd4j-mq-disruptor 既有语义，本轮不改主仓）：
     * 生产者把物理地址 {@code namespace.topic} 存入 RingBuffer 的 topic 字段，
     * {@code onEvent} 用 {@code DisruptorEvent.getRouteExpression()}（namespace 字段
     * 非空才拼前缀）与监听器 {@code MQListener.getRouteExpression}（空 namespace 也拼
     * 前导分隔符）做精确比对。对齐两侧的唯一组合：
     * <ul>
     *   <li>{@code ddd4j.mq.namespace=it} —— 物理地址带 ns（{@code it.ORDER}），而
     *       DisruptorEvent.namespace 取自 event 字段（null → 不拼前缀），route 即 {@code it.ORDER}</li>
     *   <li>监听器 annotation {@code namespace="it"} + {@code tags="*"} —— route 同为 {@code it.ORDER}</li>
     *   <li>事件 tag 置空 —— 避免事件侧在已含 tag 的物理地址上二次拼 tag</li>
     * </ul>
     * 由此 {@code MQEvent.publish()} → 生产者 → RingBuffer → 路由匹配 → 反序列化 →
     * 反射监听的全管道真实走通。
     */
    @Test
    void shouldPublishAndConsumeOrderCreatedEventEndToEnd() throws Exception {
        NoTagListener bean = new NoTagListener();
        Method method = NoTagListener.class.getDeclaredMethod("onOrderCreated", OrderCreatedEvent.class);
        MQListener listener = MQListener.of(bean, method, method.getAnnotation(MQEventListener.class));

        String originalBroker = mqProperties.getBroker();
        String originalNamespace = mqProperties.getNamespace();
        mqProperties.setBroker(mqClient.impl());
        mqProperties.setNamespace("it");
        try {
            mqClient.init(List.of(listener), mqProperties, serialization, null);

            OrderCreatedEvent sent = newOrderCreatedEvent();
            sent.setTag(null);
            sent.publish();

            await(() -> bean.matches(sent.getOrderId()), awaitTimeout());
            OrderCreatedEvent received = bean.getReceived();
            Assertions.assertThat(received).as("OrderCreatedEvent 应在超时内被 @MQEventListener 监听器收到").isNotNull();
            Assertions.assertThat(received.getOrderId()).as("orderId 应 round-trip 一致").isEqualTo(sent.getOrderId());
            Assertions.assertThat(received.getBuyerId()).as("buyerId 应 round-trip 一致").isEqualTo(sent.getBuyerId());
            Assertions.assertThat(received.getAmount()).as("amount 应 round-trip 一致").isEqualByComparingTo(sent.getAmount());
        } finally {
            mqProperties.setBroker(originalBroker);
            mqProperties.setNamespace(originalNamespace);
        }
    }

    /**
     * disruptor 专用监听器：namespace/tags 语义见 round-trip 方法的 route 模型差异说明。
     */
    public static class NoTagListener {

        private volatile OrderCreatedEvent received;

        @MQEventListener(topic = "ORDER", tags = "*", namespace = "it", group = "ddd4j-it")
        public void onOrderCreated(OrderCreatedEvent event) {
            this.received = event;
        }

        public OrderCreatedEvent getReceived() {
            return received;
        }

        public boolean matches(String orderId) {
            OrderCreatedEvent event = received;
            return event != null && orderId.equals(event.getOrderId());
        }
    }

    /**
     * disruptor testcontainers resource for Quarkus：委托共享 fixture {@link DisruptorQuarkusTestResource}。
     */
    public static class DisruptorTestResource implements QuarkusTestResourceLifecycleManager {

        private final DisruptorQuarkusTestResource fixture = new DisruptorQuarkusTestResource();

        @Override
        public Map<String, String> start() {
            Map<String, String> props = new HashMap<>(fixture.start());
            props.put("ddd4j.mq.enabled", "true");
            return props;
        }

        @Override
        public void stop() {
            fixture.stop();
        }
    }
}
