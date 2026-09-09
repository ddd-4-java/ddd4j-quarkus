package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

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
    private String previousUserDir;

    /**
     * 把 Paho 默认文件持久化目录约束到 Maven target 下，再启动 MQTT 容器。
     *
     * @return 注入 Quarkus 测试配置的连接属性
     */
    @Override
    public synchronized Map<String, String> start() {
        if (Objects.nonNull(previousUserDir)) {
            return super.start();
        }
        // 先重试上一次失败的容器回收，再安装目录覆盖，避免清理时恢复掉新目录。
        super.stop();
        previousUserDir = System.getProperty("user.dir");
        Path persistenceDirectory = Path.of(previousUserDir, "target", "mqtt-persistence");
        try {
            Files.createDirectories(persistenceDirectory);
            System.setProperty("user.dir", persistenceDirectory.toString());
            return super.start();
        } catch (IOException exception) {
            restoreUserDir();
            throw new UncheckedIOException("Cannot create MQTT test persistence directory", exception);
        } catch (RuntimeException | Error exception) {
            restoreUserDir();
            throw exception;
        }
    }

    /**
     * 关闭容器并恢复测试进程的工作目录属性。
     */
    @Override
    public synchronized void stop() {
        try {
            super.stop();
        } finally {
            restoreUserDir();
        }
    }

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
        return Wait.forLogMessage(".*mosquitto version .* running.*", 1)
                .withStartupTimeout(Duration.ofMinutes(1));
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

    private void restoreUserDir() {
        if (Objects.nonNull(previousUserDir)) {
            System.setProperty("user.dir", previousUserDir);
            previousUserDir = null;
        }
    }
}
