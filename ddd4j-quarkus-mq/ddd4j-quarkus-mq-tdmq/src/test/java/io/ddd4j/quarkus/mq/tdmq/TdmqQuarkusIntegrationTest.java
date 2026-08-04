package io.ddd4j.quarkus.mq.tdmq;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.quarkus.mq.testcontainers.TdmqQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * tdmq MQ 集成测试骨架。
 *
 * <p>验证：
 * <ul>
 *   <li>{@link MQClient} Bean 被 CDI 正确解析，且 impl() = "tdmq"</li>
 *   <li>{@link MQProperties} Bean 存在且 broker = "TDMQ"</li>
 *   <li>{@link MQEventSerialization} Bean 存在且可注入</li>
 *   <li>容器连接信息已由共享 fixture {@link TdmqQuarkusTestResource} 注入到 application.properties</li>
 * </ul>
 *
 * <p>测试使用内嵌 {@link TdmqTestResource}（委托 {@link TdmqQuarkusTestResource} 的 start/stop）启动对应容器，
 * 并通过 {@code @QuarkusTestResource} 自动注入连接信息到 Quarkus 运行时。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusTest
@QuarkusTestResource(TdmqQuarkusIntegrationTest.TdmqTestResource.class)
class TdmqQuarkusIntegrationTest {

    @Inject
    MQClient mqClient;

    @Inject
    MQProperties mqProperties;

    @Inject
    MQEventSerialization serialization;

    @Test
    void shouldInjectMQClient() {
        Assertions.assertThat(mqClient).isNotNull();
        Assertions.assertThat(mqClient.impl()).isEqualTo("tdmq");
    }

    @Test
    void shouldInjectMQProperties() {
        Assertions.assertThat(mqProperties).isNotNull();
        Assertions.assertThat(mqProperties.isEnabled()).isTrue();
        Assertions.assertThat(mqProperties.getBroker()).isEqualTo("TDMQ");
    }

    @Test
    void shouldInjectSerialization() {
        Assertions.assertThat(serialization).isNotNull();
        // 验证序列化 round-trip
        String json = serialization.serialize(Map.of("key", "value"));
        Assertions.assertThat(json).contains("key");
    }

    /**
     * tdmq testcontainers resource for Quarkus：委托共享 fixture {@link TdmqQuarkusTestResource}。
     */
    public static class TdmqTestResource implements QuarkusTestResourceLifecycleManager {

        private final TdmqQuarkusTestResource fixture = new TdmqQuarkusTestResource();

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
