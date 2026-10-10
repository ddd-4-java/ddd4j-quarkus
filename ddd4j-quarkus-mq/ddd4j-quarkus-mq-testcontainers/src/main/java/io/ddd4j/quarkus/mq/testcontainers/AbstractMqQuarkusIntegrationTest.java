package io.ddd4j.quarkus.mq.testcontainers;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.listener.MQListener;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.BooleanSupplier;

/**
 * Quarkus MQ broker 端到端 round-trip 骨架（main scope，供 12 个 broker IT 继承）。
 *
 * <p>对齐既有 {@code KafkaQuarkusIntegrationTest#shouldPublishAndConsumeEventEndToEnd}
 * 的已验证模式（fixture 由 {@code @QuarkusTestResource} 启动并把连接信息注入 Quarkus
 * config，测试内读取 config 写入 broker 专属 properties 单例、归一化 broker 后手动
 * {@link MQClient#init}，再 {@code publish()} → broker → {@code @MQEventListener} 监听器），
 * 把骨架收敛到本基类；payload 统一为 DDD 业务事件 {@link OrderCreatedEvent}
 * （orderId/buyerId/amount），断言字段级 round-trip 一致。
 *
 * <p>子类只需提供：broker 专属 properties Bean（{@link #mqPropertiesExtension()}）与
 * 连接信息填充（{@link #applyContainerProperties}）；差异逻辑走钩子
 * {@link #adaptListener}（如 SQS queue URL 改写）与 {@link #preInit}
 * （如 RocketMQ producer warm-up、RabbitMQ exchange 指定）。
 *
 * <p><b>注</b>：{@code MQClient.init} 的 broker 校验大小写敏感
 * （{@code Objects.equals(properties.getBroker(), impl())}），fixture 注入的是大写
 * {@code KAFKA} 等，故模板先归一化 broker、finally 恢复，与既有 IT 解耦测试顺序。
 *
 * @param <P> broker 专属 properties 类型
 */
public abstract class AbstractMqQuarkusIntegrationTest<P extends MQProperties> {

    /**
     * 统一业务事件 topic/tag（对齐 sample 的 OrderCreatedEvent 模式）。
     */
    protected static final String TOPIC = "ORDER";
    /** 统一业务事件 tag（默认与 {@link #eventTag()} 保持一致）。 */
    protected static final String TAG = "CREATED";

    /** 容器注入连接信息后手动 init 的共享 MQ 客户端（CDI 单例）。 */
    @Inject
    protected MQClient mqClient;

    /** 共享 MQ 连接配置（init 时归一化 broker，finally 恢复原值）。 */
    @Inject
    protected MQProperties mqProperties;

    /** 事件序列化器（JSON round-trip 的编解码入口）。 */
    @Inject
    protected MQEventSerialization serialization;

    // ========================= 子类契约 =========================

    /**
     * broker 专属 properties Bean（CDI 单例，{@code initProducer/initConsumer} 从中读取连接信息）。
     * 无专属配置的 broker（disruptor/tdmq）返回 {@code null} 跳过填充。
     *
     * @return broker 专属 properties；无专属配置时返回 {@code null}
     */
    protected abstract P mqPropertiesExtension();

    /**
     * 把 fixture 注入 Quarkus config 的容器连接信息写入专属 properties
     * （config 键来自各 fixture 的 exposedProperties）。
     *
     * @param properties 待填充的 broker 专属 properties
     */
    protected abstract void applyContainerProperties(P properties);

    /**
     * init 之前的 broker 准备钩子（在连接信息填充之后、init 之前调用）。
     *
     * @throws Exception 准备失败时抛出，中断 round-trip
     */
    protected void preInit() throws Exception {
    }

    /**
     * init 之前改写监听器钩子（如 SQS 把 topic 改写为 queue URL），默认不动。
     *
     * @param listener 待改写的监听器定义
     * @throws Exception 改写失败时抛出，中断 round-trip
     */
    protected void adaptListener(MQListener listener) throws Exception {
    }

    /**
     * consumer 注册完成后的稳定等待（订阅异步生效，避免 publish 早于 subscribe）。
     *
     * @return 稳定等待毫秒数，默认 3 秒
     */
    protected long settleMillis() {
        return 3_000L;
    }

