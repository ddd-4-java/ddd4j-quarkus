package io.ddd4j.quarkus.mq.testcontainers;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.Nats;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * NATS testcontainers fixture for Quarkus tests.
 *
 * <p>镜像：{@code nats:2.10.22}（含 jetstream 支持）。
 * 暴露属性：{@code ddd4j.mq.nats.servers}。
 * 返回连接配置前准备覆盖 {@code ORDER.CREATED} 的 JetStream stream；准备失败则启动失败。
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
        return Wait.forHttp("/healthz?js-enabled-only=true").forPort(8222).forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(1));
    }

    @Override
    protected DockerImageName dockerImageName() {
        return IMAGE;
    }

    @Override
    protected Map<String, String> exposedProperties() {
        String servers = String.format("nats://%s:%s",
                container.getHost(), firstMappedPort(container, 4222));
        try (Connection connection = Nats.connect(servers)) {
            // 同配置的 stream 创建请求可幂等重放，保留已有消息，不降级为 core NATS。
            connection.jetStreamManagement().addStream(StreamConfiguration.builder()
                    .name("DDD4J_TEST_ORDERS")
                    .subjects("ORDER.CREATED")
                    .storageType(StorageType.Memory)
                    .build());
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("NATS JetStream stream 初始化被中断", failure);
        } catch (IOException | JetStreamApiException failure) {
            throw new IllegalStateException("无法准备 NATS JetStream ORDER.CREATED stream", failure);
        }
        return Map.of(
                "ddd4j.mq.nats.servers", servers,
                "ddd4j.mq.broker", "NATS"
        );
    }
}
