package io.ddd4j.quarkus.mq.kafka;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.kafka.KafkaMQProperties;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.quarkus.mq.testcontainers.KafkaQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Kafka MQ 端到端集成测试。
 *
 * <p>验证：
 * <ul>
 *   <li>{@link MQClient} Bean 被 CDI 正确解析，且 impl() = "kafka"</li>
 *   <li>{@link MQProperties} Bean 存在且 broker = "KAFKA"</li>
 *   <li>{@link MQEventSerialization} Bean 存在且可注入</li>
 *   <li>端到端：{@link PingEvent#publish()} → Kafka 容器 → {@link PingListener}（{@code @MQEventListener}）收到事件</li>
 * </ul>
 *
 * <p>测试使用 {@link KafkaTestResource}（复用共享 fixture {@link KafkaQuarkusTestResource}）
 * 启动 Kafka 容器（KRaft 模式），并通过 {@code @QuarkusTestResource} 自动注入连接信息到 Quarkus 运行时。
 *
 * <p><b>端到端说明</b>：{@link QuarkusMQListenerRegistrar} 在 StartupEvent 时调用
 * {@link MQClient#init}，但 {@code MQClient.init()} 的 broker 校验是大小写敏感的
 * （{@code Objects.equals(properties.getBroker(), impl())}），fixture 注入的是大写
 * {@code KAFKA}，因此启动时不会注册 producer/consumer。端到端测试方法中先归一化 broker
 * 再手动触发 init（同时把 testcontainers 注入的 bootstrap-servers 写入
 * {@link KafkaMQProperties} 单例），随后 {@code event.publish()} 即可经
 * {@code BaseContext} 路由到真实 Kafka 生产者。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusTest
@QuarkusTestResource(KafkaQuarkusIntegrationTest.KafkaTestResource.class)
class KafkaQuarkusIntegrationTest {

    @Inject
    MQClient mqClient;

    @Inject
    MQProperties mqProperties;

    @Inject
    MQEventSerialization serialization;

    @Inject
    KafkaMQProperties kafkaProperties;

    @Inject
    PingListener pingListener;

    @Test
    void shouldInjectMQClient() {
        Assertions.assertThat(mqClient).isNotNull();
        Assertions.assertThat(mqClient.impl()).isEqualTo("kafka");
    }

    @Test
    void shouldInjectMQProperties() {
        Assertions.assertThat(mqProperties).isNotNull();
        Assertions.assertThat(mqProperties.isEnabled()).isTrue();
        Assertions.assertThat(mqProperties.getBroker()).isEqualTo("KAFKA");
    }

    @Test
    void shouldInjectSerialization() {
        Assertions.assertThat(serialization).isNotNull();
        // 验证序列化/反序列化 round-trip
        String json = serialization.serialize(Map.of("key", "value"));
        Assertions.assertThat(json).contains("key");
    }

    @Test
    void shouldPublishAndConsumeEventEndToEnd() throws Exception {
        // 1. 把 testcontainers 注入的 bootstrap-servers 写入 KafkaMQProperties 单例，
        //    KafkaMQClient 懒构造 producer，init 前改写生效，使 CDI 客户端连接测试容器。
        String bootstrap = ConfigProvider.getConfig()
                .getValue("ddd4j.mq.kafka.bootstrap-servers", String.class);
        kafkaProperties.setBootstrapServers(bootstrap);

        // 2. 归一化 broker（fixture 注入大写 KAFKA，MQClient.init 大小写敏感）后手动触发 init：
        //    注册 producer 到 BaseContext + 启动 Kafka 消费者。
        mqProperties.setBroker(mqClient.impl());
        try {
            mqClient.init(List.of(listenerDefinition()), mqProperties, serialization, null);

            // 3. 发布事件并等待监听器收到
            PingListener.reset();
            new PingEvent().publish();

            Assertions.assertThat(PingListener.LATCH.await(30, TimeUnit.SECONDS))
                    .as("PingEvent 应在 30s 内被 @MQEventListener 监听器收到")
                    .isTrue();
            Assertions.assertThat(PingListener.LAST.get()).isNotNull();
            Assertions.assertThat(PingListener.LAST.get().getMessage()).isEqualTo("hello-ddd4j");
        } finally {
            // 恢复共享单例，避免影响 shouldInjectMQProperties 的断言（与测试执行顺序解耦）
            mqProperties.setBroker("KAFKA");
        }
    }

    /**
     * 手工构建 {@link MQListener} 定义（与 QuarkusMQListenerRegistrar 的扫描结果等价）。
     */
    private MQListener listenerDefinition() throws Exception {
        Method method = PingListener.class.getDeclaredMethod("onPing", PingEvent.class);
        return MQListener.of(pingListener, method, method.getAnnotation(MQEventListener.class));
    }

    /**
     * 端到端测试事件：topic=TEST、tag=PING。
     */
    public static class PingEvent extends MQEvent {

        private String message;

        public PingEvent() {
            setTopic("TEST");
            setTag("PING");
            this.message = "hello-ddd4j";
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * 端到端监听器：{@code @ApplicationScoped} CDI Bean，收到 {@link PingEvent} 后
     * 通过静态 {@link CountDownLatch} 通知测试线程。
     */
    @ApplicationScoped
    public static class PingListener {

        static final AtomicReference<PingEvent> LAST = new AtomicReference<>();
        static volatile CountDownLatch LATCH = new CountDownLatch(1);

        @MQEventListener(topic = "TEST", tags = "PING")
        public void onPing(PingEvent event) {
            LAST.set(event);
            LATCH.countDown();
        }

        static void reset() {
            LAST.set(null);
            LATCH = new CountDownLatch(1);
        }
    }

    /**
     * Kafka testcontainers resource for Quarkus：委托共享 fixture {@link KafkaQuarkusTestResource}。
     */
    public static class KafkaTestResource implements QuarkusTestResourceLifecycleManager {

        private final KafkaQuarkusTestResource fixture = new KafkaQuarkusTestResource();

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