    /**
     * round-trip 异步断言的最长等待时间。
     *
     * @return 等待超时时长，默认 30 秒
     */
    protected Duration awaitTimeout() {
        return Duration.ofSeconds(30);
    }

    /**
     * 监听器订阅 tag 表达式（注解仅作占位，init 前由模板写入 MQListener）。
     * 默认与事件 tag 一致；Pulsar 等无 broker 端 tag 过滤且 producer/consumer 物理
     * 地址不对称的 broker 覆写为 {@code "*"}。
     *
     * @return 监听器订阅用的 tag 表达式
     */
    protected String listenerTags() {
        return TAG;
    }

    /**
     * 发布事件的 tag；返回 {@code null} 表示不带 tag（对齐 javalin Pulsar 先例——
     * pulsar producer 物理地址含 {@code :tag} 而 consumer 订阅地址不含，tag 必须为空才能对齐）。
     *
     * @return 发布事件的 tag；{@code null} 表示不带 tag
     */
    protected String eventTag() {
        return TAG;
    }

    /**
     * round-trip 事件工厂；SQS 等需要覆盖 topic（queue URL）的 broker 覆写本方法。
     *
     * @return 待发布的订单创建事件（orderId 唯一，避免复用容器残留消息干扰）
     */
    protected OrderCreatedEvent newOrderCreatedEvent() {
        return new OrderCreatedEvent("IT-" + System.nanoTime(), "buyer-it", "99.90");
    }

    // ========================= 工具 =========================

    /**
     * 读取 QuarkusTestResource 注入的 config 值。
     *
     * @param key 配置键（各 fixture 的 exposedProperties 键）
     * @return 配置值
     */
    protected static String config(String key) {
        return ConfigProvider.getConfig().getValue(key, String.class);
    }

    /**
     * 轮询直到条件满足或超时（不引入 awaitility 依赖；最终一次断言保证失败可读）。
     *
     * @param condition 轮询条件
     * @param timeout   最长等待时长
     */
    public static void await(BooleanSupplier condition, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Assertions.assertTrue(condition.getAsBoolean(),
                () -> "condition not met within " + timeout);
    }

    /**
     * 从共享 {@link OrderCreatedListener} 构建 {@link MQListener} 定义
     * （与 QuarkusMQListenerRegistrar 的扫描结果等价）。注解值无法按子类参数化，
     * tags 由 {@link #listenerTags()} 在运行时写入（对齐 javalin 骨架做法）。
     *
     * @param bean 监听器实例
     * @return 写入 {@link #listenerTags()} 后的监听器定义
     * @throws Exception 反射查找监听方法失败时抛出
     */
    protected final MQListener listenerDefinition(OrderCreatedListener bean) throws Exception {
        Method method = OrderCreatedListener.class.getDeclaredMethod("onOrderCreated", OrderCreatedEvent.class);
        MQListener listener = MQListener.of(bean, method, method.getAnnotation(MQEventListener.class));
        listener.setTags(listenerTags());
        return listener;
    }

