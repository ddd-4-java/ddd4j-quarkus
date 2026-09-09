package io.ddd4j.quarkus.mq.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** 验证专用容器 API 返回值真正进入 Quarkus 配置，禁止用通用 host/port 重建端点。 */
class OfficialContainerAdapterTest {

    @Test
    void shouldExposeArtemisBrokerApiRatherThanReconstructingItsUrl() throws Exception {
        ArtemisContainer adapter = new ArtemisContainer("apache/activemq-artemis:2.33.0-alpine") {
            @Override
            public String getBrokerUrl() {
                return "tcp://artemis-advertised:31001";
            }

            @Override
            public String getHost() {
                return "generic-host";
            }

            @Override
            public Integer getMappedPort(int originalPort) {
                return 21001;
            }
        };

        assertThat(properties(new ActiveMqQuarkusTestResource(), adapter))
                .containsEntry("ddd4j.mq.activemq.broker-url", "tcp://artemis-advertised:31001")
                .containsEntry("ddd4j.mq.activemq.host", "generic-host")
                .containsEntry("ddd4j.mq.activemq.port", "21001");
    }

    @Test
    void shouldExposeKafkaBootstrapApiRatherThanReconstructingItsUrl() throws Exception {
        ConfluentKafkaContainer adapter = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.7.2") {
            @Override
            public String getBootstrapServers() {
                return "PLAINTEXT://kafka-advertised:31002";
            }

            @Override
            public String getHost() {
                return "generic-host";
            }

            @Override
            public Integer getMappedPort(int originalPort) {
                return 21002;
            }
        };

        assertThat(properties(new KafkaQuarkusTestResource(), adapter))
                .containsEntry("ddd4j.mq.kafka.bootstrap-servers", "PLAINTEXT://kafka-advertised:31002");
    }

    @Test
    void shouldExposePulsarBrokerApiRatherThanReconstructingItsUrl() throws Exception {
        PulsarContainer adapter = new PulsarContainer("apachepulsar/pulsar:3.2.0") {
            @Override
            public String getPulsarBrokerUrl() {
                return "pulsar://pulsar-advertised:31003";
            }

            @Override
            public String getHost() {
                return "generic-host";
            }

            @Override
            public Integer getMappedPort(int originalPort) {
                return 21003;
            }
        };

        assertThat(properties(new PulsarQuarkusTestResource(), adapter))
                .containsEntry("ddd4j.mq.pulsar.service-url", "pulsar://pulsar-advertised:31003");
        assertThat(properties(new TdmqQuarkusTestResource(), adapter))
                .containsEntry("ddd4j.mq.tdmq.service-url", "pulsar://pulsar-advertised:31003")
                .containsEntry("ddd4j.mq.broker", "TDMQ");
    }

    @Test
    void shouldExposeRabbitMqAmqpAndAdminApis() throws Exception {
        RabbitMQContainer adapter = new RabbitMQContainer("rabbitmq:3.13-management-alpine") {
            @Override
            public String getHost() {
                return "rabbit-host";
            }

            @Override
            public Integer getAmqpPort() {
                return 31004;
            }

            @Override
            public Integer getMappedPort(int originalPort) {
                return 21004;
            }

            @Override
            public String getAdminUsername() {
                return "adapter-admin";
            }

            @Override
            public String getAdminPassword() {
                return "adapter-password";
            }
        };

        assertThat(properties(new RabbitMqQuarkusTestResource(), adapter))
                .containsEntry("ddd4j.mq.rabbitmq.host", "rabbit-host")
                .containsEntry("ddd4j.mq.rabbitmq.port", "31004")
                .containsEntry("ddd4j.mq.rabbitmq.username", "adapter-admin")
                .containsEntry("ddd4j.mq.rabbitmq.password", "adapter-password");
    }

    @Test
    void shouldExposeLocalStackEndpointApiRatherThanReconstructingItsUrl() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "LocalStack 2.0.5 constructor requires Docker socket discovery");
        LocalStackContainer adapter = new LocalStackContainer("localstack/localstack:3.8.0") {
            @Override
            public URI getEndpoint() {
                return URI.create("http://localstack-advertised:31005/sqs");
            }

            @Override
            public String getHost() {
                return "generic-host";
            }

            @Override
            public Integer getMappedPort(int originalPort) {
                return 21005;
            }
        };

        assertThat(properties(new SqsQuarkusTestResource(), adapter))
                .containsEntry("ddd4j.mq.sqs.endpoint", "http://localstack-advertised:31005/sqs");
    }

    /** 只替换 Docker API 边界，实际执行生产 exposedProperties；不向生产类添加测试接口。 */
    private static Map<String, String> properties(AbstractTestContainerFixture fixture,
                                                   GenericContainer<?> adapter) throws Exception {
        Field container = fixture.getClass().getDeclaredField("container");
        container.setAccessible(true);
        container.set(fixture, adapter);
        return fixture.exposedProperties();
    }
}
