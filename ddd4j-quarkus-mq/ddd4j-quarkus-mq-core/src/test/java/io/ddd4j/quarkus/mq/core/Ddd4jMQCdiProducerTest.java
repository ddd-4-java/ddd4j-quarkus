package io.ddd4j.quarkus.mq.core;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Ddd4jMQCdiProducer} 集成测试：验证 CDI 生产者从 MicroProfile Config
 * （{@code src/test/resources/application.properties}）读取 {@code ddd4j.mq.enabled / broker / namespace}。
 *
 * <p>同时验证默认 JSON 序列化 Bean 可注入、以及 mq-core 模块无真实 broker 时
 * {@link MQClient} 仍可通过 {@link Instance} 解析（测试夹具 Bean）。
 */
@QuarkusTest
class Ddd4jMQCdiProducerTest {

    @Inject
    MQProperties mqProperties;

    @Inject
    MQEventSerialization serialization;

    @Inject
    Instance<MQClient> mqClients;

    @Test
    void readsMqPropertiesFromApplicationProperties() {
        assertThat(mqProperties.isEnabled()).as("ddd4j.mq.enabled 应从 application.properties 读取").isFalse();
        assertThat(mqProperties.getBroker()).as("ddd4j.mq.broker 应从 application.properties 读取").isEqualTo("disruptor");
        assertThat(mqProperties.getNamespace()).as("ddd4j.mq.namespace 应从 application.properties 读取").isEqualTo("test-namespace");
    }

    @Test
    void producesDefaultJsonSerialization() {
        assertThat(serialization).isNotNull();
        // JsonMQEventSerialization 与 MQEventSerialization 接口绑定均指向同一默认实现
        // （serialize 为 <T> T 泛型方法，显式指定 String，避免被 String.valueOf(char[]) 重载推断为 char[]）
        assertThat(serialization.<String>serialize("hello")).contains("hello");
    }

    @Test
    void mqClientResolvableViaInstanceWithoutRealBroker() {
        // 无真实 broker 依赖时，MQClient 不应导致解析失败（测试夹具 Bean 提供实现）
        assertThat(mqClients.isResolvable()).isTrue();
        assertThat(mqClients.get().impl()).isEqualTo("disruptor");
    }
}
