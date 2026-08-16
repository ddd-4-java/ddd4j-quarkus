package io.ddd4j.sample.quarkus.mq.rabbitmq;

import io.ddd4j.sample.quarkus.mq.rabbitmq.order.web.OrderResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * sample-mq-rabbitmq 应用上下文启动测试。
 *
 * <p>本 sample 主配置开启了 {@code ddd4j.mq.enabled=true}，测试通过 profile
 * 覆盖为 false（轻量验证不依赖 RabbitMQ broker；端到端流转由框架层
 * ddd4j-quarkus-mq-rabbitmq 的 testcontainers 集成测试覆盖）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@QuarkusTest
@TestProfile(SampleMqRabbitBootTest.MqDisabledProfile.class)
class SampleMqRabbitBootTest {

    @Inject
    OrderResource orderResource;

    @Test
    void applicationContextBootsAndResourceInjectable() {
        assertThat(orderResource).isNotNull();
    }

    /**
     * 禁用 MQ 的测试 profile。
     */
    public static class MqDisabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("ddd4j.mq.enabled", "false");
        }
    }
}
