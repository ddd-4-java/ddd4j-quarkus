package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

/**
 * Kafka testcontainers fixture for Quarkus tests.
 *
 * <p>镜像：{@code confluentinc/cp-kafka:7.6.1}（KRaft 模式，无需 Zookeeper）。
 * 暴露属性：{@code ddd4j.mq.kafka.bootstrap-servers}。
 */
public class KafkaQuarkusTestResource extends AbstractTestContainerFixture {

    private static final DockerImageName IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.6.1");
    private static final int KAFKA_PORT = 9092;

    private KafkaContainer container;

    @Override
    protected GenericContainer<?> container() {
        container = new KafkaContainer(IMAGE)
                .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
                .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1");
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
                "ddd4j.mq.kafka.bootstrap-servers", container.getBootstrapServers(),
                "ddd4j.mq.broker", "KAFKA"
        );
    }
}