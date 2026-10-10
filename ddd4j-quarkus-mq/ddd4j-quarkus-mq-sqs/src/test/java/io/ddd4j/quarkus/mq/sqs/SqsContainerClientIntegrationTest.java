package io.ddd4j.quarkus.mq.sqs;

import io.ddd4j.core.context.BaseContext;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import io.ddd4j.mq.sqs.SqsMQClient;
import io.ddd4j.mq.sqs.SqsProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AWS SQS 客户端级端到端集成测试（Testcontainers LocalStack，纯 JUnit 手动装配）。
 *
 * <p>覆盖：LocalStack SQS 上的 {@code MQClient#init} → {@code MQEvent.publish()} →
 * 长轮询消费 → JSON 反序列化全链路，并断言 {@code init} 把生产者注册到
 * {@link BaseContext}（key = {@link MQClient#impl()}）的装配契约。无 Docker 环境时
 * 自动跳过（{@code disabledWithoutDocker = true}），不伪报通过。
 *
 * <p>与 {@link SqsQuarkusIntegrationTest}（{@code @QuarkusTest} + Arc 装配）互补：
 * 本测试不启动 Quarkus runtime，直接构造 {@link SqsProperties} / {@link SqsMQClient}
 * 手动 {@code init}，对齐 ddd4j-boot 同款客户端级集成测试。
 *
 * <p><b>SQS 差异</b>：SQS 没有 topic/tag 概念，{@code MQListener.topic} 与事件的 topic
 * 均为完整 queueUrl；注解 topic 用占位符常量（编译期常量），容器启动后在 {@link #beforeAll}
 * 创建队列、于 {@code init} 前改写为运行时 queueUrl（对齐既有 IT 的 adaptListener 做法）。
 * 容器端口动态分配，无固定端口冲突。
 */
@Testcontainers(disabledWithoutDocker = true)
class SqsContainerClientIntegrationTest {

    private static final String QUEUE_NAME = "ddd4j-events";

    /** 注解占位符（编译期常量），init 前改写为运行时 queueUrl。 */
    private static final String QUEUE_URL_PLACEHOLDER = "${test.sqs.queue-url}";

    private static final String ACCESS_KEY = "test";

    private static final String SECRET_KEY = "test";

    /** round-trip 断言用 tag（SQS 无 broker 端 tag 过滤，应用层 TagMatcher 兜底）。 */
    private static final String TAG = "created";

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            // 测试镜像来源: https://testcontainers.com/modules/localstack/
            // （SQS 由 LocalStack 模块覆盖，Testcontainers 官方模块索引无独立 SQS 模块）
            DockerImageName.parse("localstack/localstack:3.8.0"))
            .withServices("sqs");

    /** 运行时 queueUrl（LocalStack 固定账号段 000000000000），由 {@link #beforeAll} 创建队列后回填。 */
    private static String queueUrl;

    private static URI endpoint() {
        return LOCALSTACK.getEndpoint();
    }

    private static SqsClient sqsClient() {
        return SqsClient.builder()
                .endpointOverride(endpoint())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .build();
    }

    @BeforeAll
    static void createQueue() {
        try (SqsClient sqs = sqsClient()) {
            // 用 createQueue 返回的 queueUrl（含真实账号段与端口），避免手拼 URL 漂移
            queueUrl = sqs.createQueue(CreateQueueRequest.builder().queueName(QUEUE_NAME).build()).queueUrl();
        }
        assertThat(queueUrl).startsWith("http");
    }

    @Test
    void shouldPublishAndConsumeEventThroughLocalStackSqsContainer() throws Exception {
        // 手动装配（对齐 boot 客户端级测试的装配契约）：broker 必须与 impl() 大小写一致
        // （MQClient#init 的守卫是 Objects.equals(properties.getBroker(), impl())，大小写敏感）。
        SqsProperties properties = new SqsProperties();
        properties.setEnabled(true);
        properties.setBroker("sqs");
        properties.setEndpointOverride(endpoint().toString());
        properties.setRegion("us-east-1");
        properties.setAccessKey(ACCESS_KEY);
        properties.setSecretKey(SECRET_KEY);
        properties.setWaitTimeSeconds(2);
        properties.setPollIntervalMs(200L);

        OrderCreatedListener listenerBean = new OrderCreatedListener();
        MQListener listener = listenerDefinition(listenerBean);
        // SQS 契约：监听器 topic 必须是完整 queueUrl（initConsumer 对非 http 前缀直接抛错）
        listener.setTopic(queueUrl);

        SqsMQClient client = new SqsMQClient(properties);
        try {
            client.init(List.of(listener), properties, new JsonMQEventSerialization(), null);

            // 装配契约：init 后生产者必须已注册到 BaseContext（key = impl()），否则 publish 只会 warn 后静默丢弃
            assertThat(client.impl()).isEqualTo("sqs");
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            assertThat(publishers).as("init 应把 SQS 生产者注册到 BaseContext").isNotNull();
            assertThat(publishers).containsKey("sqs");

            OrderCreatedEvent sent = new OrderCreatedEvent();
            // SQS 契约：事件 topic 同样是完整 queueUrl（initProducer 直接把 topic 当 queueUrl 发送）
            sent.setTopic(queueUrl);
            sent.setTag(TAG);
            sent.setOrderId("S-001");
            sent.publish();

            // 长轮询 + 轮询调度有传播延迟，比本地 broker 放宽超时（对齐 boot 的 60s 约定）
            await(() -> listenerBean.matches("S-001"), Duration.ofSeconds(60));
            OrderCreatedEvent received = listenerBean.getReceived();
            assertThat(received).as("事件应在超时内被监听器收到").isNotNull();
            assertThat(received.getOrderId()).isEqualTo("S-001");
            assertThat(received.getTag()).isEqualTo(TAG);
            assertThat(received.getTopic()).isEqualTo(queueUrl);
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
     * round-trip 监听器（纯 Java 实例，非 CDI Bean——本测试不启动 Quarkus runtime）。
     * 注解 topic 为占位符，init 前由测试改写为运行时 queueUrl。
     */
    public static class OrderCreatedListener {

        private final List<OrderCreatedEvent> received = new CopyOnWriteArrayList<>();

        @MQEventListener(topic = QUEUE_URL_PLACEHOLDER, tags = TAG, group = "ddd4j-it")
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
