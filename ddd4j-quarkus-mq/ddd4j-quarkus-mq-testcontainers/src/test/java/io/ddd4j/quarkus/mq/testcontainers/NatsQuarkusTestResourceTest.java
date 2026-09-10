package io.ddd4j.quarkus.mq.testcontainers;

import io.nats.client.Connection;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.api.PublishAck;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证 NATS fixture 只在显式配置时创建业务 JetStream 拓扑。 */
@Testcontainers(disabledWithoutDocker = true)
class NatsQuarkusTestResourceTest {

    @Test
    void shouldEnableJetStreamWithoutProvisioningBusinessTopologyByDefault() throws Exception {
        NatsQuarkusTestResource fixture = new NatsQuarkusTestResource();
        try {
            Map<String, String> properties = fixture.start();
            try (Connection connection = Nats.connect(properties.get("ddd4j.mq.nats.servers"))) {
                assertThat(connection.jetStreamManagement().getStreamNames()).isEmpty();
            }
        } finally {
            fixture.stop();
        }
    }

    @Test
    void shouldProvisionExplicitStreamAndRetainMessagesOnRepeatedProvisioning() throws Exception {
        String streamName = "DDD4J_TEST_ORDERS_" + System.nanoTime();
        String subject = "ORDER.CREATED." + System.nanoTime();
        NatsQuarkusTestResource fixture = new NatsQuarkusTestResource(streamName, List.of(subject));
        try {
            Map<String, String> properties = fixture.start();
            try (Connection connection = Nats.connect(properties.get("ddd4j.mq.nats.servers"))) {
                JetStreamManagement management = connection.jetStreamManagement();
                assertThat(management.getStreamNames(subject)).containsExactly(streamName);

                PublishAck acknowledgment = connection.jetStream().publish(
                        subject, "fixture-order".getBytes(StandardCharsets.UTF_8));
                assertThat(acknowledgment.getStream()).isEqualTo(streamName);
                assertThat(management.getStreamInfo(streamName).getStreamState().getMsgCount()).isEqualTo(1);

                // 再次执行容器启动后的配置阶段，不能删除已有 stream 或已确认的消息。
                assertThat(fixture.exposedProperties()).isEqualTo(properties);
                assertThat(management.getStreamNames(subject)).containsExactly(streamName);
                assertThat(management.getStreamInfo(streamName).getStreamState().getMsgCount()).isEqualTo(1);
            }
        } finally {
            fixture.stop();
        }
    }
}
