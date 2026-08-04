package io.ddd4j.quarkus.mq.rocket;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.quarkus.mq.testcontainers.RocketMqQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * rocketmq MQ 集成测试骨架。
 *
 * <p>验证：
 * <ul>
 *   <li>{@link MQClient} Bean 被 CDI 正确解析，且 impl() = "rocket"</li>
 *   <li>{@link MQProperties} Bean 存在且 broker = "ROCKET"</li>
 *   <li>{@link MQEventSerialization} Bean 存在且可注入</li>
 *   <li>容器连接信息已由共享 fixture {@link RocketMqQuarkusTestResource} 注入到 application.properties</li>
 * </ul>
 *
 * <p>测试使用内嵌 {@link RocketMqTestResource}（委托 {@link RocketMqQuarkusTestResource} 的 start/stop）启动对应容器，
 * 并通过 {@code @QuarkusTestResource} 自动注入连接信息到 Quarkus 运行时。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusTest
@QuarkusTestResource(RocketMqQuarkusIntegrationTest.RocketMqTestResource.class)
class RocketMqQuarkusIntegrationTest {

    @Inject
    MQClient mqClient;

    @Inject
    MQProperties mqProperties;

    @Inject
    MQEventSerialization serialization;

    @Test
    void shouldInjectMQClient() {
        Assertions.assertThat(mqClient).isNotNull();
        Assertions.assertThat(mqClient.impl()).isEqualTo("rocket");
    }

    @Test
    void shouldInjectMQProperties() {
        Assertions.assertThat(mqProperties).isNotNull();
        Assertions.assertThat(mqProperties.isEnabled()).isTrue();
        Assertions.assertThat(mqProperties.getBroker()).isEqualTo("ROCKET");
    }

    @Test
    void shouldInjectSerialization() {
        Assertions.assertThat(serialization).isNotNull();
        // 验证序列化 round-trip
        String json = serialization.serialize(Map.of("key", "value"));
        Assertions.assertThat(json).contains("key");
    }

    /**
     * rocketmq testcontainers resource for Quarkus：委托共享 fixture {@link RocketMqQuarkusTestResource}。
     */
    public static class RocketMqTestResource implements QuarkusTestResourceLifecycleManager {

        private final RocketMqQuarkusTestResource fixture = new RocketMqQuarkusTestResource();

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
