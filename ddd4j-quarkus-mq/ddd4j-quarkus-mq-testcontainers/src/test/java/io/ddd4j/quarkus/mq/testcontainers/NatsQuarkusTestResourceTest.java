package io.ddd4j.quarkus.mq.testcontainers;

import io.nats.client.Connection;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.api.PublishAck;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证 NATS fixture 在暴露连接前准备真实 JetStream，并在重复配置时保留消息。 */
class NatsQuarkusTestResourceTest {

    @Test
    void shouldProvisionOrderStreamBeforeExposureAndRetainMessagesOnRepeatedProvisioning() throws Exception {
        NatsQuarkusTestResource fixture = new NatsQuarkusTestResource();
        try {
            Map<String, String> properties = fixture.start();
            try (Connection connection = Nats.connect(properties.get("ddd4j.mq.nats.servers"))) {
                JetStreamManagement management = connection.jetStreamManagement();
                List<String> streams = management.getStreamNames("ORDER.CREATED");
                assertThat(streams).hasSize(1);
                String stream = streams.get(0);

                PublishAck acknowledgment = connection.jetStream().publish(
                        "ORDER.CREATED", "fixture-order".getBytes(StandardCharsets.UTF_8));
                assertThat(acknowledgment.getStream()).isEqualTo(stream);
                assertThat(management.getStreamInfo(stream).getStreamState().getMsgCount()).isEqualTo(1);

                // 再次执行容器启动后的配置阶段，不能删除已有 stream 或已确认的消息。
                assertThat(fixture.exposedProperties()).isEqualTo(properties);
                assertThat(management.getStreamNames("ORDER.CREATED")).containsExactly(stream);
                assertThat(management.getStreamInfo(stream).getStreamState().getMsgCount()).isEqualTo(1);
            }
        } finally {
            fixture.stop();
        }
    }
}
