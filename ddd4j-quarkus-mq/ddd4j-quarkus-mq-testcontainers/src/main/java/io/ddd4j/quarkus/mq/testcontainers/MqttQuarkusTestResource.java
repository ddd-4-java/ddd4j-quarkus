package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

/**
 * MQTT (Eclipse Mosquitto) testcontainers fixture for Quarkus tests.
 *
 * <p>镜像：{@code eclipse-mosquitto:2.0}（默认监听 1883，允许匿名连接）。
 * 暴露属性：{@code ddd4j.mq.mqtt.broker-url}（{@code tcp://host:1883}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class MqttQuarkusTestResource extends AbstractTestContainerFixture {

    private static final DockerImageName IMAGE = DockerImageName.parse("eclipse-mosquitto:2.0");
    private static final int MQTT_PORT = 1883;

    private GenericContainer<?> container;

    @Override
    protected GenericContainer<?> container() {
        container = new GenericContainer<>(IMAGE)
                .withExposedPorts(MQTT_PORT)
                // mosquitto 2.0 无配置时仅监听容器 loopback（2.0 breaking change），
                // 必须显式指定镜像内置的 no-auth 配置监听 0.0.0.0 并允许匿名（对齐 javalin 先例）
                .withCommand("/usr/sbin/mosquitto", "-c", "/mosquitto-no-auth.conf", "-v");
        return container;
    }

    @Override
    protected org.testcontainers.containers.wait.strategy.WaitStrategy waitStrategy() {
        return Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(1));
    }

    @Override
    protected DockerImageName dockerImageName() {
        return IMAGE;
    }

    @Override
    protected Map<String, String> exposedProperties() {
        return Map.of(
                "ddd4j.mq.mqtt.broker-url", String.format("tcp://%s:%s",
                        container.getHost(), firstMappedPort(container, MQTT_PORT)),
                "ddd4j.mq.broker", "MQTT"
        );
    }
}
