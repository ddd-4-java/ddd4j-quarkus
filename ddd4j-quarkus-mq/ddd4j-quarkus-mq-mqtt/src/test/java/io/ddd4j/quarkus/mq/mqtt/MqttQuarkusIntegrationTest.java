package io.ddd4j.quarkus.mq.mqtt;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.mqtt.MqttMQProperties;
import io.ddd4j.quarkus.mq.testcontainers.AbstractMqQuarkusIntegrationTest;
import io.ddd4j.quarkus.mq.testcontainers.JunitJupiterQuarkusTestContainers;
import io.ddd4j.quarkus.mq.testcontainers.MqttQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * mqtt MQ 集成测试。
 *
 * <p>验证：
 * <ul>
 *   <li>{@link MQClient} Bean 被 CDI 正确解析，且 impl() = "mqtt"</li>
 *   <li>{@link MQProperties} Bean 存在且 broker = "MQTT"</li>
 *   <li>{@link MQEventSerialization} Bean 存在且可注入</li>
 *   <li>端到端：{@code OrderCreatedEvent.publish()} → Mosquitto 容器 →
 *       {@code @MQEventListener} 监听器收到事件（继承 {@link AbstractMqQuarkusIntegrationTest}
 *       round-trip 骨架）</li>
 * </ul>
 *
 * <p>测试使用内嵌 {@link MqttTestResource}（委托 {@link MqttQuarkusTestResource} 的 start/stop）启动对应容器，
 * 并通过 {@code @QuarkusTestResource} 自动注入连接信息到 Quarkus 运行时。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusTest
@QuarkusTestResource(MqttQuarkusIntegrationTest.MqttTestResource.class)
@JunitJupiterQuarkusTestContainers
class MqttQuarkusIntegrationTest extends AbstractMqQuarkusIntegrationTest<MqttMQProperties> {

    private static final Pattern PAHO_DIRECTORY = Pattern.compile("^ddd4j-mq-.*-tcplocalhost\\d+$");

    @Inject
    MqttMQProperties mqttProperties;

    @Override
    protected MqttMQProperties mqPropertiesExtension() {
        return mqttProperties;
    }

    @Override
    protected void applyContainerProperties(MqttMQProperties properties) {
        properties.setServerUri(config("ddd4j.mq.mqtt.broker-url"));
    }

    @Test
    void shouldInjectMQClient() {
        Assertions.assertThat(mqClient).isNotNull();
        Assertions.assertThat(mqClient.impl()).isEqualTo("mqtt");
    }

    @Test
    void shouldInjectMQProperties() {
        Assertions.assertThat(mqProperties).isNotNull();
        Assertions.assertThat(mqProperties.isEnabled()).isTrue();
        Assertions.assertThat(mqProperties.getBroker()).isEqualTo("MQTT");
    }

    @Test
    void shouldInjectSerialization() {
        Assertions.assertThat(serialization).isNotNull();
        // 验证序列化 round-trip
        String json = serialization.serialize(Map.of("key", "value"));
        Assertions.assertThat(json).contains("key");
    }

    /**
     * 端到端：OrderCreatedEvent 发布 → Mosquitto（MQTT topic ORDER/CREATED）→ 监听器消费。
     */
    @Test
    void shouldPublishAndConsumeOrderCreatedEventEndToEnd() throws Exception {
        Set<String> persistenceDirectoriesBefore = pahoPersistenceDirectories();
        runOrderCreatedRoundTrip();
        Assertions.assertThat(pahoPersistenceDirectories())
                .as("Paho 持久化目录必须位于 target，而不是 Maven 模块根目录")
                .isEqualTo(persistenceDirectoriesBefore);
    }

    private Set<String> pahoPersistenceDirectories() throws IOException {
        try (Stream<Path> paths = Files.list(Path.of("").toAbsolutePath())) {
            return paths.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> PAHO_DIRECTORY.matcher(name).matches())
                    .collect(Collectors.toSet());
        }
    }

    /**
     * mqtt testcontainers resource for Quarkus：委托共享 fixture {@link MqttQuarkusTestResource}。
     */
    public static class MqttTestResource implements QuarkusTestResourceLifecycleManager {

        private final MqttQuarkusTestResource fixture = new MqttQuarkusTestResource();

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
