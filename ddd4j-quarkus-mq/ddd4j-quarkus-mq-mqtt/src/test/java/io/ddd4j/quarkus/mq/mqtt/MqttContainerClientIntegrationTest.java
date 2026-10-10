package io.ddd4j.quarkus.mq.mqtt;

import io.ddd4j.core.context.BaseContext;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.mqtt.MqttMQClient;
import io.ddd4j.mq.mqtt.MqttMQProperties;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * mqtt MQ 客户端级端到端集成测试（Testcontainers，纯 JUnit 手动装配）。
 *
 * <p>覆盖：真实 MQTT 上 Paho 客户端的 {@code MQClient#init} → {@code MQEvent.publish()} →
 * broker 路由（topic {@code order/created}，订阅 {@code order/created/#}，tag 走应用层
 * {@code TagMatcher} 过滤）→ {@code @MQEventListener} 监听器 → JSON 反序列化全链路，并断言
 * {@code init} 把生产者注册到 {@link BaseContext}（key = {@link MQClient#impl()}）的装配契约。
 * 无 Docker 环境时自动跳过（{@code @Testcontainers(disabledWithoutDocker = true)}），不伪报通过。
 *
 * <p>与 {@link MqttQuarkusIntegrationTest}（{@code @QuarkusTest} + Arc 装配）互补：
 * 本测试不启动 Quarkus runtime，直接构造 {@link MqttMQProperties} / {@link MqttMQClient}
 * 手动 {@code init}，对齐 ddd4j-boot 同款客户端级集成测试（照抄其已验证的容器方案）。
 *
 * <p>容器方案：镜像选 {@code eclipse-mosquitto:1.6.15} 而非 2.x——mosquitto 2.x 默认配置
 * 不再允许匿名连接（需挂载 allow_anonymous 配置），1.x 默认即允许匿名、开箱监听 1883，
 * 与 ddd4j-boot 已验证方案同款（本仓 fixture 的 2.0.22 挂 conf 属于 QuarkusTestResource 场景）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Testcontainers(disabledWithoutDocker = true)
class MqttContainerClientIntegrationTest {

    /** round-trip 断言用 topic/tag（按 mqtt 拼接符 {@code /} 组成 topic {@code order/created}）。 */
    private static final String TOPIC = "order";
    private static final String TAG = "created";

    @Container
    static final GenericContainer<?> MOSQUITTO = new GenericContainer<>(
            // 测试镜像来源: https://testcontainers.com/modules/mosquitto/
            DockerImageName.parse("eclipse-mosquitto:1.6.15"))
            .withExposedPorts(1883)
            .waitingFor(Wait.forListeningPort());

    @Test
    void shouldPublishAndConsumeEventThroughRealMqttContainer() throws Exception {
        // 手动装配（对齐 boot 客户端级测试的装配契约）：broker 必须与 impl() 大小写一致
        // （MQClient#init 的守卫是 Objects.equals(properties.getBroker(), impl())，大小写敏感，
        // 不匹配时 init 直接 return，publish 只会 warn 后静默丢弃）。
        MqttMQProperties properties = new MqttMQProperties();
        properties.setEnabled(true);
        properties.setBroker("mqtt");
        // serverUri 形如 tcp://宿主机:映射端口；mosquitto 1.x 默认匿名，无需注入用户名密码
        properties.setServerUri(serverUri());

        OrderCreatedListener listenerBean = new OrderCreatedListener();
        MQListener listener = listenerDefinition(listenerBean);
        MqttMQClient client = new MqttMQClient(properties);
        try {
            client.init(List.of(listener), properties, new JsonMQEventSerialization(), null);

            // 装配契约：init 后生产者必须注册到 BaseContext（key = impl()），否则 publish 只会 warn 后静默丢弃
            assertThat(client.impl()).isEqualTo("mqtt");
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            assertThat(publishers).as("init 应把 Paho 生产者注册到 BaseContext").isNotNull();
            assertThat(publishers).containsKey("mqtt");

            // 订阅异步生效，先稳定等待再发布，避免 publish 早于 subscribe（对齐骨架 settle 约定）
            Thread.sleep(3_000L);

            OrderCreatedEvent sent = new OrderCreatedEvent();
            sent.setTopic(TOPIC);
            sent.setTag(TAG);
            sent.setOrderId("P-001");
            sent.publish();

            await(() -> listenerBean.matches("P-001"), Duration.ofSeconds(30));
            OrderCreatedEvent received = listenerBean.getReceived();
            assertThat(received).as("事件应在超时内被监听器收到").isNotNull();
            assertThat(received.getOrderId()).isEqualTo("P-001");
            assertThat(received.getTopic()).isEqualTo(TOPIC);
            assertThat(received.getTag()).isEqualTo(TAG);
        } finally {
            client.close();
        }
    }

    /**
     * 返回容器映射后的 MQTT broker 地址（mosquitto 1.x 默认允许匿名连接，
     * {@code MqttMQProperties#connectOptions()} 仅在 username/password 非空时才注入凭据）。
     *
     * @return {@code tcp://host:port} 形式的 broker URI
     */
    private static String serverUri() {
        return "tcp://" + MOSQUITTO.getHost() + ":" + MOSQUITTO.getMappedPort(1883);
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
            Thread.sleep(200L);
        }
        assertThat(condition.getAsBoolean()).as("condition not met within " + timeout).isTrue();
    }

    /**
     * round-trip 监听器（纯 Java 实例，非 CDI Bean——本测试不启动 Quarkus runtime，
     * 由测试在 init 时以 {@link MQListener#of} 注册）。
     */
    public static class OrderCreatedListener {

        private final List<OrderCreatedEvent> received = new CopyOnWriteArrayList<>();

        @MQEventListener(topic = TOPIC, tags = TAG, group = "ddd4j-it")
        public void onOrderCreated(OrderCreatedEvent event) {
            received.add(event);
        }

        /** 是否已收到指定 orderId 的事件（过滤 broker 中残留的陈旧消息）。 */
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
