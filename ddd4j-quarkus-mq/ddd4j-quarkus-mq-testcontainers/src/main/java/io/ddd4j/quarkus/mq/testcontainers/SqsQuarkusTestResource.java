package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

/**
 * AWS SQS (LocalStack) testcontainers fixture for Quarkus tests.
 *
 * <p>镜像：{@code localstack/localstack:3.8.0}（仅启用 sqs 服务，缩短启动时间）。
 * 暴露属性：{@code ddd4j.mq.sqs.endpoint}（{@code http://host:4566}），
 * 配合 {@code ddd4j.mq.sqs.region=us-east-1} 使用。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class SqsQuarkusTestResource extends AbstractTestContainerFixture {

    private static final DockerImageName IMAGE = DockerImageName.parse("localstack/localstack:3.8.0");
    private static final int EDGE_PORT = 4566;

    private GenericContainer<?> container;

    @Override
    protected GenericContainer<?> container() {
        container = new GenericContainer<>(IMAGE)
                .withExposedPorts(EDGE_PORT)
                .withEnv("SERVICES", "sqs");
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
                "ddd4j.mq.sqs.endpoint", String.format("http://%s:%s",
                        container.getHost(), firstMappedPort(container, EDGE_PORT)),
                "ddd4j.mq.broker", "SQS"
        );
    }
}
