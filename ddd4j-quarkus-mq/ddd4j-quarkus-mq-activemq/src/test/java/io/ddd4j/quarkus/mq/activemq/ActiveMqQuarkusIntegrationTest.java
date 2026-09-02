package io.ddd4j.quarkus.mq.activemq;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.activemq.ActiveMQClient;
import io.ddd4j.mq.activemq.ActiveMQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.quarkus.mq.testcontainers.AbstractMqQuarkusIntegrationTest;
import io.ddd4j.quarkus.mq.testcontainers.JunitJupiterQuarkusTestContainers;
import io.ddd4j.quarkus.mq.testcontainers.ActiveMqQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * activemq MQ 集成测试。
 *
 * <p>验证：
 * <ul>
 *   <li>{@link MQClient} Bean 被 CDI 正确解析，且 impl() = "activemq"</li>
 *   <li>{@link MQProperties} Bean 存在且 broker = "ACTIVEMQ"</li>
 *   <li>{@link MQEventSerialization} Bean 存在且可注入</li>
 *   <li>端到端：{@code OrderCreatedEvent.publish()} → ActiveMQ Artemis 容器 →
 *       {@code @MQEventListener} 监听器收到事件（继承 {@link AbstractMqQuarkusIntegrationTest}
 *       round-trip 骨架）</li>
 * </ul>
 *
 * <p>fixture 镜像为 {@code apache/activemq-artemis:2.33.0-alpine}——主仓
 * {@code ddd4j-mq-activemq} 客户端是 {@code artemis-jakarta-client}（CORE 协议 61616），
 * 旧版 activemq-classic（OpenWire）协议不兼容。测试使用内嵌 {@link ActiveMqTestResource}
 * （委托 {@link ActiveMqQuarkusTestResource} 的 start/stop）启动容器，
 * 并通过 {@code @QuarkusTestResource} 自动注入连接信息到 Quarkus 运行时。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusTest
@QuarkusTestResource(ActiveMqQuarkusIntegrationTest.ActiveMqTestResource.class)
@JunitJupiterQuarkusTestContainers
class ActiveMqQuarkusIntegrationTest extends AbstractMqQuarkusIntegrationTest<ActiveMQProperties> {

    @Inject
    ActiveMQProperties activeMQProperties;

    @Override
    protected ActiveMQProperties mqPropertiesExtension() {
        return activeMQProperties;
    }

    @Override
    protected void applyContainerProperties(ActiveMQProperties properties) {
        properties.setBrokerUrl(config("ddd4j.mq.activemq.broker-url"));
        properties.setUsername(config("ddd4j.mq.activemq.username"));
        properties.setPassword(config("ddd4j.mq.activemq.password"));
    }

    @Override
    protected void preInit() throws Exception {
        // ActiveMQClient 在构造期即 buildFactory 并缓存（CDI Bean 于 app 启动创建，
        // 早于测试方法改写 properties），这里把容器地址/凭证回写到已缓存的 factory
        // （主仓构造语义不改，仅测试内校正）。
        Field field = ActiveMQClient.class.getDeclaredField("connectionFactory");
        field.setAccessible(true);
        ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) field.get(mqClient);
        factory.setBrokerURL(activeMQProperties.getBrokerUrl());
        factory.setUser(activeMQProperties.getUsername());
        factory.setPassword(activeMQProperties.getPassword());
    }

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
     * 端到端：OrderCreatedEvent 发布 → Artemis（JMS Topic ORDER.CREATED，tag 走 JMS selector）→ 监听器消费。
     */
    @Test
    void shouldPublishAndConsumeOrderCreatedEventEndToEnd() throws Exception {
        runOrderCreatedRoundTrip();
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
