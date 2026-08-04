package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

/**
 * Pulsar testcontainers fixture for Quarkus tests.
 *
 * <p>镜像：{@code apachepulsar/pulsar:3.2.0}（默认 entrypoint 以 standalone 模式启动，
 * 内置 broker + bookkeeper）。暴露 broker 端口 6650（Binary 协议）与 8080（HTTP admin）。
 * 暴露属性：{@code ddd4j.mq.pulsar.service-url}（{@code pulsar://host:6650}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class PulsarQuarkusTestResource extends AbstractTestContainerFixture {

    private static final DockerImageName IMAGE = DockerImageName.parse("apachepulsar/pulsar:3.2.0");
    private static final int BROKER_PORT = 6650;
    private static final int HTTP_PORT = 8080;

    private GenericContainer<?> container;

    @Override
    protected GenericContainer<?> container() {
        container = new GenericContainer<>(IMAGE)
                .withExposedPorts(BROKER_PORT, HTTP_PORT);
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
                "ddd4j.mq.pulsar.service-url", String.format("pulsar://%s:%s",
                        container.getHost(), firstMappedPort(container, BROKER_PORT)),
                "ddd4j.mq.broker", "PULSAR"
        );
    }
}
