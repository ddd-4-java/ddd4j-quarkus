package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

/**
 * ActiveMQ Artemis testcontainers fixture for Quarkus tests.
 *
 * <p>镜像：{@code apache/activemq-artemis:2.33.0-alpine}。<b>协议注意</b>：主仓
 * {@code ddd4j-mq-activemq} 客户端是 {@code artemis-jakarta-client}（CORE 协议，61616），
 * 旧版 fixture 误用 {@code apache/activemq-classic}（OpenWire 协议）导致客户端无法连接，
 * 现已对齐 artemis 镜像。默认凭证 {@code artemis/artemis}（{@code ARTEMIS_USER} /
 * {@code ARTEMIS_PASSWORD}），Web 控制台 8161。
 * 暴露属性：{@code ddd4j.mq.activemq.broker-url}（{@code tcp://host:61616}）及
 * host/port/username/password 分量。
 */
public class ActiveMqQuarkusTestResource extends AbstractTestContainerFixture {

    private static final DockerImageName IMAGE = DockerImageName.parse("apache/activemq-artemis:2.33.0-alpine");
    private static final int CORE_PORT = 61616;
    private static final int CONSOLE_PORT = 8161;
    private static final String USERNAME = "artemis";
    private static final String PASSWORD = "artemis";

    private GenericContainer<?> container;

    @Override
    protected GenericContainer<?> container() {
        container = new GenericContainer<>(IMAGE)
                .withExposedPorts(CORE_PORT, CONSOLE_PORT)
                .withEnv("ARTEMIS_USER", USERNAME)
                .withEnv("ARTEMIS_PASSWORD", PASSWORD);
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
                "ddd4j.mq.activemq.broker-url", String.format("tcp://%s:%s",
                        container.getHost(), firstMappedPort(container, CORE_PORT)),
                "ddd4j.mq.activemq.host", container.getHost(),
                "ddd4j.mq.activemq.port", firstMappedPort(container, CORE_PORT),
                "ddd4j.mq.activemq.username", USERNAME,
                "ddd4j.mq.activemq.password", PASSWORD,
                "ddd4j.mq.broker", "ACTIVEMQ"
        );
    }
}