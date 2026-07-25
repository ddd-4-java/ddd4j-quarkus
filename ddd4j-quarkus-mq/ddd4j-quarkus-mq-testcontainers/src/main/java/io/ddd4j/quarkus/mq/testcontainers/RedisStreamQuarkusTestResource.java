package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

/**
 * Redis (Stream 模式) testcontainers fixture for Quarkus tests.
 *
 * <p>镜像：{@code redis:7.4-alpine}。通过 {@code CONFIG SET} 启用
 * {@code notify-keyspace-events}，保证 Redis Stream XADD 事件能被消费者捕获。
 * 暴露属性：{@code ddd4j.mq.redis-stream.host/port}。
 */
public class RedisStreamQuarkusTestResource extends AbstractTestContainerFixture {

    private static final DockerImageName IMAGE = DockerImageName.parse("redis:7.4-alpine");

    private GenericContainer<?> container;

    @Override
    protected GenericContainer<?> container() {
        container = new GenericContainer<>(IMAGE)
                .withExposedPorts(6379)
                .withCommand("redis-server", "--appendonly", "yes", "--notify-keyspace-events", "KEA");
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
                "ddd4j.mq.redis-stream.host", container.getHost(),
                "ddd4j.mq.redis-stream.port", firstMappedPort(container, 6379),
                "ddd4j.mq.redis-stream.database", "0",
                "ddd4j.mq.broker", "REDIS_STREAM"
        );
    }
}