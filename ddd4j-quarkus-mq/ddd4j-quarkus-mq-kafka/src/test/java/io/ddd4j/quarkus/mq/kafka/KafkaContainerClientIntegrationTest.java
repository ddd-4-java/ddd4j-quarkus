package io.ddd4j.quarkus.mq.kafka;

import io.ddd4j.core.context.BaseContext;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.kafka.KafkaMQClient;
import io.ddd4j.mq.kafka.KafkaMQProperties;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import org.junit.jupiter.api.Test;
import org.testcontainers.kafka.ConfluentKafkaContainer;
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
 * kafka MQ 客户端级端到端集成测试（Testcontainers，纯 JUnit 手动装配）。
 *
 * <p>覆盖：真实 Kafka 上的 {@code MQClient#init} → {@code MQEvent.publish()} →
 * broker 路由（topic {@code order_created}，tag 走应用层 {@code TagMatcher} 过滤）→
 * {@code @MQEventListener} 监听器 → JSON 反序列化全链路，并断言 {@code init} 把生产者
 * 注册到 {@link BaseContext}（key = {@link MQClient#impl()}）的装配契约。
 * 无 Docker 环境时自动跳过（{@code @Testcontainers(disabledWithoutDocker = true)}），不伪报通过。
 *
 * <p>与 {@link KafkaQuarkusIntegrationTest}（{@code @QuarkusTest} + Arc 装配）互补：
 * 本测试不启动 Quarkus runtime，直接构造 {@link KafkaMQProperties} / {@link KafkaMQClient}
 * 手动 {@code init}，对齐 ddd4j-boot 同款客户端级集成测试（照抄其已验证的容器方案）。
 *
 * <p>容器方案：官方 Testcontainers kafka 模块（{@link ConfluentKafkaContainer}），镜像钉版
 * {@code confluentinc/cp-kafka:7.7.2}（KRaft 单节点），与本仓 fixture 及 ddd4j-boot 同款。
 * 4 个 env 与 fixture 对齐：自动建 topic + 单副本（offsets/事务日志副本因子与最小 ISR 压到 1），
 * 业务 topic {@code order_created} 依赖 broker 自动创建（producer 仅 {@code ensureTopic}
 * 兜底 default topic {@code DEFAULT}）；consumer 侧默认 {@code auto.offset.reset=earliest}
 * 且关闭自动提交，先 publish 后 subscribe 也不丢消息。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Testcontainers(disabledWithoutDocker = true)
class KafkaContainerClientIntegrationTest {

    /** round-trip 断言用 topic/tag（按 kafka 拼接符 {@code _} 组成 topic {@code order_created}）。 */
    private static final String TOPIC = "order";
    private static final String TAG = "created";

    @Container
    static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer(
            // 测试镜像来源: https://testcontainers.com/modules/kafka/
            DockerImageName.parse("confluentinc/cp-kafka:7.7.2"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1");

    @Test
    void shouldPublishAndConsumeEventThroughRealKafkaContainer() throws Exception {
        // 手动装配（对齐 boot 客户端级测试的装配契约）：broker 必须与 impl() 大小写一致
        // （MQClient#init 的守卫是 Objects.equals(properties.getBroker(), impl())，大小写敏感，
        // 不匹配时 init 直接 return，publish 只会 warn 后静默丢弃）。
        KafkaMQProperties properties = new KafkaMQProperties();
        properties.setEnabled(true);
        properties.setBroker("kafka");
        // bootstrap-servers 形如 PLAINTEXT://宿主机:映射端口（ConfluentKafkaContainer 内置就绪等待）
        properties.setBootstrapServers(KAFKA.getBootstrapServers());

        OrderCreatedListener listenerBean = new OrderCreatedListener();
        MQListener listener = listenerDefinition(listenerBean);
        KafkaMQClient client = new KafkaMQClient(properties, null);
        try {
            client.init(List.of(listener), properties, new JsonMQEventSerialization(), null);

            // 装配契约：init 后生产者必须注册到 BaseContext（key = impl()），否则 publish 只会 warn 后静默丢弃
            assertThat(client.impl()).isEqualTo("kafka");
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            assertThat(publishers).as("init 应把 Kafka 生产者注册到 BaseContext").isNotNull();
            assertThat(publishers).containsKey("kafka");

            // consumer 组加入/订阅异步生效，先稳定等待再发布，避免 publish 早于 subscribe（对齐骨架 settle 约定）
            Thread.sleep(3_000L);

            OrderCreatedEvent sent = new OrderCreatedEvent();
            sent.setTopic(TOPIC);
            sent.setTag(TAG);
            sent.setOrderId("O-001");
            sent.publish();

            await(() -> listenerBean.matches("O-001"), Duration.ofSeconds(30));
            OrderCreatedEvent received = listenerBean.getReceived();
            assertThat(received).as("事件应在超时内被监听器收到").isNotNull();
            assertThat(received.getOrderId()).isEqualTo("O-001");
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
     * kafka consumer 组以 {@code ddd4j-方法名} 兜底，此处显式给组名保证可读。
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
