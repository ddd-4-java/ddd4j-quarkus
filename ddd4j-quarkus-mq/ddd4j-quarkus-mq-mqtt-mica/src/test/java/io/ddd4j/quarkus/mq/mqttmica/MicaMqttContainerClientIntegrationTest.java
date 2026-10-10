package io.ddd4j.quarkus.mq.mqttmica;

import io.ddd4j.core.context.BaseContext;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.mqttmica.MicaMqttMQClient;
import io.ddd4j.mq.mqttmica.MicaMqttProperties;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
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
 * mqtt-mica MQ 客户端级端到端集成测试（Testcontainers，纯 JUnit 手动装配）。
 *
 * <p>覆盖：真实 MQTT 上 mica-mqtt 客户端的 {@code MQClient#init} → {@code MQEvent.publish()} →
 * broker 路由（topic {@code order/created}，订阅 {@code order/created/#}，tag 走应用层
 * {@code TagMatcher} 过滤）→ {@code @MQEventListener} 监听器 → JSON 反序列化全链路，并断言
 * {@code init} 把生产者注册到 {@link BaseContext}（key = {@link MQClient#impl()}）的装配契约。
 * 无 Docker 环境时自动跳过（{@code @Testcontainers(disabledWithoutDocker = true)}），不伪报通过。
 *
 * <p>与 {@link MicaMqttQuarkusIntegrationTest}（{@code @QuarkusTest} + Arc 装配）互补：
 * 本测试不启动 Quarkus runtime，直接构造 {@link MicaMqttProperties} / {@link MicaMqttMQClient}
 * 手动 {@code init}，对齐 ddd4j-boot 同款客户端级集成测试（照抄其已验证的容器方案）。
 *
 * <p>容器方案：同 ddd4j-boot 选 {@code eclipse-mosquitto:1.6.15}——mosquitto 1.x 默认配置即
 * 允许匿名连接（2.x 需显式 allow_anonymous），开箱监听 1883；mica-mqtt 按 serverIp + port 连接。
 * mica-mqtt 2.6.6 客户端（smart-socket AIO）在 macOS arm64 上有已知首连缺陷，round-trip 以
 * {@code @DisabledIf("isMacArm64")} 跳过（对齐 {@link MicaMqttQuarkusIntegrationTest} 先例），
 * Linux/Windows 真实执行。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Testcontainers(disabledWithoutDocker = true)
class MicaMqttContainerClientIntegrationTest {

    /** round-trip 断言用 topic/tag（按 mqtt-mica 拼接符 {@code /} 组成 topic {@code order/created}）。 */
    private static final String TOPIC = "order";
    private static final String TAG = "created";

    @Container
    static final GenericContainer<?> MOSQUITTO = new GenericContainer<>(
            // 测试镜像来源: https://testcontainers.com/modules/mosquitto/
            DockerImageName.parse("eclipse-mosquitto:1.6.15"))
            .withExposedPorts(1883)
            .waitingFor(Wait.forListeningPort());

    /**
     * round-trip：mica-mqtt 2.6.6 客户端（smart-socket AIO）在 macOS arm64 上首连被
     * broker 拒绝且 publish 静默丢失（核心库 ddd4j-mq-mqtt-mica 的 AIO 发送缺陷，
     * mosquitto/EMQX 均复现），对齐 javalin Ddd4jMicaMqttMqIT 先例；Linux/Windows CI 继续执行。
     */
    @Test
    @DisabledIf("isMacArm64")
    void shouldPublishAndConsumeEventThroughRealMicaMqttContainer() throws Exception {
        // 手动装配（对齐 boot 客户端级测试的装配契约）：broker 必须与 impl() 大小写一致
        // （MQClient#init 的守卫是 Objects.equals(properties.getBroker(), impl())，大小写敏感，
        // 不匹配时 init 直接 return，publish 只会 warn 后静默丢弃）。
        MicaMqttProperties properties = new MicaMqttProperties();
        properties.setEnabled(true);
        properties.setBroker("mqtt-mica");
        // mica-mqtt 客户端按 serverIp + port 连接（容器映射后的宿主机地址与端口）
        properties.setServerIp(MOSQUITTO.getHost());
        properties.setPort(MOSQUITTO.getMappedPort(1883));

        OrderCreatedListener listenerBean = new OrderCreatedListener();
        MQListener listener = listenerDefinition(listenerBean);
        MicaMqttMQClient client = new MicaMqttMQClient(properties);
        try {
            client.init(List.of(listener), properties, new JsonMQEventSerialization(), null);

            // 装配契约：init 后生产者必须注册到 BaseContext（key = impl()），否则 publish 只会 warn 后静默丢弃
            assertThat(client.impl()).isEqualTo("mqtt-mica");
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            assertThat(publishers).as("init 应把 mica-mqtt 生产者注册到 BaseContext").isNotNull();
            assertThat(publishers).containsKey("mqtt-mica");

            // 订阅异步生效，先稳定等待再发布，避免 publish 早于 subscribe（对齐骨架 settle 约定）
            Thread.sleep(3_000L);

            OrderCreatedEvent sent = new OrderCreatedEvent();
            sent.setTopic(TOPIC);
            sent.setTag(TAG);
            sent.setOrderId("M-001");
            sent.publish();

            await(() -> listenerBean.matches("M-001"), Duration.ofSeconds(30));
            OrderCreatedEvent received = listenerBean.getReceived();
            assertThat(received).as("事件应在超时内被监听器收到").isNotNull();
            assertThat(received.getOrderId()).isEqualTo("M-001");
            assertThat(received.getTopic()).isEqualTo(TOPIC);
            assertThat(received.getTag()).isEqualTo(TAG);
        } finally {
            client.close();
        }
    }

    /** 是否为 macOS arm64（mica-mqtt 2.6.6 AIO 已知缺陷平台，round-trip 跳过）。 */
    static boolean isMacArm64() {
        return isMacArm64(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
    }

    /** 平台判定（分离纯函数便于单元验证，对齐 {@link MicaPlatformConditionTest} 先例）。 */
    static boolean isMacArm64(String osName, String osArch) {
        return "Mac OS X".equalsIgnoreCase(osName)
                && ("aarch64".equalsIgnoreCase(osArch) || "arm64".equalsIgnoreCase(osArch));
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
