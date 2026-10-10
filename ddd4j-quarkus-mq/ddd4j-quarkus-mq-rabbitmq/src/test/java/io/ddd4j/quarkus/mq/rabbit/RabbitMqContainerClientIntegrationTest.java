package io.ddd4j.quarkus.mq.rabbit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.rabbitmq.RabbitMQClient;
import io.ddd4j.mq.rabbitmq.RabbitMQProperties;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
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
 * rabbit MQ 客户端级端到端集成测试（Testcontainers，纯 JUnit 手动装配）。
 *
 * <p>覆盖：真实 RabbitMQ 上的 {@code MQClient#init} → {@code MQEvent.publish()} →
 * topic exchange 路由（路由键 {@code order.created}，tag 走应用层 {@code TagMatcher}
 * 过滤）→ {@code @MQEventListener} 监听器 → JSON 反序列化全链路，并断言 {@code init}
 * 把生产者注册到 {@link BaseContext}（key = {@link MQClient#impl()}）的装配契约。
 * 无 Docker 环境时自动跳过（{@code @Testcontainers(disabledWithoutDocker = true)}），不伪报通过。
 *
 * <p>与 {@link RabbitMqQuarkusIntegrationTest}（{@code @QuarkusTest} + Arc 装配）互补：
 * 本测试不启动 Quarkus runtime，直接构造 {@link RabbitMQProperties} / {@link RabbitMQClient}
 * 手动 {@code init}，对齐 ddd4j-boot 同款客户端级集成测试（照抄其已验证的容器方案）。
 *
 * <p><b>三个 broker 前置条件</b>（上游 {@code RabbitMQClient} 不负责声明，照抄 boot
 * {@code RabbitMQClientIntegrationTest}；手动装配下 exchange 与连接参数收敛到同一份
 * properties 对象，随 {@code init} 传入即可，不再区分全局/专属前缀）：
 * <ul>
 *   <li><b>凭据</b>：上游 {@link RabbitMQProperties} 默认 username/password 为空串，
 *       空凭据必被 RabbitMQ 拒绝（PLAIN 认证失败），故 {@link #beforeAll} 用
 *       {@code rabbitmqctl} 显式创建用户并授权。不用 {@code withUser()}：
 *       Testcontainers 的 {@code withUser()} 只追加一条 {@code rabbitmqadmin declare user ...}
 *       （无 administrator 标签、无 vhost 权限），环境变量仍是默认 guest/guest，
 *       创建出的用户无法访问 vhost</li>
 *   <li><b>交换机</b>：默认 {@code exchange=""} 是 RabbitMQ 直连默认交换机，路由键必须
 *       等于队列名，而上游队列名是 {@code group.namespace.Class.method}，永远无法匹配
 *       {@code namespace.topic.tag} 路由键；且上游 {@code initConsumer} 无条件
 *       {@code queueBind}，对默认交换机会被 RabbitMQ 以 403 拒绝。故必须指向预声明的
 *       topic 交换机（{@link #beforeAll} 声明）</li>
 *   <li><b>属性前缀</b>：连接参数（host/port/username/password）与路由参数（exchange）
 *       同在 {@link RabbitMQProperties}（含父类字段），一起随 {@code init} 传入</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Testcontainers(disabledWithoutDocker = true)
class RabbitMqContainerClientIntegrationTest {

    private static final String USER = "ddd4j";

    private static final String PASSWORD = "ddd4j-secret";

    private static final String EXCHANGE = "ddd4j-events";

    /** round-trip 断言用 topic/tag（路由键 = topic + "." + tag）。 */
    private static final String TOPIC = "order";

    private static final String TAG = "created";

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer(
            // 测试镜像来源: https://testcontainers.com/modules/rabbitmq/
            DockerImageName.parse("rabbitmq:3.13-management"));

    @BeforeAll
    static void beforeAll() throws Exception {
        createUserAndPermissions();
        declareTopicExchange();
    }

    /** 用 rabbitmqctl 自建用户并授权（不用 withUser()，原因见类注释「凭据」）。 */
    private static void createUserAndPermissions() throws Exception {
        RABBIT.execInContainer("rabbitmqctl", "add_user", USER, PASSWORD);
        RABBIT.execInContainer("rabbitmqctl", "set_user_tags", USER, "administrator");
        RABBIT.execInContainer("rabbitmqctl", "set_permissions", "-p", "/", USER, ".*", ".*", ".*");
    }

    /** 预声明持久化 topic 交换机（与生产一致：交换机由运维预声明，上游 client 不负责创建）。 */
    private static void declareTopicExchange() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBIT.getHost());
        factory.setPort(RABBIT.getAmqpPort());
        factory.setUsername(USER);
        factory.setPassword(PASSWORD);
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE, "topic", true);
        }
    }

    @Test
    void shouldPublishAndConsumeEventThroughRealRabbitContainer() throws Exception {
        // 手动装配（对齐 boot 客户端级测试的装配契约）：假定属性已绑定，broker 必须与 impl() 大小写一致
        // （MQClient#init 的守卫是 Objects.equals(properties.getBroker(), impl())，大小写敏感）。
        RabbitMQProperties properties = new RabbitMQProperties();
        properties.setEnabled(true);
        properties.setBroker("rabbit");
        // 路由参数：exchange 必须指向预声明的 topic 交换机（默认交换机永远匹配不上路由键）
        properties.setExchange(EXCHANGE);
        // 连接参数：容器动态映射端口 + 自建用户
        properties.setHost(RABBIT.getHost());
        properties.setPort(RABBIT.getAmqpPort());
        properties.setUsername(USER);
        properties.setPassword(PASSWORD);

        OrderCreatedListener listenerBean = new OrderCreatedListener();
        MQListener listener = listenerDefinition(listenerBean);

        RabbitMQClient client = new RabbitMQClient(properties);
        try {
            client.init(List.of(listener), properties, new JsonMQEventSerialization(), null);

            // 装配契约：init 后生产者必须已注册到 BaseContext（key = impl()），否则 publish 只会 warn 后静默丢弃
            assertThat(client.impl()).isEqualTo("rabbit");
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            assertThat(publishers).as("init 应把 RabbitMQ 生产者注册到 BaseContext").isNotNull();
            assertThat(publishers).containsKey("rabbit");

            // 先稳定等待订阅生效再发布，避免 publish 早于 subscribe（对齐骨架 settle 约定）
            Thread.sleep(3_000L);

            OrderCreatedEvent sent = new OrderCreatedEvent();
            sent.setTopic(TOPIC);
            sent.setTag(TAG);
            sent.setOrderId("R-001");
            sent.publish();

            await(() -> listenerBean.matches("R-001"), Duration.ofSeconds(30));
            OrderCreatedEvent received = listenerBean.getReceived();
            assertThat(received).as("事件应在超时内被监听器收到").isNotNull();
            assertThat(received.getOrderId()).isEqualTo("R-001");
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
     * 由测试在 init 时以 {@link MQListener#of} 注册）。group 决定队列名
     * {@code group.namespace.Class.method}，必须非空。
     */
    public static class OrderCreatedListener {

        private final List<OrderCreatedEvent> received = new CopyOnWriteArrayList<>();

        @MQEventListener(topic = TOPIC, tags = TAG, group = "ddd4j-it")
        public void onOrderCreated(OrderCreatedEvent event) {
            received.add(event);
        }

        /** 是否已收到指定 orderId 的事件（过滤复用队列中残留的陈旧消息）。 */
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
