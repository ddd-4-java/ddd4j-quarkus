package io.ddd4j.quarkus.mq.pulsar;

import io.ddd4j.core.context.BaseContext;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.pulsar.PulsarMQClient;
import io.ddd4j.mq.pulsar.PulsarProperties;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * pulsar MQ 客户端级端到端集成测试（Testcontainers，纯 JUnit 手动装配）。
 *
 * <p>覆盖：真实 Pulsar standalone 上的 {@code MQClient#init} → {@code MQEvent.publish()} →
 * 物理 topic 路由（{@code public/default/order}，事件不带 tag、tag 走应用层
 * {@code TagMatcher} 过滤）→ {@code @MQEventListener} 监听器 → JSON 反序列化全链路，
 * 并断言 {@code init} 把生产者注册到 {@link BaseContext}（key = {@link MQClient#impl()}）
 * 的装配契约。
 * 无 Docker 环境时自动跳过（{@code @Testcontainers(disabledWithoutDocker = true)}），不伪报通过。
 *
 * <p>与 {@link PulsarQuarkusIntegrationTest}（{@code @QuarkusTest} + Arc 装配）互补：
 * 本测试不启动 Quarkus runtime，直接构造 {@link PulsarProperties} / {@link PulsarMQClient}
 * 手动 {@code init}，对齐 ddd4j-boot 同款客户端级集成测试（照抄其已验证的容器方案）。
 *
 * <p><b>Pulsar 三个关键点</b>（照抄 boot {@code PulsarMQClientIntegrationTest}）：
 * <ul>
 *   <li><b>standalone 启动慢</b>：30-60s，等待策略用日志匹配 + 180s 宽限超时</li>
 *   <li><b>namespace 必须为 default</b>：空 namespace 会拼出非法物理 topic {@code public//order}，
 *       故 {@link #ensureDefaultNamespace} 先用 admin API 预建 {@code public/default}</li>
 *   <li><b>tag 不对称</b>：producer 物理 topic 是 {@code tenant/namespace/topic[:tag]}，
 *       consumer 订阅的是 {@code tenant/namespace/topic}（tag 排除、应用层过滤），两者是
 *       不同的 Pulsar topic——事件必须不带 tag、监听器 tags 放行全部（{@code *}）才能对齐</li>
 * </ul>
 *
 * <p>测试结束必须 {@code close()} 客户端：PulsarClient 的 IO 线程非守护，不关会挂起 JVM。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Testcontainers(disabledWithoutDocker = true)
class PulsarContainerClientIntegrationTest {

    /** round-trip 断言用 topic（事件不带 tag，见类注释「tag 不对称」）。 */
    private static final String TOPIC = "order";

    @Container
    static final GenericContainer<?> PULSAR = new GenericContainer<>(
            // 测试镜像来源: https://testcontainers.com/modules/pulsar/
            DockerImageName.parse("apachepulsar/pulsar:3.2.0"))
            .withCommand("bin/pulsar", "standalone")
            .withExposedPorts(6650, 8080)
            .waitingFor(Wait.forLogMessage(".*messaging service is ready.*", 1)
                    .withStartupTimeout(Duration.ofSeconds(180)));

    @Test
    void shouldPublishAndConsumeEventThroughRealPulsarContainer() throws Exception {
        String serviceUrl = "pulsar://" + PULSAR.getHost() + ":" + PULSAR.getMappedPort(6650);
        // 先预建 public/default 命名空间（否则物理 topic public//order 非法，produce/consume 均失败）
        ensureDefaultNamespace();

        // 手动装配（对齐 boot 客户端级测试的装配契约）：假定属性已绑定，broker 必须与 impl() 大小写一致
        // （MQClient#init 的守卫是 Objects.equals(properties.getBroker(), impl())，大小写敏感）。
        PulsarProperties properties = new PulsarProperties();
        properties.setEnabled(true);
        properties.setBroker("pulsar");
        properties.setServiceUrl(serviceUrl);
        // 物理 topic = tenant/namespace/topic：standalone 默认 public/default，namespace 必须非空
        properties.setNamespace("default");
        properties.setSubscriptionName("ddd4j-it-sub");

        OrderCreatedListener listenerBean = new OrderCreatedListener();
        MQListener listener = listenerDefinition(listenerBean);

        PulsarMQClient client = new PulsarMQClient(properties);
        try {
            client.init(List.of(listener), properties, new JsonMQEventSerialization(), null);

            // 装配契约：init 后生产者必须已注册到 BaseContext（key = impl()），否则 publish 只会 warn 后静默丢弃
            assertThat(client.impl()).isEqualTo("pulsar");
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            assertThat(publishers).as("init 应把 Pulsar 生产者注册到 BaseContext").isNotNull();
            assertThat(publishers).containsKey("pulsar");

            // 先稳定等待订阅生效再发布，避免 publish 早于 subscribe（对齐骨架 settle 约定）
            Thread.sleep(3_000L);

            OrderCreatedEvent sent = new OrderCreatedEvent();
            sent.setTopic(TOPIC);
            // 事件不带 tag：producer 物理 topic 含 ":tag" 而 consumer 订阅地址不含，带 tag 会指向不同 topic
            sent.setOrderId("P-001");
            sent.publish();

            // standalone 消费链路慢，对齐 boot 的 60s 约定
            await(() -> listenerBean.matches("P-001"), Duration.ofSeconds(60));
            OrderCreatedEvent received = listenerBean.getReceived();
            assertThat(received).as("事件应在超时内被监听器收到").isNotNull();
            assertThat(received.getOrderId()).isEqualTo("P-001");
            assertThat(received.getTopic()).isEqualTo(TOPIC);
            assertThat(received.getTag()).as("事件不带 tag，round-trip 后应仍为空").isNull();
        } finally {
            // 必须显式关闭：PulsarClient IO 线程非守护，不 close 会挂起 JVM（对齐 boot 约定）
            client.close();
        }
    }

    /** 预建 {@code public/default} 命名空间（admin API PUT，容忍 204 已建 / 409 已存在）。 */
    private static void ensureDefaultNamespace() throws Exception {
        URL namespaceUrl = new URL("http://" + PULSAR.getHost() + ":" + PULSAR.getMappedPort(8080)
                + "/admin/v2/namespaces/public/default");
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        while (System.nanoTime() < deadline) {
            HttpURLConnection connection = (HttpURLConnection) namespaceUrl.openConnection();
            connection.setRequestMethod("PUT");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.setDoOutput(true);
            connection.getOutputStream().close();
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT
                    || responseCode == HttpURLConnection.HTTP_CONFLICT) {
                return;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Pulsar namespace public/default was not ready within PT60S");
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
     * 由测试在 init 时以 {@link MQListener#of} 注册）。tags 必须为 {@code *}：
     * 事件不带 tag，consumer 收到的 tag 属性为空，非通配表达式会把消息全部过滤掉。
     */
    public static class OrderCreatedListener {

        private final List<OrderCreatedEvent> received = new CopyOnWriteArrayList<>();

        @MQEventListener(topic = TOPIC, tags = "*", group = "ddd4j-it")
        public void onOrderCreated(OrderCreatedEvent event) {
            received.add(event);
        }

        /** 是否已收到指定 orderId 的事件（过滤复用 topic 中残留的陈旧消息）。 */
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
