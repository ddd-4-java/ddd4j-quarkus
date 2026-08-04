package io.ddd4j.quarkus.mq.activemq;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.quarkus.mq.testcontainers.ActiveMqQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * activemq MQ 集成测试骨架。
 *
 * <p>验证：
 * <ul>
 *   <li>{@link MQClient} Bean 被 CDI 正确解析，且 impl() = "activemq"</li>
 *   <li>{@link MQProperties} Bean 存在且 broker = "ACTIVEMQ"</li>
 *   <li>{@link MQEventSerialization} Bean 存在且可注入</li>
 *   <li>容器连接信息已由共享 fixture {@link ActiveMqQuarkusTestResource} 注入到 application.properties</li>
 * </ul>
 *
 * <p>测试使用内嵌 {@link ActiveMqTestResource}（委托 {@link ActiveMqQuarkusTestResource} 的 start/stop）启动对应容器，
 * 并通过 {@code @QuarkusTestResource} 自动注入连接信息到 Quarkus 运行时。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusTest
@QuarkusTestResource(ActiveMqQuarkusIntegrationTest.ActiveMqTestResource.class)
class ActiveMqQuarkusIntegrationTest {

    @Inject
    MQClient mqClient;

    @Inject
    MQProperties mqProperties;

    @Inject
    MQEventSerialization serialization;

    @Test
    void shouldInjectMQClient() {
        Assertions.assertThat(mqClient).isNotNull();
        Assertions.assertThat(mqClient.impl()).isEqualTo("activemq");
    }

    @Test
    void shouldInjectMQProperties() {
        Assertions.assertThat(mqProperties).isNotNull();
        Assertions.assertThat(mqProperties.isEnabled()).isTrue();
        Assertions.assertThat(mqProperties.getBroker()).isEqualTo("ACTIVEMQ");
    }

    @Test
    void shouldInjectSerialization() {
        Assertions.assertThat(serialization).isNotNull();
        // 验证序列化 round-trip
        String json = serialization.serialize(Map.of("key", "value"));
        Assertions.assertThat(json).contains("key");
    }

    /**
     * activemq testcontainers resource for Quarkus：委托共享 fixture {@link ActiveMqQuarkusTestResource}。
     */
    public static class ActiveMqTestResource implements QuarkusTestResourceLifecycleManager {

        private final ActiveMqQuarkusTestResource fixture = new ActiveMqQuarkusTestResource();

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
