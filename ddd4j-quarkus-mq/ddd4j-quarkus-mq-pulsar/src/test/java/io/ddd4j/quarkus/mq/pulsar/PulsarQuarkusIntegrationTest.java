package io.ddd4j.quarkus.mq.pulsar;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.pulsar.PulsarProperties;
import io.ddd4j.quarkus.mq.testcontainers.AbstractMqQuarkusIntegrationTest;
import io.ddd4j.quarkus.mq.testcontainers.JunitJupiterQuarkusTestContainers;
import io.ddd4j.quarkus.mq.testcontainers.PulsarQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * pulsar MQ 集成测试。
 *
 * <p>验证：
 * <ul>
 *   <li>{@link MQClient} Bean 被 CDI 正确解析，且 impl() = "pulsar"</li>
 *   <li>{@link MQProperties} Bean 存在且 broker = "PULSAR"</li>
 *   <li>{@link MQEventSerialization} Bean 存在且可注入</li>
 *   <li>端到端：{@code OrderCreatedEvent.publish()} → Pulsar standalone 容器 →
 *       {@code @MQEventListener} 监听器收到事件（继承 {@link AbstractMqQuarkusIntegrationTest}
 *       round-trip 骨架）</li>
 * </ul>
 *
 * <p>测试使用内嵌 {@link PulsarTestResource}（委托 {@link PulsarQuarkusTestResource} 的 start/stop）启动对应容器，
 * 并通过 {@code @QuarkusTestResource} 自动注入连接信息到 Quarkus 运行时。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusTest
@QuarkusTestResource(PulsarQuarkusIntegrationTest.PulsarTestResource.class)
@JunitJupiterQuarkusTestContainers
class PulsarQuarkusIntegrationTest extends AbstractMqQuarkusIntegrationTest<PulsarProperties> {

    @Inject
    PulsarProperties pulsarProperties;

    @Override
    protected PulsarProperties mqPropertiesExtension() {
        return pulsarProperties;
    }

    @Override
    protected void applyContainerProperties(PulsarProperties properties) {
        properties.setServiceUrl(config("ddd4j.mq.pulsar.service-url"));
        // 物理 topic = tenant/namespace/topic：standalone 默认 public/default
        properties.setNamespace("default");
        properties.setSubscriptionName("ddd4j-it-sub");
    }

    @Override
    protected String listenerTags() {
        // pulsar producer 物理地址含 ":tag" 而 consumer 订阅地址不含（主仓既有语义），
        // 事件必须不带 tag、监听器放行全部（对齐 javalin Ddd4jPulsarMqIT 先例）
        return "*";
    }

    @Override
    protected String eventTag() {
        return null;
    }

    @Test
    void shouldInjectMQClient() {
        Assertions.assertThat(mqClient).isNotNull();
        Assertions.assertThat(mqClient.impl()).isEqualTo("pulsar");
    }

    @Test
    void shouldInjectMQProperties() {
        Assertions.assertThat(mqProperties).isNotNull();
        Assertions.assertThat(mqProperties.isEnabled()).isTrue();
        Assertions.assertThat(mqProperties.getBroker()).isEqualTo("PULSAR");
    }

    @Test
    void shouldInjectSerialization() {
        Assertions.assertThat(serialization).isNotNull();
        // 验证序列化 round-trip
        String json = serialization.serialize(Map.of("key", "value"));
        Assertions.assertThat(json).contains("key");
    }

    /**
     * 端到端：OrderCreatedEvent 发布 → Pulsar（persistent://public/default/ORDER，tag 应用层过滤）→ 监听器消费。
     */
    @Test
    void shouldPublishAndConsumeOrderCreatedEventEndToEnd() throws Exception {
        runOrderCreatedRoundTrip();
    }

    /**
     * pulsar testcontainers resource for Quarkus：委托共享 fixture {@link PulsarQuarkusTestResource}。
     */
    public static class PulsarTestResource implements QuarkusTestResourceLifecycleManager {

        private final PulsarQuarkusTestResource fixture = new PulsarQuarkusTestResource();

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
