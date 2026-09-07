package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * Kafka testcontainers fixture for Quarkus tests.
 *
 * <p>镜像：{@code confluentinc/cp-kafka:7.7.2}（KRaft 模式，无需 Zookeeper）。
 * 暴露属性：{@code ddd4j.mq.kafka.bootstrap-servers}。
 */
public class KafkaQuarkusTestResource extends AbstractTestContainerFixture {

    private static final DockerImageName IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.7.2");
    private ConfluentKafkaContainer container;

    @Override
    protected GenericContainer<?> container() {
        container = new ConfluentKafkaContainer(IMAGE)
                .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
                .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1");
        return container;
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
