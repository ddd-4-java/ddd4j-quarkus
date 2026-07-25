package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

/**
 * ActiveMQ Classic testcontainers fixture for Quarkus tests.
 *
 * <p>镜像：{@code apache/activemq-classic:5.18.3}。默认 OpenWire 端口 61616，
 * Web 控制台 8161。
 * 暴露属性：{@code ddd4j.mq.activemq.broker-url}。
 */
public class ActiveMqQuarkusTestResource extends AbstractTestContainerFixture {

    private static final DockerImageName IMAGE = DockerImageName.parse("apache/activemq-classic:5.18.3");

    private GenericContainer<?> container;

    @Override
    protected GenericContainer<?> container() {
        container = new GenericContainer<>(IMAGE)
                .withExposedPorts(61616, 8161)
                .withEnv("ACTIVEMQ_ADMIN_USER", "admin")
                .withEnv("ACTIVEMQ_ADMIN_PASSWORD", "admin");
        return container;
    }

    @Override
    protected org.testcontainers.containers.wait.strategy.WaitStrategy waitStrategy() {
        return Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2));
    }

    @Override
    protected DockerImageName dockerImageName() {
        return IMAGE;
    }

    @Override
    protected Map<String, String> exposedProperties() {
        return Map.of(
                "ddd4j.mq.activemq.host", container.getHost(),
                "ddd4j.mq.activemq.port", firstMappedPort(container, 61616),
                "ddd4j.mq.activemq.username", "admin",
                "ddd4j.mq.activemq.password", "admin",
                "ddd4j.mq.broker", "ACTIVEMQ"
        );
    }
}