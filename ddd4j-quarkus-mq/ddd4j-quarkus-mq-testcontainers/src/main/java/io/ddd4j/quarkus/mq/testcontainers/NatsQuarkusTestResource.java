package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

/**
 * NATS testcontainers fixture for Quarkus tests.
 *
 * <p>镜像：{@code nats:2.10.22}（含 jetstream 支持）。
 * 暴露属性：{@code ddd4j.mq.nats.servers}。
 */
public class NatsQuarkusTestResource extends AbstractTestContainerFixture {

    private static final DockerImageName IMAGE = DockerImageName.parse("nats:2.10.22");

    private GenericContainer<?> container;

    @Override
    protected GenericContainer<?> container() {
        container = new GenericContainer<>(IMAGE)
                .withExposedPorts(4222, 8222)
                // 镜像 ENTRYPOINT 为 /nats-server，只需追加参数：-js 启用 JetStream；
                // -m 8222 启用 HTTP monitoring，否则 8222 不监听，Wait.forListeningPort() 会超时
                .withCommand("-js", "-m", "8222");
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
                "ddd4j.mq.nats.servers", String.format("nats://%s:%s",
                        container.getHost(), firstMappedPort(container, 4222)),
                "ddd4j.mq.broker", "NATS"
        );
    }
}