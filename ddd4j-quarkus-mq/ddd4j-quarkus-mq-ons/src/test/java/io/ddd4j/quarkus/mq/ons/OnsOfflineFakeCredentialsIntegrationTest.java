package io.ddd4j.quarkus.mq.ons;

import io.ddd4j.core.context.BaseContext;
import io.ddd4j.mq.lifecycle.MQInitializationException;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.ons.OnsMQClient;
import io.ddd4j.mq.ons.OnsProperties;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ONS 客户端级契约测试（离线假凭据法，纯 JUnit 手动装配，无容器）。
 *
 * <p><b>为什么不起容器</b>：ONS 是阿里云商业协议消息服务（需 AccessKey 与控制台预建
 * Producer/Consumer ID），Testcontainers 官方模块索引无对应镜像——既有
 * {@link OnsQuarkusIntegrationTest} 的端到端用例也因此 {@code @Disabled}。
 * 对齐 ddd4j-boot {@code OnsMQBootAutoConfigurationTest} 的最新假凭据写法：
 * 「格式合法的假凭据 + 回环地址 NameServer」让生产者预热在离线环境完整走完
 * （NameServer 非空让 SDK 走本地解析分支、跳过联网探测；心跳/路由刷新均为异步任务，
 * 失败只记日志），任何值都不会被用于真实建连。
 *
 * <p><b>装配绑定坑位</b>：boot 侧需要 {@code ConfigurationPropertiesAutoConfiguration}
 * 提供绑定后处理，否则假凭据不会绑定进 {@link OnsProperties}（accessKey 为 null，
 * {@code sessionProperties} 对空值直接 NPE）。本测试为纯 Java 手动装配，直接 setter
 * 注入，等价覆盖「假凭据必须非空」这一绑定契约（见
 * {@link #shouldFailFastWhenFakeCredentialsMissing()}）。
 *
 * <p>按任务约定统一标注 {@code @Testcontainers(disabledWithoutDocker = true)}
 * （本类无 {@code @Container}，同 {@code NatsQuarkusTestResourceTest} 先例）。
 */
// 测试镜像来源: 无（阿里云商业协议消息服务，无本地可启动镜像；https://testcontainers.com/modules/ 索引无对应模块，
// 故按任务约定对齐 boot 家族最新假凭据离线法，不起容器）
@Testcontainers(disabledWithoutDocker = true)
class OnsOfflineFakeCredentialsIntegrationTest {

    /** 离线假凭据：仅满足格式校验（非空），endpoint 指向回环地址，不会用于真实建连。 */
    private static final String FAKE_ACCESS_KEY = "FAKE_ACCESS_KEY_FOR_CONTRACT_TEST";
    private static final String FAKE_SECRET_KEY = "FAKE_SECRET_KEY_FOR_CONTRACT_TEST";
    private static final String FAKE_NAME_SRV_ADDR = "127.0.0.1:9876";
    private static final String FAKE_PRODUCER_ID = "GID_FAKE_CONTRACT_TEST";

    /**
     * 每个测试前后清理 BaseContext 静态注册（MQ_EVENT_PUBLISHER / MQ_PROPERTIES），
     * 避免进程级静态状态跨测试类残留导致断言顺序耦合。
     */
    @BeforeEach
    @AfterEach
    void cleanBaseContext() {
        BaseContext.remove(MQEvent.MQ_EVENT_PUBLISHER);
        BaseContext.remove(MQEvent.MQ_PROPERTIES);
    }

    /** 用假凭据构造 ONS 配置（启用 + broker 与 impl() 大小写一致）。 */
    private static OnsProperties fakeProperties() {
        OnsProperties properties = new OnsProperties();
        properties.setEnabled(true);
        properties.setBroker("ons");
        properties.setAccessKey(FAKE_ACCESS_KEY);
        properties.setSecretKey(FAKE_SECRET_KEY);
        properties.setNameSrvAddr(FAKE_NAME_SRV_ADDR);
        properties.setProducerId(FAKE_PRODUCER_ID);
        return properties;
    }

    @Test
    void shouldCompleteProducerWarmUpOfflineWithFakeCredentials() {
        OnsProperties properties = fakeProperties();
        OnsMQClient client = new OnsMQClient(properties);
        try {
            // 无监听器的纯生产者预热（对齐 boot：急切初始化仅注册 producer，消费者需控制台预建资源）
            client.init(List.of(), properties, new JsonMQEventSerialization(), null);

            // 装配契约：impl 与假凭据绑定（对齐 boot defaultAssemblyShouldCreateClientAndProperties 断言）
            assertThat(client.impl()).isEqualTo("ons");
            assertThat(properties.getAccessKey()).isEqualTo(FAKE_ACCESS_KEY);
            assertThat(properties.getNameSrvAddr()).isEqualTo(FAKE_NAME_SRV_ADDR);
            assertThat(properties.getProducerId()).isEqualTo(FAKE_PRODUCER_ID);

            // 装配契约：init 后生产者必须已注册到 BaseContext（key = impl()），否则 publish 只会 warn 后静默丢弃
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            assertThat(publishers).as("init 应把 ONS 生产者注册到 BaseContext").isNotNull();
            assertThat(publishers).containsKey("ons");
        } finally {
            client.close();
        }
    }

    @Test
    void shouldSkipInitializationWhenBrokerPropertyMismatch() {
        // MQClient#init 守卫契约：broker 与 impl() 不一致（大小写敏感相等）时直接 return，
        // 不注册 publisher（对齐 boot shouldBackOffWhenBrokerPropertyMismatch 的回退语义）
        OnsProperties properties = fakeProperties();
        properties.setBroker("other");
        OnsMQClient client = new OnsMQClient(properties);
        try {
            client.init(List.of(), properties, new JsonMQEventSerialization(), null);
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            assertThat(publishers).as("broker 不匹配时不应注册任何 publisher").isNull();
        } finally {
            client.close();
        }
    }

    @Test
    void shouldFailFastWhenFakeCredentialsMissing() {
        // 绑定契约（boot 坑位的客户端级等价覆盖）：accessKey/secretKey 必须非空，
        // 否则 OnsProperties#sessionProperties 调 Properties.setProperty 直接 NPE，
        // initProducer 捕获后包装为 IllegalStateException，init 再按必选初始化项 fail-fast。
        OnsProperties properties = fakeProperties();
        properties.setAccessKey(null);
        properties.setSecretKey(null);
        OnsMQClient client = new OnsMQClient(properties);
        try {
            assertThatThrownBy(() -> client.init(List.of(), properties,
                    new JsonMQEventSerialization(), null))
                    .isInstanceOf(MQInitializationException.class)
                    .hasMessageContaining("ons");
            // 失败回滚：publisher 不应残留
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            assertThat(publishers).isNull();
        } finally {
            client.close();
        }
    }
}
