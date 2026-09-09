package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * RabbitMQ testcontainers fixture for Quarkus tests.
 *
 * <p>镜像：{@code rabbitmq:3.13-management-alpine}（带 management UI 便于调试）。
 * 暴露属性：{@code ddd4j.mq.rabbitmq.host/port/username/password}。
 */
public class RabbitMqQuarkusTestResource extends AbstractTestContainerFixture {

    private static final DockerImageName IMAGE = DockerImageName.parse("rabbitmq:3.13-management-alpine");

    private RabbitMQContainer container;

    @Override
    protected GenericContainer<?> container() {
        container = new RabbitMQContainer(IMAGE)
                .withEnv("RABBITMQ_DEFAULT_USER", "guest")
                .withEnv("RABBITMQ_DEFAULT_PASS", "guest");
        return container;
    }

    @Override
    protected DockerImageName dockerImageName() {
        return IMAGE;
    }

    @Override
    protected Map<String, String> exposedProperties() {
        return Map.of(
                "ddd4j.mq.rabbitmq.host", container.getHost(),
                "ddd4j.mq.rabbitmq.port", String.valueOf(container.getAmqpPort()),
                "ddd4j.mq.rabbitmq.username", container.getAdminUsername(),
                "ddd4j.mq.rabbitmq.password", container.getAdminPassword(),
                "ddd4j.mq.rabbitmq.virtual-host", "/",
                "ddd4j.mq.broker", "RABBIT"
        );
    }
}
