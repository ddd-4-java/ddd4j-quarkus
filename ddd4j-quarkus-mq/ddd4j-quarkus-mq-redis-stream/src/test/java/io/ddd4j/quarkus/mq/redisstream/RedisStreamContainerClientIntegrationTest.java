package io.ddd4j.quarkus.mq.redisstream;

import io.ddd4j.core.context.BaseContext;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.redisstream.RedisStreamMQClient;
import io.ddd4j.mq.redisstream.RedisStreamMQProperties;
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
 * redis-stream MQ 客户端级端到端集成测试（Testcontainers，纯 JUnit 手动装配）。
 *
 * <p>覆盖：真实 Redis Stream 上的 {@code MQClient#init} → {@code MQEvent.publish()} →
 * XADD stream key 路由（{@code order:created}，tag 经 {@code ddd4jTag} 字段走应用层
 * {@code TagMatcher} 过滤）→ 消费组 {@code XREADGROUP} → {@code @MQEventListener}
 * 监听器 → JSON 反序列化全链路，并断言 {@code init} 把生产者注册到 {@link BaseContext}
 * （key = {@link MQClient#impl()}）的装配契约。
 * 无 Docker 环境时自动跳过（{@code @Testcontainers(disabledWithoutDocker = true)}），不伪报通过。
 *
 * <p>与 {@link RedisStreamQuarkusIntegrationTest}（{@code @QuarkusTest} + Arc 装配）互补：
 * 本测试不启动 Quarkus runtime，直接构造 {@link RedisStreamMQProperties} /
 * {@link RedisStreamMQClient} 手动 {@code init}，对齐 ddd4j-boot 同款客户端级集成测试
 * （照抄其已验证的容器方案）。
 *
 * <p><b>Redis Stream 差异</b>：stream key 走默认拼接符 {@code :}（即 {@code order:created}），
 * 无 broker 端 selector，tag 经 {@code ddd4jTag} 字段应用层过滤；消费组由注解
 * {@code group} 提供（{@code autoCreateGroup=true} 时 {@code xgroupCreate} 自动建组），
 * 守护线程 {@code XREADGROUP} 轮询——group 必须非空，否则建组失败。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisStreamContainerClientIntegrationTest {

    /** round-trip 断言用 topic/tag（stream key = topic + ":" + tag）。 */
    private static final String TOPIC = "order";

    private static final String TAG = "created";

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            // 测试镜像来源: https://testcontainers.com/modules/redis/
            DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    @Test
    void shouldPublishAndConsumeEventThroughRealRedisStreamContainer() throws Exception {
        String url = "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379);

        // 手动装配（对齐 boot 客户端级测试的装配契约）：假定属性已绑定，broker 必须与 impl() 大小写一致
        // （MQClient#init 的守卫是 Objects.equals(properties.getBroker(), impl())，大小写敏感）。
        RedisStreamMQProperties properties = new RedisStreamMQProperties();
        properties.setEnabled(true);
        properties.setBroker("redisStream");
        properties.setUrl(url);

        OrderCreatedListener listenerBean = new OrderCreatedListener();
        MQListener listener = listenerDefinition(listenerBean);

        RedisStreamMQClient client = new RedisStreamMQClient(properties);
        try {
            client.init(List.of(listener), properties, new JsonMQEventSerialization(), null);

            // 装配契约：init 后生产者必须已注册到 BaseContext（key = impl()），否则 publish 只会 warn 后静默丢弃
            assertThat(client.impl()).isEqualTo("redisStream");
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            assertThat(publishers).as("init 应把 Redis Stream 生产者注册到 BaseContext").isNotNull();
            assertThat(publishers).containsKey("redisStream");

            // 消费走守护线程轮询，先稳定等待订阅生效再发布，避免 publish 早于 subscribe（对齐骨架 settle 约定）
            Thread.sleep(3_000L);

            OrderCreatedEvent sent = new OrderCreatedEvent();
            sent.setTopic(TOPIC);
            sent.setTag(TAG);
            sent.setOrderId("RS-001");
            sent.publish();

            await(() -> listenerBean.matches("RS-001"), Duration.ofSeconds(30));
            OrderCreatedEvent received = listenerBean.getReceived();
            assertThat(received).as("事件应在超时内被监听器收到").isNotNull();
            assertThat(received.getOrderId()).isEqualTo("RS-001");
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
     * 由测试在 init 时以 {@link MQListener#of} 注册）。group 即 Redis Stream 消费组名，
     * 必须非空（空组名 xgroupCreate 直接失败）。
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
