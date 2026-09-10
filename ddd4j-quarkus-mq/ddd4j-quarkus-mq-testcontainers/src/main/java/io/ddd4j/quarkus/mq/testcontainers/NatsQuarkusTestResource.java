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
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * NATS testcontainers fixture for Quarkus tests.
 *
 * <p>镜像：{@code nats:2.10.22}（含 jetstream 支持）。
 * 暴露属性：{@code ddd4j.mq.nats.servers}。
 * 默认只启用 JetStream，不创建业务 stream；测试可通过显式构造参数声明自己拥有的拓扑。
 */
public class NatsQuarkusTestResource extends AbstractTestContainerFixture {

    private static final DockerImageName IMAGE = DockerImageName.parse("nats:2.10.22");

    private GenericContainer<?> container;
    private final String streamName;
    private final List<String> subjects;
    private String servers;

    /** 启用 JetStream，但不创建任何业务 stream。 */
    public NatsQuarkusTestResource() {
        this.streamName = null;
        this.subjects = List.of();
    }

    /**
     * 启用 JetStream 并创建一个由当前 fixture 拥有的 stream。
     *
     * @param streamName stream 名称
     * @param subjects stream 覆盖的 subjects
     */
    public NatsQuarkusTestResource(String streamName, List<String> subjects) {
        this.streamName = Objects.requireNonNull(streamName, "streamName");
        this.subjects = List.copyOf(Objects.requireNonNull(subjects, "subjects"));
        if (this.subjects.isEmpty()) {
            throw new IllegalArgumentException("subjects 不能为空");
        }
    }

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
        servers = String.format("nats://%s:%s",
                container.getHost(), firstMappedPort(container, 4222));
        if (Objects.nonNull(streamName)) {
            provisionStream();
        }
        return Map.of(
                "ddd4j.mq.nats.servers", servers,
                "ddd4j.mq.broker", "NATS"
        );
    }

    private void provisionStream() {
        try (Connection connection = Nats.connect(servers)) {
            // 同配置的 stream 创建请求可幂等重放，保留已有消息，不降级为 core NATS。
            connection.jetStreamManagement().addStream(StreamConfiguration.builder()
                    .name(streamName)
                    .subjects(subjects.toArray(String[]::new))
                    .storageType(StorageType.Memory)
                    .build());
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("NATS JetStream stream 初始化被中断", failure);
        } catch (IOException | JetStreamApiException failure) {
            throw new IllegalStateException("无法准备 NATS JetStream stream: " + streamName, failure);
        }
    }

    @Override
    public synchronized void stop() {
        RuntimeException cleanupFailure = null;
        if (Objects.nonNull(streamName) && Objects.nonNull(servers)) {
            try (Connection connection = Nats.connect(servers)) {
                connection.jetStreamManagement().deleteStream(streamName);
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                cleanupFailure = new IllegalStateException("NATS JetStream stream 清理被中断: " + streamName, failure);
            } catch (IOException | JetStreamApiException failure) {
                cleanupFailure = new IllegalStateException("无法清理 NATS JetStream stream: " + streamName, failure);
            }
        }
        servers = null;
        try {
            super.stop();
        } catch (RuntimeException stopFailure) {
            if (Objects.nonNull(cleanupFailure)) {
                stopFailure.addSuppressed(cleanupFailure);
            }
            throw stopFailure;
        }
        if (Objects.nonNull(cleanupFailure)) {
            throw cleanupFailure;
        }
    }
}
