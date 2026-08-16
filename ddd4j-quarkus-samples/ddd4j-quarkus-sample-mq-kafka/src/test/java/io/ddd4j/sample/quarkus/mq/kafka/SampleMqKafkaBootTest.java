package io.ddd4j.sample.quarkus.mq.kafka;

import io.ddd4j.sample.quarkus.mq.kafka.web.OrderResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * sample-mq-kafka 应用上下文启动测试。
 *
 * <p>轻量验证（不依赖 Kafka broker）：{@code ddd4j.mq.enabled} 默认 false 时
 * 应用正常启动，REST 资源与监听器 Bean 可注入。broker 端到端流转由框架层
 * ddd4j-quarkus-mq-kafka 的 testcontainers 集成测试覆盖。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@QuarkusTest
class SampleMqKafkaBootTest {

    @Inject
    OrderResource orderResource;

    @Test
    void applicationContextBootsAndResourceInjectable() {
        assertThat(orderResource).isNotNull();
    }
}
