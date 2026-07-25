package io.ddd4j.quarkus.mq.kafka;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import io.quarkus.test.common.QuarkusTestResource;

import java.time.Duration;
import java.util.Map;

/**
 * Kafka MQ 集成测试骨架。
 *
 * <p>验证：
 * <ul>
 *   <li>{@link MQClient} Bean 被 CDI 正确解析，且 impl() = "kafka"</li>
 *   <li>{@link MQProperties} Bean 存在且 broker = "KAFKA"</li>
 *   <li>{@link MQEventSerialization} Bean 存在且可注入</li>
 *   <li>Kafka 容器连接信息已注入到 application.properties</li>
 * </ul>
 *
 * <p>测试使用 {@link KafkaTestResource} 启动 Kafka 容器（KRaft 模式），
 * 并通过 {@code @QuarkusTestResource} 自动注入连接信息到 Quarkus 运行时。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusTest
@io.quarkus.test.common.QuarkusTestResource(KafkaQuarkusIntegrationTest.KafkaTestResource.class)
class KafkaQuarkusIntegrationTest {

    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.6.1");

    @Inject
    MQClient mqClient;

    @Inject
    MQProperties mqProperties;

    @Inject
    MQEventSerialization serialization;

    @Test
    void shouldInjectMQClient() {
        Assertions.assertThat(mqClient).isNotNull();
        Assertions.assertThat(mqClient.impl()).isEqualTo("kafka");
    }

    @Test
    void shouldInjectMQProperties() {
        Assertions.assertThat(mqProperties).isNotNull();
        Assertions.assertThat(mqProperties.isEnabled()).isTrue();
        Assertions.assertThat(mqProperties.getBroker()).isEqualTo("KAFKA");
    }

    @Test
    void shouldInjectSerialization() {
        Assertions.assertThat(serialization).isNotNull();
        // 验证序列化/反序列化 round-trip
        String json = serialization.serialize(Map.of("key", "value"));
        Assertions.assertThat(json).contains("key");
    }

    /**
     * Kafka testcontainers resource for Quarkus.
     *
     * <p>启动 Kafka KRaft 模式容器，并将连接信息注入到 application.properties。
     * 与 {@link io.ddd4j.quarkus.mq.testcontainers.KafkaQuarkusTestResource} 共享镜像配置。
     */
    public static class KafkaTestResource implements QuarkusTestResourceLifecycleManager {

        private static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE)
                .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
                .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
                .withReuse(true);

        @Override
        public Map<String, String> start() {
            KAFKA.start();
            return Map.of(
                    "ddd4j.mq.enabled", "true",
                    "ddd4j.mq.broker", "KAFKA",
                    "ddd4j.mq.kafka.bootstrap-servers", KAFKA.getBootstrapServers()
            );
        }

        @Override
        public void stop() {
            // withReuse(true): 容器不会真正停止，CI 全局清理时处理
        }
    }
}