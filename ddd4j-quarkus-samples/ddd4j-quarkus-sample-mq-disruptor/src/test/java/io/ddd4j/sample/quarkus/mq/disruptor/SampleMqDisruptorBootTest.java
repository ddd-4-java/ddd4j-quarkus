package io.ddd4j.sample.quarkus.mq.disruptor;

import io.ddd4j.sample.quarkus.mq.disruptor.web.OrderResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * sample-mq-disruptor 应用上下文启动测试。
 *
 * <p>Disruptor 为本地内存队列（无外部 broker），{@code ddd4j.mq.enabled}
 * 默认 false 时应用正常启动、REST 资源可注入。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@QuarkusTest
class SampleMqDisruptorBootTest {

    @Inject
    OrderResource orderResource;

    @Test
    void applicationContextBootsAndResourceInjectable() {
        assertThat(orderResource).isNotNull();
    }
}
