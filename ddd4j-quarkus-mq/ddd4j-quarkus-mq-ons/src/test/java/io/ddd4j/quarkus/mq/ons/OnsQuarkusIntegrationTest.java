package io.ddd4j.quarkus.mq.ons;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.ons.OnsProperties;
import io.ddd4j.quarkus.mq.testcontainers.AbstractMqQuarkusIntegrationTest;
import io.ddd4j.quarkus.mq.testcontainers.JunitJupiterQuarkusTestContainers;
import io.ddd4j.quarkus.mq.testcontainers.OnsQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * ons MQ 集成测试。
 *
 * <p>验证：
 * <ul>
 *   <li>{@link MQClient} Bean 被 CDI 正确解析，且 impl() = "ons"</li>
 *   <li>{@link MQProperties} Bean 存在且 broker = "ONS"</li>
 *   <li>{@link MQEventSerialization} Bean 存在且可注入</li>
 *   <li>round-trip：<b>@Disabled</b>——ONS 为阿里云商业协议（aliyun-sdk-ons 需 AccessKey
 *       与 ONS 控制台预建资源），无 Testcontainers 镜像可本地起端点，对齐 javalin
 *       Ddd4jOnsMqIT 先例；fixture 仍以本地 RocketMQ namesrv 充当协议端点占位</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusTest
@QuarkusTestResource(OnsQuarkusIntegrationTest.OnsTestResource.class)
@JunitJupiterQuarkusTestContainers
class OnsQuarkusIntegrationTest extends AbstractMqQuarkusIntegrationTest<OnsProperties> {

    @Inject
    OnsProperties onsProperties;

    @Override
    protected OnsProperties mqPropertiesExtension() {
        return onsProperties;
    }

    @Override
    protected void applyContainerProperties(OnsProperties properties) {
        properties.setNameSrvAddr(config("ddd4j.mq.ons.endpoint"));
    }

    @Test
    void shouldInjectMQClient() {
        Assertions.assertThat(mqClient).isNotNull();
        Assertions.assertThat(mqClient.impl()).isEqualTo("ons");
    }

    @Test
    void shouldInjectMQProperties() {
        Assertions.assertThat(mqProperties).isNotNull();
        Assertions.assertThat(mqProperties.isEnabled()).isTrue();
        Assertions.assertThat(mqProperties.getBroker()).isEqualTo("ONS");
    }

    @Test
    void shouldInjectSerialization() {
        Assertions.assertThat(serialization).isNotNull();
        // 验证序列化 round-trip
        String json = serialization.serialize(Map.of("key", "value"));
        Assertions.assertThat(json).contains("key");
    }

    /**
     * round-trip：ONS 商业协议无 Testcontainers 镜像（需阿里云 AccessKey 与控制台预建
     * Producer/Consumer ID），本地无法起真实端点，对齐 javalin Ddd4jOnsMqIT 先例；
     * shouldInject* 保持可跑。
     */
    @Test
    @Disabled("ONS 商业协议无 Testcontainers 镜像，对齐 javalin Ddd4jOnsMqIT 先例")
    void shouldPublishAndConsumeOrderCreatedEventEndToEnd() throws Exception {
        runOrderCreatedRoundTrip();
    }

    /**
     * ons testcontainers resource for Quarkus：委托共享 fixture {@link OnsQuarkusTestResource}。
     */
    public static class OnsTestResource implements QuarkusTestResourceLifecycleManager {

        private final OnsQuarkusTestResource fixture = new OnsQuarkusTestResource();

        @Override
        public Map<String, String> start() {
            Map<String, String> props = new HashMap<>(fixture.start());
            props.put("ddd4j.mq.enabled", "true");
            return props;
        }

        @Override
        public void stop() {
            fixture.stop();
        }
    }
}
