package io.ddd4j.quarkus.mq.rabbit;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.rabbitmq.RabbitMQProperties;
import io.ddd4j.quarkus.mq.testcontainers.AbstractMqQuarkusIntegrationTest;
import io.ddd4j.quarkus.mq.testcontainers.JunitJupiterQuarkusTestContainers;
import io.ddd4j.quarkus.mq.testcontainers.RabbitMqQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * rabbitmq MQ 集成测试。
 *
 * <p>验证：
 * <ul>
 *   <li>{@link MQClient} Bean 被 CDI 正确解析，且 impl() = "rabbit"</li>
 *   <li>{@link MQProperties} Bean 存在且 broker = "RABBIT"</li>
 *   <li>{@link MQEventSerialization} Bean 存在且可注入</li>
 *   <li>端到端：{@code OrderCreatedEvent.publish()} → RabbitMQ 容器 →
 *       {@code @MQEventListener} 监听器收到事件（继承 {@link AbstractMqQuarkusIntegrationTest}
 *       round-trip 骨架）</li>
 * </ul>
 *
 * <p>测试使用内嵌 {@link RabbitMqTestResource}（委托 {@link RabbitMqQuarkusTestResource} 的 start/stop）启动对应容器，
 * 并通过 {@code @QuarkusTestResource} 自动注入连接信息到 Quarkus 运行时。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusTest
@QuarkusTestResource(RabbitMqQuarkusIntegrationTest.RabbitMqTestResource.class)
@JunitJupiterQuarkusTestContainers
class RabbitMqQuarkusIntegrationTest extends AbstractMqQuarkusIntegrationTest<RabbitMQProperties> {

    @Inject
    RabbitMQProperties rabbitProperties;

    @Override
    protected RabbitMQProperties mqPropertiesExtension() {
        return rabbitProperties;
    }

    @Override
    protected void applyContainerProperties(RabbitMQProperties properties) {
        properties.setHost(config("ddd4j.mq.rabbitmq.host"));
        properties.setPort(Integer.parseInt(config("ddd4j.mq.rabbitmq.port")));
        properties.setUsername(config("ddd4j.mq.rabbitmq.username"));
        properties.setPassword(config("ddd4j.mq.rabbitmq.password"));
        properties.setVirtualHost(config("ddd4j.mq.rabbitmq.virtual-host"));
    }

    @Override
    protected void preInit() {
        // 复用 RabbitMQ 内置 topic exchange（amq.topic）：producer basicPublish 与
        // consumer queue bind 使用同一 exchange，免声明（对齐 javalin Ddd4jRabbitMqIT 先例）。
        mqProperties.setExchange("amq.topic");
    }

    @Test
    void shouldInjectMQClient() {
        Assertions.assertThat(mqClient).isNotNull();
        Assertions.assertThat(mqClient.impl()).isEqualTo("rabbit");
    }

    @Test
    void shouldInjectMQProperties() {
        Assertions.assertThat(mqProperties).isNotNull();
        Assertions.assertThat(mqProperties.isEnabled()).isTrue();
        Assertions.assertThat(mqProperties.getBroker()).isEqualTo("RABBIT");
    }

    @Test
    void shouldInjectSerialization() {
        Assertions.assertThat(serialization).isNotNull();
        // 验证序列化 round-trip
        String json = serialization.serialize(Map.of("key", "value"));
        Assertions.assertThat(json).contains("key");
    }

    /**
     * 端到端：OrderCreatedEvent 发布 → RabbitMQ（amq.topic exchange 路由 ORDER.CREATED）→ 监听器消费。
     */
    @Test
    void shouldPublishAndConsumeOrderCreatedEventEndToEnd() throws Exception {
        runOrderCreatedRoundTrip();
    }

    /**
     * rabbitmq testcontainers resource for Quarkus：委托共享 fixture {@link RabbitMqQuarkusTestResource}。
     */
    public static class RabbitMqTestResource implements QuarkusTestResourceLifecycleManager {

        private final RabbitMqQuarkusTestResource fixture = new RabbitMqQuarkusTestResource();

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
