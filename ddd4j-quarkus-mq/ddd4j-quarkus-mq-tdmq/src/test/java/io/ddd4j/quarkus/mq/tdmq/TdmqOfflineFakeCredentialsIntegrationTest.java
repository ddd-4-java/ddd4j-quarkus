package io.ddd4j.quarkus.mq.tdmq;

import io.ddd4j.core.context.BaseContext;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.lifecycle.MQInitializationException;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import io.ddd4j.mq.tdmq.TdmqMQClient;
import io.ddd4j.mq.tdmq.TdmqProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDMQ 客户端级契约测试（离线假凭据法，纯 JUnit 手动装配，无容器）。
 *
 * <p><b>为什么不起容器</b>：TDMQ 是腾讯云托管消息服务，没有可本地启动的开源镜像
 * （Testcontainers 官方模块索引无对应模块，javalin 家族同款 IT 也因此长期 disabled）。
 * 对齐 ddd4j-boot 的假凭据离线方法（同 {@code OnsMQBootAutoConfigurationTest}）：
 * 用「格式合法的假凭据 + 回环地址 endpoint」让 {@link TdmqMQClient} 完整走完
 * {@code MQClient#init} 装配与发布/消费管道，任何值都不会被用于真实建连。
 *
 * <p><b>为什么 round-trip 仍有效</b>：{@code TdmqMQClient} 把 publish/subscribe 委托给
 * {@code BrokerPublisher}/{@code BrokerSubscriber} SPI（业务侧腾讯云 SDK 封装），未注入
 * SPI 时回落进程内内存总线（客户端自带，供本地/测试）。本测试走 fallback 路径，验证
 * ddd4j 完整管道：BaseContext 路由 → JSON 序列化 → namespace/topic/tag 物理地址解析 →
 * tag 过滤 → 反射监听，与既有 {@link TdmqQuarkusIntegrationTest}（{@code @QuarkusTest}）
 * 互补——后者依赖 Quarkus runtime，本测试是客户端级裸装配契约。
 *
 * <p>按任务约定统一标注 {@code @Testcontainers(disabledWithoutDocker = true)}
 * （本类无 {@code @Container}，同 {@code NatsQuarkusTestResourceTest} 先例）。
 */
// 测试镜像来源: 无（腾讯云托管消息服务，无本地可启动镜像；https://testcontainers.com/modules/ 索引无对应模块，
// 故按任务约定走 boot 家族假凭据离线法，不起容器）
@Testcontainers(disabledWithoutDocker = true)
class TdmqOfflineFakeCredentialsIntegrationTest {

    /** 离线假凭据：仅满足格式校验，endpoint 指向回环地址，任何值都不会用于真实建连。 */
    private static final String FAKE_SERVICE_URL = "http://127.0.0.1:18080";
    private static final String FAKE_TENANT = "fake-tenant";
    private static final String FAKE_ACCESS_KEY = "FAKE_ACCESS_KEY_FOR_CONTRACT_TEST";
    private static final String FAKE_SECRET_KEY = "FAKE_SECRET_KEY_FOR_CONTRACT_TEST";

    /** round-trip 断言用 topic/tag。 */
    private static final String TOPIC = "order";
    private static final String TAG = "created";

    /**
     * 每个测试前后都清理 BaseContext 中的静态注册（MQ_EVENT_PUBLISHER / MQ_PROPERTIES），
     * 避免同 JVM 内其他测试类（如 @QuarkusTest 装配）遗留的静态状态导致断言顺序耦合
     * （BaseContext 是进程级静态容器，跨测试类共享）。
     */
    @BeforeEach
    @AfterEach
    void cleanBaseContext() {
        BaseContext.remove(MQEvent.MQ_EVENT_PUBLISHER);
        BaseContext.remove(MQEvent.MQ_PROPERTIES);
    }

    /** 用假凭据构造 TDMQ 配置（启用 + broker 与 impl() 大小写一致）。 */
    private static TdmqProperties fakeProperties() {
        TdmqProperties properties = new TdmqProperties();
        properties.setEnabled(true);
        properties.setBroker("tdmq");
        properties.setServiceUrl(FAKE_SERVICE_URL);
        properties.setTenant(FAKE_TENANT);
        properties.setAccessKey(FAKE_ACCESS_KEY);
        properties.setSecretKey(FAKE_SECRET_KEY);
        properties.setDefaultGroup("ddd4j-it");
        properties.setAutoStartConsumers(true);
        return properties;
    }

    /** 从注解构建 {@link MQListener} 定义（与 {@code QuarkusMQListenerRegistrar} 扫描结果等价）。 */
    private static MQListener listenerDefinition(OrderCreatedListener bean) throws Exception {
        Method method = OrderCreatedListener.class.getDeclaredMethod("onOrderCreated", OrderCreatedEvent.class);
        return MQListener.of(bean, method, method.getAnnotation(MQEventListener.class));
    }

    /** 轮询直到条件满足或超时（不引入 awaitility 依赖；失败断言保证可读）。 */
    private static void await(BooleanSupplier condition, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(100L);
        }
        assertThat(condition.getAsBoolean()).as("condition not met within " + timeout).isTrue();
    }

    @Test
    void shouldPublishAndConsumeRoundTripWithFakeCredentialsOverInMemoryFallback() throws Exception {
        TdmqProperties properties = fakeProperties();
        OrderCreatedListener listenerBean = new OrderCreatedListener();
        MQListener listener = listenerDefinition(listenerBean);
        TdmqMQClient client = new TdmqMQClient(properties);
        try {
            client.init(List.of(listener), properties, new JsonMQEventSerialization(), null);

            // 装配契约：impl 标识
            assertThat(client.impl()).isEqualTo("tdmq");
            // 装配契约：init 后生产者必须已注册到 BaseContext（key = impl()），否则 publish 只会 warn 后静默丢弃
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            assertThat(publishers).as("init 应把 TDMQ 生产者注册到 BaseContext").isNotNull();
            assertThat(publishers).containsKey("tdmq");

            // 假凭据断言：格式合法的假凭据已进入共享配置（离线装配等价于 boot relaxed binding 断言）
            assertThat(properties.getServiceUrl()).isEqualTo(FAKE_SERVICE_URL);
            assertThat(properties.getAccessKey()).isEqualTo(FAKE_ACCESS_KEY);

            OrderCreatedEvent sent = new OrderCreatedEvent();
            sent.setTopic(TOPIC);
            sent.setTag(TAG);
            sent.setOrderId("T-001");
            sent.publish();

            await(() -> listenerBean.matches("T-001"), Duration.ofSeconds(10));
            OrderCreatedEvent received = listenerBean.getReceived();
            assertThat(received).as("事件应在超时内被监听器收到").isNotNull();
            assertThat(received.getOrderId()).isEqualTo("T-001");
            assertThat(received.getTopic()).isEqualTo(TOPIC);
            assertThat(received.getTag()).isEqualTo(TAG);
        } finally {
            client.close();
        }
    }

    @Test
    void shouldSkipInitializationWhenBrokerPropertyMismatch() {
        // MQClient#init 守卫契约：broker 与 impl() 不一致（大小写敏感相等）时直接 return，
        // 不注册 publisher、不注入共享配置（对齐 boot 的 shouldBackOffWhenBrokerPropertyMismatch）
        TdmqProperties properties = fakeProperties();
        properties.setBroker("other");
        TdmqMQClient client = new TdmqMQClient(properties);
        try {
            client.init(List.of(), properties, new JsonMQEventSerialization(), null);
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            assertThat(publishers).as("broker 不匹配时不应注册任何 publisher").isNull();
        } finally {
            client.close();
        }
    }

    @Test
    void shouldFailFastWhenConsumerAutoStartDisabled() throws Exception {
        // required 监听器 + autoStartConsumers=false：initConsumer 返回 false，
        // init 按必选监听器契约抛 MQInitializationException 并回滚（对齐 boot 的 fail-fast 语义）
        TdmqProperties properties = fakeProperties();
        properties.setAutoStartConsumers(false);
        OrderCreatedListener listenerBean = new OrderCreatedListener();
        MQListener listener = listenerDefinition(listenerBean);
        TdmqMQClient client = new TdmqMQClient(properties);
        try {
            assertThatThrownBy(() -> client.init(List.of(listener), properties,
                    new JsonMQEventSerialization(), null))
                    .isInstanceOf(MQInitializationException.class)
                    .hasMessageContaining("tdmq");
            // 失败回滚：publisher 不应残留
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            assertThat(publishers).isNull();
        } finally {
            client.close();
        }
    }

    /**
     * round-trip 监听器（纯 Java 实例，非 CDI Bean——本测试不启动 Quarkus runtime）。
     * group 非空由注解声明；TDMQ 未声明时回落 {@code defaultGroup}。
     */
    public static class OrderCreatedListener {

        private final List<OrderCreatedEvent> received = new CopyOnWriteArrayList<>();

        @MQEventListener(topic = TOPIC, tags = TAG, group = "ddd4j-it")
        public void onOrderCreated(OrderCreatedEvent event) {
            received.add(event);
        }

        /** 是否已收到指定 orderId 的事件。 */
        public boolean matches(String orderId) {
            return received.stream().anyMatch(event -> orderId.equals(event.getOrderId()));
        }

        /** 获取最近一次收到的事件。 */
        public OrderCreatedEvent getReceived() {
            return received.isEmpty() ? null : received.get(received.size() - 1);
        }
    }

    /** 订单创建事件（继承 {@link MQEvent}，含 no-arg 构造 + getter/setter 保证 JSON 反序列化）。 */
    public static class OrderCreatedEvent extends MQEvent {

        private static final long serialVersionUID = 1L;

        private String orderId;

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }
    }
}
