package io.ddd4j.quarkus.mq.core;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.listener.MQListener;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link QuarkusMQListenerRegistrar} 集成测试。
 *
 * <p>覆盖三个核心行为：
 * <ul>
 *   <li><b>mq disabled</b>：{@code ddd4j.mq.enabled=false} 时 {@code onStart} 直接跳过
 *       （不扫描、不初始化任何 MQClient）—— {@link QuarkusMQListenerRegistrarTest}</li>
 *   <li><b>scanListeners</b>：有 {@code @ApplicationScoped + @MQEventListener(topic="TEST", tags="A")}
 *       监听器 Bean 时能扫描到—— {@link QuarkusMQListenerRegistrarEnabledTest}</li>
 *   <li><b>findActiveClient</b>：{@code ddd4j.mq.broker=disruptor} 且存在 {@code impl()=="disruptor"}
 *       的 MQClient Bean 时，选中该 client 并委托 {@link MQClient#init}—— {@link QuarkusMQListenerRegistrarEnabledTest}</li>
 * </ul>
 *
 * <p>说明：两个测试类使用不同的应用配置（默认 mq disabled + {@code @TestProfile} 覆盖为 enabled），
 * Quarkus 会为不同 profile 分别启动应用实例。
 */
@QuarkusTest
class QuarkusMQListenerRegistrarTest {

    @Inject
    QuarkusMQListenerRegistrar registrar;

    @Inject
    TestMQClient testClient;

    @Test
    void onStart_withMqDisabled_skipsListenerRegistration() {
        testClient.reset();

        registrar.onStart(new StartupEvent());

        // enabled=false 时应在扫描前直接返回，绝不触碰任何 MQClient
        assertThat(testClient.initCount()).as("mq disabled 时不应初始化 MQClient").isZero();
    }

    /**
     * 覆盖 mq 启用配置的 profile：仅翻转 {@code ddd4j.mq.enabled}，broker/namespace 来自 application.properties。
     */
    public static class MqEnabledProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("ddd4j.mq.enabled", "true");
        }
    }
}

/**
 * mq 启用场景：onStart 应扫描到 {@link TestMqListener}，并通过 findActiveClient 选中
 * {@link TestMQClient}（impl=disruptor）完成初始化。
 */
@QuarkusTest
@TestProfile(QuarkusMQListenerRegistrarTest.MqEnabledProfile.class)
class QuarkusMQListenerRegistrarEnabledTest {

    @Inject
    QuarkusMQListenerRegistrar registrar;

    @Inject
    TestMQClient testClient;

    @Test
    void onStart_scansListenerBeansAndInitializesActiveClient() {
        testClient.reset();

        registrar.onStart(new StartupEvent());

        assertThat(testClient.initCount()).as("应恰好委托一次 client.init").isEqualTo(1);
        TestMQClient.InitCall call = testClient.initCalls().get(0);

        // scanListeners 扫描结果：@MQEventListener(topic="TEST", tags="A")
        assertThat(call.listeners()).hasSize(1);
        MQListener listener = call.listeners().get(0);
        assertThat(listener.getTopic()).isEqualTo("TEST");
        assertThat(listener.getTags()).isEqualTo("A");
        assertThat(listener.getBean()).isInstanceOf(TestMqListener.class);
        assertThat(listener.getMethod().getName()).isEqualTo("onTestEvent");

        // findActiveClient 结果：选中 impl()==broker 的 client（disruptor）
        assertThat(call.properties().getBroker()).isEqualTo("disruptor");
        assertThat(call.properties().getNamespace()).isEqualTo("test-namespace");
        assertThat(call.properties().isEnabled()).isTrue();

        // 序列化器由 Ddd4jMQCdiProducer 提供
        assertThat(call.serialization()).isNotNull();
        assertThat(call.serialization()).isInstanceOf(MQEventSerialization.class);
    }

    @Test
    void findActiveClient_selectsClientMatchingConfiguredBroker() {
        // findActiveClient 是私有方法，通过 onStart 的可观察结果断言：
        // broker=disruptor 时必然选中 TestMQClient（其 impl() 恰为 "disruptor"）
        testClient.reset();
        registrar.onStart(new StartupEvent());
        assertThat(testClient.initCount()).isEqualTo(1);
        List<MQListener> listeners = testClient.initCalls().get(0).listeners();
        assertThat(listeners).isNotEmpty();
    }
}
