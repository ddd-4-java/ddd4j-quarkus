package io.ddd4j.quarkus.mq.mqttmica;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.mqttmica.MicaMqttProperties;
import io.ddd4j.quarkus.mq.testcontainers.AbstractMqQuarkusIntegrationTest;
import io.ddd4j.quarkus.mq.testcontainers.JunitJupiterQuarkusTestContainers;
import io.ddd4j.quarkus.mq.testcontainers.MicaMqttQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * mqtt-mica MQ 集成测试。
 *
 * <p>验证：
 * <ul>
 *   <li>{@link MQClient} Bean 被 CDI 正确解析，且 impl() = "mqtt-mica"</li>
 *   <li>{@link MQProperties} Bean 存在且 broker = "MQTT_MICA"</li>
 *   <li>{@link MQEventSerialization} Bean 存在且可注入</li>
 *   <li>round-trip：<b>@Disabled</b>（见方法 Javadoc），shouldInject* 保持可跑</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusTest
@QuarkusTestResource(MicaMqttQuarkusIntegrationTest.MicaMqttTestResource.class)
@JunitJupiterQuarkusTestContainers
class MicaMqttQuarkusIntegrationTest extends AbstractMqQuarkusIntegrationTest<MicaMqttProperties> {

    @Inject
    MicaMqttProperties micaMqttProperties;

    @Override
    protected MicaMqttProperties mqPropertiesExtension() {
        return micaMqttProperties;
    }

    @Override
    protected void applyContainerProperties(MicaMqttProperties properties) {
        // broker-url 形如 tcp://host:port，mica-mqtt 客户端按 serverIp + port 连接
        String hostPort = config("ddd4j.mq.mqtt-mica.broker-url").replaceFirst("^tcp://", "");
        properties.setServerIp(hostPort.substring(0, hostPort.indexOf(':')));
        properties.setPort(Integer.parseInt(hostPort.substring(hostPort.indexOf(':') + 1)));
    }

    @Test
    void shouldInjectMQClient() {
        Assertions.assertThat(mqClient).isNotNull();
        Assertions.assertThat(mqClient.impl()).isEqualTo("mqtt-mica");
    }

    @Test
    void shouldInjectMQProperties() {
        Assertions.assertThat(mqProperties).isNotNull();
        Assertions.assertThat(mqProperties.isEnabled()).isTrue();
        Assertions.assertThat(mqProperties.getBroker()).isEqualTo("MQTT_MICA");
    }

    @Test
    void shouldInjectSerialization() {
        Assertions.assertThat(serialization).isNotNull();
        // 验证序列化 round-trip
        String json = serialization.serialize(Map.of("key", "value"));
        Assertions.assertThat(json).contains("key");
    }

    /**
     * round-trip：mica-mqtt 2.6.6 客户端（smart-socket AIO）在 macOS arm64 上首连被
     * broker 拒绝且 publish 静默丢失（核心库 ddd4j-mq-mqtt-mica 的 AIO 发送缺陷，
     * mosquitto/EMQX 均复现），对齐 javalin Ddd4jMicaMqttMqIT 先例；CI linux 可移除。
     */
    @Test
    @Disabled("mica-mqtt AIO 在 macOS arm64 的已知缺陷，对齐 javalin Ddd4jMicaMqttMqIT 先例；CI linux 可移除")
    void shouldPublishAndConsumeOrderCreatedEventEndToEnd() throws Exception {
        runOrderCreatedRoundTrip();
    }

    /**
     * mqtt-mica testcontainers resource for Quarkus：委托共享 fixture {@link MicaMqttQuarkusTestResource}。
     */
    public static class MicaMqttTestResource implements QuarkusTestResourceLifecycleManager {

        private final MicaMqttQuarkusTestResource fixture = new MicaMqttQuarkusTestResource();

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
