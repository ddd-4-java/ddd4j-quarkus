package io.ddd4j.quarkus.mq.tdmq;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.tdmq.TdmqProperties;
import io.ddd4j.quarkus.mq.testcontainers.AbstractMqQuarkusIntegrationTest;
import io.ddd4j.quarkus.mq.testcontainers.JunitJupiterQuarkusTestContainers;
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
 * tdmq MQ 集成测试。
 *
 * <p>验证：
 * <ul>
 *   <li>{@link MQClient} Bean 被 CDI 正确解析，且 impl() = "tdmq"</li>
 *   <li>{@link MQProperties} Bean 存在且 broker = "TDMQ"</li>
 *   <li>{@link MQEventSerialization} Bean 存在且可注入</li>
 *   <li>round-trip：{@code OrderCreatedEvent.publish()} → {@code TdmqMQClient} →
 *       {@code @MQEventListener} 监听器收到事件（继承 {@link AbstractMqQuarkusIntegrationTest}
 *       骨架）</li>
 * </ul>
 *
 * <p><b>协议说明</b>：主仓 {@code TdmqMQClient} 将 publish/subscribe 委托给
 * {@code BrokerPublisher}/{@code BrokerSubscriber} SPI（业务侧腾讯云 SDK 封装），
 * 未注入 SPI 时回落进程内内存总线（客户端自带，供本地/测试）。因此本 round-trip
 * 验证的是 ddd4j 完整发布/消费管道（BaseContext 路由 → 序列化 → tag 过滤 → 反射监听），
 * 而非 Pulsar standalone 容器——容器仍由 fixture 启动（对齐 TDMQ 无开源镜像的
 * javalin Ddd4jTdmqMqIT 先例）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusTest
@QuarkusTestResource(TdmqQuarkusIntegrationTest.TdmqTestResource.class)
@JunitJupiterQuarkusTestContainers
class TdmqQuarkusIntegrationTest extends AbstractMqQuarkusIntegrationTest<TdmqProperties> {

    @Inject
    TdmqProperties tdmqProperties;

    @Override
    protected TdmqProperties mqPropertiesExtension() {
        return tdmqProperties;
    }

    @Override
    protected void applyContainerProperties(TdmqProperties properties) {
        // TDMQ 客户端走 SPI/内存总线，service-url 仅作配置完整性记录
        properties.setServiceUrl(config("ddd4j.mq.tdmq.service-url"));
    }

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
     * round-trip：OrderCreatedEvent 发布 → TdmqMQClient（内存总线 SPI fallback）→ 监听器消费。
     */
    @Test
    void shouldPublishAndConsumeOrderCreatedEventEndToEnd() throws Exception {
        runOrderCreatedRoundTrip();
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
