package io.ddd4j.quarkus.mq.nats;

import io.ddd4j.core.context.BaseContext;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.nats.NatsMQClient;
import io.ddd4j.mq.nats.NatsProperties;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.api.StreamConfiguration;
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
 * nats MQ 客户端级端到端集成测试（Testcontainers，纯 JUnit 手动装配）。
 *
 * <p>覆盖：真实 NATS 上的 {@code MQClient#init} → {@code MQEvent.publish()} →
 * JetStream subject 路由（topic {@code order} + tag {@code created} → subject
 * {@code order.created}，tag 走应用层 {@code TagMatcher} 过滤）→
 * {@code @MQEventListener} 监听器 → JSON 反序列化全链路，并断言 {@code init} 把生产者
 * 注册到 {@link BaseContext}（key = {@link MQClient#impl()}）的装配契约。
 * 无 Docker 环境时自动跳过（{@code @Testcontainers(disabledWithoutDocker = true)}），不伪报通过。
 *
 * <p>与 {@link NatsQuarkusIntegrationTest}（{@code @QuarkusTest} + Arc 装配）互补：
 * 本测试不启动 Quarkus runtime，直接构造 {@link NatsProperties} / {@link NatsMQClient}
 * 手动 {@code init}，对齐 ddd4j-boot 同款客户端级集成测试（照抄其已验证的容器方案）。
 *
 * <p><b>NATS 三个前置条件</b>（照抄 boot {@code NatsMQClientIntegrationTest}）：
 * <ul>
 *   <li><b>JetStream 开关</b>：上游 {@code NatsMQClient} 发布/订阅都走 JetStream，
 *       容器必须以 {@code -js} 启动，否则 publish 直接失败</li>
 *   <li><b>stream 预创建</b>：JetStream 要求 subject 被 stream 覆盖，故
 *       {@code init} 前先建 stream（subjects {@code order.*}），与生产一致：stream 由运维预创建</li>
 *   <li><b>group 非空</b>：{@code initConsumer} 用 {@code PushSubscribeOptions.durable(group)}，
 *       空 group 会让 JetStream durable 订阅失败，故注解必须指定 {@code group}</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Testcontainers(disabledWithoutDocker = true)
class NatsContainerClientIntegrationTest {

    /** round-trip 断言用 topic/tag（subject = topic + "." + tag）。 */
    private static final String TOPIC = "order";

    private static final String TAG = "created";

    /** JetStream stream 名（覆盖发布与订阅 subject {@code order.created}）。 */
    private static final String STREAM = "DDD4J_QUARKUS_ORDER";

    @Container
    static final GenericContainer<?> NATS = new GenericContainer<>(
            // 测试镜像来源: https://testcontainers.com/modules/nats/
            DockerImageName.parse("nats:2.10.22"))
            // 上游客户端走 JetStream，必须显式启用（-js）
            .withCommand("-js")
            .withExposedPorts(4222)
            .waitingFor(Wait.forListeningPort());

    @Test
    void shouldPublishAndConsumeEventThroughRealNatsContainer() throws Exception {
        String servers = "nats://" + NATS.getHost() + ":" + NATS.getMappedPort(4222);

        // 预创建 JetStream stream 覆盖发布/订阅 subject（stream 不存在时 JetStream publish 会失败）
        try (Connection connection = Nats.connect(servers)) {
            connection.jetStreamManagement().addStream(StreamConfiguration.builder()
                    .name(STREAM)
                    .subjects("order.*")
                    .build());
        }

        // 手动装配（对齐 boot 客户端级测试的装配契约）：假定属性已绑定，broker 必须与 impl() 大小写一致
        // （MQClient#init 的守卫是 Objects.equals(properties.getBroker(), impl())，大小写敏感）。
        NatsProperties properties = new NatsProperties();
        properties.setEnabled(true);
        properties.setBroker("nats");
        properties.setServers(servers);

        OrderCreatedListener listenerBean = new OrderCreatedListener();
        MQListener listener = listenerDefinition(listenerBean);

        NatsMQClient client = new NatsMQClient(properties);
        try {
            client.init(List.of(listener), properties, new JsonMQEventSerialization(), null);

            // 装配契约：init 后生产者必须已注册到 BaseContext（key = impl()），否则 publish 只会 warn 后静默丢弃
            assertThat(client.impl()).isEqualTo("nats");
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            assertThat(publishers).as("init 应把 NATS 生产者注册到 BaseContext").isNotNull();
            assertThat(publishers).containsKey("nats");

            // 先稳定等待订阅生效再发布，避免 publish 早于 subscribe（对齐骨架 settle 约定）
            Thread.sleep(3_000L);

            OrderCreatedEvent sent = new OrderCreatedEvent();
            sent.setTopic(TOPIC);
            sent.setTag(TAG);
            sent.setOrderId("N-001");
            sent.publish();

            await(() -> listenerBean.matches("N-001"), Duration.ofSeconds(30));
            OrderCreatedEvent received = listenerBean.getReceived();
            assertThat(received).as("事件应在超时内被监听器收到").isNotNull();
            assertThat(received.getOrderId()).isEqualTo("N-001");
            assertThat(received.getTopic()).isEqualTo(TOPIC);
            assertThat(received.getTag()).isEqualTo(TAG);
        } finally {
            client.close();
        }
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
     * 由测试在 init 时以 {@link MQListener#of} 注册）。group 必须非空：
     * JetStream durable 订阅直接使用 group 作为 durable 名。
     */
    public static class OrderCreatedListener {

        private final List<OrderCreatedEvent> received = new CopyOnWriteArrayList<>();

        @MQEventListener(topic = TOPIC, tags = TAG, group = "ddd4j-it")
        public void onOrderCreated(OrderCreatedEvent event) {
            received.add(event);
        }

        /** 是否已收到指定 orderId 的事件（过滤复用 stream 中残留的陈旧消息）。 */
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