    /**
     * consumer 注册完成后的稳定等待（订阅异步生效，避免 publish 早于 subscribe）。
     */
    protected final void settle() {
        try {
            Thread.sleep(settleMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ========================= round-trip 模板 =========================

    /**
     * 完整 round-trip：填充连接信息 → 改写监听器 → preInit → 归一化 broker + init →
     * settle → publish 业务事件 → await 监听器 → 断言 orderId/buyerId/amount 一致。
     *
     * @throws Exception broker 准备、init 或 publish 失败时抛出
     */
    protected final void runOrderCreatedRoundTrip() throws Exception {
        OrderCreatedListener bean = new OrderCreatedListener();
        MQListener listener = listenerDefinition(bean);

        P extension = mqPropertiesExtension();
        if (extension != null) {
            applyContainerProperties(extension);
        }
        adaptListener(listener);
        preInit();

        // init 的 broker 校验大小写敏感：fixture 注入大写（如 KAFKA），归一化后再手动 init，
        // finally 恢复共享单例，避免影响 shouldInjectMQProperties 断言（与测试顺序解耦）。
        String originalBroker = mqProperties.getBroker();
        mqProperties.setBroker(mqClient.impl());
        try {
            mqClient.init(List.of(listener), mqProperties, serialization, null);

            settle();
            OrderCreatedEvent sent = newOrderCreatedEvent();
            sent.setTag(eventTag());
            sent.publish();

            await(() -> bean.matches(sent.getOrderId()), awaitTimeout());
            OrderCreatedEvent received = bean.getReceived();
            Assertions.assertNotNull(received, "OrderCreatedEvent 应在超时内被 @MQEventListener 监听器收到");
            Assertions.assertEquals(sent.getOrderId(), received.getOrderId(), "orderId 应 round-trip 一致");
            Assertions.assertEquals(sent.getBuyerId(), received.getBuyerId(), "buyerId 应 round-trip 一致");
            Assertions.assertEquals(0, sent.getAmount().compareTo(received.getAmount()), "amount 应 round-trip 一致");
        } finally {
            mqProperties.setBroker(originalBroker);
        }
    }

    // ========================= 共享业务事件与监听器 =========================

    /**
     * DDD 业务事件：订单创建（topic=ORDER、tag=CREATED）。
     * 含 no-arg 构造 + getter/setter，保证 JSON 序列化 round-trip 可反序列化。
     */
    public static class OrderCreatedEvent extends MQEvent {

        private static final long serialVersionUID = 1L;

        /** 订单唯一标识。 */
        private String orderId;
        /** 下单用户标识。 */
        private String buyerId;
        /** 订单金额。 */
        private java.math.BigDecimal amount;

        /**
         * 无参构造函数（默认 topic/tag），供反序列化与手工赋值使用。
         */
        public OrderCreatedEvent() {
            setTopic(TOPIC);
            setTag(TAG);
        }

        /**
         * 全参构造函数。
         *
         * @param orderId 订单唯一标识
         * @param buyerId 下单用户标识
         * @param amount  订单金额（十进制字符串）
         */
        public OrderCreatedEvent(String orderId, String buyerId, String amount) {
            this();
            this.orderId = orderId;
            this.buyerId = buyerId;
            this.amount = new java.math.BigDecimal(amount);
        }

        /**
         * 获取订单唯一标识。
         *
         * @return 订单唯一标识
         */
        public String getOrderId() {
            return orderId;
        }

        /**
         * 设置订单唯一标识。
         *
         * @param orderId 订单唯一标识
         */
        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        /**
         * 获取下单用户标识。
         *
         * @return 下单用户标识
         */
        public String getBuyerId() {
            return buyerId;
        }

        /**
         * 设置下单用户标识。
         *
         * @param buyerId 下单用户标识
         */
        public void setBuyerId(String buyerId) {
            this.buyerId = buyerId;
        }

        /**
         * 获取订单金额。
         *
         * @return 订单金额
         */
        public java.math.BigDecimal getAmount() {
            return amount;
        }

        /**
         * 设置订单金额。
         *
         * @param amount 订单金额
         */
        public void setAmount(java.math.BigDecimal amount) {
            this.amount = amount;
        }
    }

    /**
     * round-trip 监听器（纯 Java 实例，非 CDI Bean——不参与启动期扫描，
     * 由测试在 init 时以 {@link MQListener#of} 注册）。收到匹配事件后放开 latch。
     */
    public static class OrderCreatedListener {

        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile OrderCreatedEvent received;

        /**
         * 监听订单创建事件：记录事件并放行 latch。
         *
         * @param event 收到的订单创建事件
         */
        @MQEventListener(topic = TOPIC, tags = TAG, group = "ddd4j-it")
        public void onOrderCreated(OrderCreatedEvent event) {
            this.received = event;
            latch.countDown();
        }

        /**
         * 获取最近一次收到的事件。
         *
         * @return 事件，未收到时为 {@code null}
         */
        public OrderCreatedEvent getReceived() {
            return received;
        }

        /**
         * 是否已收到指定 orderId 的事件（过滤复用容器中残留的陈旧消息）。
         *
         * @param orderId 期望的订单唯一标识
         * @return 已收到匹配事件返回 {@code true}
         */
        public boolean matches(String orderId) {
            OrderCreatedEvent event = received;
            return event != null && orderId.equals(event.getOrderId());
        }

        /**
         * 阻塞等待首个事件（供不用 {@link #await} 的子类使用）。
         *
         * @param timeoutMillis 最长等待毫秒数
         * @return 等待期间收到事件返回 {@code true}，超时返回 {@code false}
         * @throws InterruptedException 等待被中断时抛出
         */
        public boolean awaitEvent(long timeoutMillis) throws InterruptedException {
            return latch.await(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }
}
