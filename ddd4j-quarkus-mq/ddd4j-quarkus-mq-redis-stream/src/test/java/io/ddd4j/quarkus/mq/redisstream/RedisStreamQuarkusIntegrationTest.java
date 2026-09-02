package io.ddd4j.quarkus.mq.redisstream;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.redisstream.RedisStreamMQProperties;
import io.ddd4j.quarkus.mq.testcontainers.AbstractMqQuarkusIntegrationTest;
import io.ddd4j.quarkus.mq.testcontainers.JunitJupiterQuarkusTestContainers;
import io.ddd4j.quarkus.mq.testcontainers.RedisStreamQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * redis-stream MQ 集成测试。
 *
 * <p>验证：
 * <ul>
 *   <li>{@link MQClient} Bean 被 CDI 正确解析，且 impl() = "redisStream"</li>
 *   <li>{@link MQProperties} Bean 存在且 broker = "REDIS_STREAM"</li>
 *   <li>{@link MQEventSerialization} Bean 存在且可注入</li>
 *   <li>端到端：{@code OrderCreatedEvent.publish()} → Redis Stream（key ORDER:CREATED）→
 *       消费组 {@code @MQEventListener} 监听器收到事件（继承
 *       {@link AbstractMqQuarkusIntegrationTest} round-trip 骨架）</li>
 * </ul>
 *
 * <p>测试使用内嵌 {@link RedisStreamTestResource}（委托 {@link RedisStreamQuarkusTestResource} 的 start/stop）启动对应容器，
 * 并通过 {@code @QuarkusTestResource} 自动注入连接信息到 Quarkus 运行时。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusTest
@QuarkusTestResource(RedisStreamQuarkusIntegrationTest.RedisStreamTestResource.class)
@JunitJupiterQuarkusTestContainers
class RedisStreamQuarkusIntegrationTest extends AbstractMqQuarkusIntegrationTest<RedisStreamMQProperties> {

    @Inject
    RedisStreamMQProperties redisStreamProperties;

    @Override
    protected RedisStreamMQProperties mqPropertiesExtension() {
        return redisStreamProperties;
    }

    @Override
    protected void applyContainerProperties(RedisStreamMQProperties properties) {
        properties.setUrl("redis://" + config("ddd4j.mq.redis-stream.host")
                + ":" + config("ddd4j.mq.redis-stream.port"));
    }

    @Test
    void shouldInjectMQClient() {
        Assertions.assertThat(mqClient).isNotNull();
        Assertions.assertThat(mqClient.impl()).isEqualTo("redisStream");
    }

    @Test
    void shouldInjectMQProperties() {
        Assertions.assertThat(mqProperties).isNotNull();
        Assertions.assertThat(mqProperties.isEnabled()).isTrue();
        Assertions.assertThat(mqProperties.getBroker()).isEqualTo("REDIS_STREAM");
    }

    @Test
    void shouldInjectSerialization() {
        Assertions.assertThat(serialization).isNotNull();
        // 验证序列化 round-trip
        String json = serialization.serialize(Map.of("key", "value"));
        Assertions.assertThat(json).contains("key");
    }

    /**
     * 端到端：OrderCreatedEvent 发布 → Redis Stream（XADD ORDER:CREATED + 消费组 XREADGROUP）→ 监听器消费。
     */
    @Test
    void shouldPublishAndConsumeOrderCreatedEventEndToEnd() throws Exception {
        runOrderCreatedRoundTrip();
    }

    /**
     * redis-stream testcontainers resource for Quarkus：委托共享 fixture {@link RedisStreamQuarkusTestResource}。
     */
    public static class RedisStreamTestResource implements QuarkusTestResourceLifecycleManager {

        private final RedisStreamQuarkusTestResource fixture = new RedisStreamQuarkusTestResource();

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
