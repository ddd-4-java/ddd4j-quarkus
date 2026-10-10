package io.ddd4j.quarkus.mq.rocket;

import io.ddd4j.core.context.BaseContext;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.rocketmq.RocketMQClient;
import io.ddd4j.mq.rocketmq.RocketMQProperties;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RocketMQ broker 客户端级端到端集成测试（Testcontainers，纯 JUnit 手动装配）。
 *
 * <p>覆盖：真实 RocketMQ 上的 {@code MQClient#init} → {@code MQEvent.publish()} →
 * broker 路由 → {@code @MQEventListener} 监听器 → JSON 反序列化全链路，并断言
 * {@code init} 把生产者注册到 {@link BaseContext}（key = {@link MQClient#impl()}）的
 * 装配契约。无 Docker 环境时自动跳过（{@code disabledWithoutDocker = true}），不伪报通过。
 *
 * <p>与 {@link RocketMqQuarkusIntegrationTest}（{@code @QuarkusTest} + Arc 装配）互补：
 * 本测试不启动 Quarkus runtime，直接构造 {@link RocketMQProperties} / {@link RocketMQClient}
 * 手动 {@code init}，对齐 ddd4j-boot 同款客户端级集成测试（照抄其已验证的容器方案）。
 *
 * <p>容器方案三个关键点（官方镜像不提供开箱即用的单容器组合，照抄 boot 已验证方案）：
 * <ul>
 *   <li><b>单容器双进程</b>：{@code sh mqnamesrv & sleep 10 && sh mqbroker ...}，同容器内
 *       以 localhost 互联；官方 {@code apache/rocketmq} 镜像没有 Testcontainers 专用模块</li>
 *   <li><b>brokerIP1 + 固定端口对</b>：broker 向 namesrv 公告 {@code 127.0.0.1:固定端口}，
 *       客户端（宿主机）经 namesrv 拿到该地址直连 broker，故必须固定映射 broker 端口与
 *       VIP 通道端口（端口 - 2）；端口对从 20011 起步进选择，并用跨 JVM 文件锁串行化，
 *       可用 {@code -Dddd4j.test.rocketmq.brokerPort=} 覆盖</li>
 *   <li><b>topic 预创建</b>：上游 client 按 partition key 走 {@code MessageQueueSelector}
 *       发送路径，该路径没有 TBW102 自动建 topic 兜底，故 {@link #beforeAll} 用
 *       {@code mqadmin} 预创建 topic（与生产一致：topic 由运维预创建）</li>
 * </ul>
 *
 * <p>broker 配置文件拷入容器必须用 {@link MountableFile#forHostPath(Path, int)} 强制
 * tar mode=0644：Windows（NTFS）没有 posix:permissions 视图，不能用
 * {@code Files.setPosixFilePermissions} 放宽本地权限。broker 默认堆 2g，测试用
 * {@code JAVA_OPT_EXT} 压到 512m。
 */
@Testcontainers(disabledWithoutDocker = true)
class RocketMqContainerClientIntegrationTest {

    /**
     * 跨 JVM 串行 RocketMQ 端口选择与容器生命周期，避免并行构建选中同一端口对
     * （锁文件与 ddd4j-boot 同款测试共用，跨仓库并行构建同样串行）。
     */
    private static final FileChannel PORT_LOCK_CHANNEL = openPortLockChannel();
    private static final FileLock PORT_LOCK = acquirePortLock();

    /**
     * broker 对外公告端口，每次测试动态选择可用的 broker/VIP 端口对。
     */
    private static final int BROKER_PORT = Integer.getInteger(
            "ddd4j.test.rocketmq.brokerPort", availableBrokerPort());

    /**
     * 覆盖镜像自带 broker.conf 的完整配置（含公告地址/namesrv/自动建 topic）。
     */
    private static final String BROKER_CONF_TEMPLATE = """
            brokerClusterName = DefaultCluster
            brokerName = broker-a
            brokerId = 0
            deleteWhen = 04
            fileReservedTime = 48
            brokerRole = ASYNC_MASTER
            flushDiskType = ASYNC_FLUSH
            brokerIP1 = 127.0.0.1
            listenPort = %d
            namesrvAddr = 127.0.0.1:9876
            autoCreateTopicEnable = true
            """;

    /** round-trip 断言用 topic/tag（与 mqadmin 预创建的 topic 一致）。 */
    private static final String TOPIC = "order";
    private static final String TAG = "created";

    /** namesrv 的宿主机映射地址（{@link #beforeAll} 从容器读取）。 */
    private static String nameServerAddress;

    @Container
    static final GenericContainer<?> ROCKETMQ = rocketmqContainer();

    private static GenericContainer<?> rocketmqContainer() {
        try {
            // 临时文件默认 0600，拷入容器后属主为 root，而镜像内 broker 以 rocketmq(uid 3000) 用户
            // 运行无法读取，必须落盘为 0644。不能用 Files.setPosixFilePermissions 放宽本地权限：
            // Windows（NTFS 默认文件系统）没有 posix:permissions 视图，该调用会在类静态初始化期
            // 抛 UnsupportedOperationException 导致整个测试类 ExceptionInInitializerError；
            // 改用 MountableFile 强制 tar mode=0644（跨平台等价于 rw-r--r--，不依赖宿主机文件系统）
            Path brokerConf = Files.createTempFile("ddd4j-rocketmq-broker", ".conf");
            Files.writeString(brokerConf, String.format(BROKER_CONF_TEMPLATE, BROKER_PORT));
            GenericContainer<?> container = new LockedRocketMQContainer(
                    // 测试镜像来源: https://hub.docker.com/r/apache/rocketmq
                    // （Testcontainers 官方模块索引 https://testcontainers.com/modules/ 暂无 RocketMQ 模块）
                    DockerImageName.parse("apache/rocketmq:5.3.2"))
                    .withEnv("JAVA_OPT_EXT", "-Xms512m -Xmx512m -Xmn256m")
                    .withCommand("sh", "-c",
                            "sh mqnamesrv & sleep 10 && sh mqbroker -n 127.0.0.1:9876"
                                    + " -c /home/rocketmq/rocketmq-5.3.2/conf/broker.conf")
                    .withCopyFileToContainer(MountableFile.forHostPath(brokerConf, 0644),
                            "/home/rocketmq/rocketmq-5.3.2/conf/broker.conf")
                    .withExposedPorts(9876)
                    .waitingFor(Wait.forLogMessage(".*boot success.*", 2));
            container.setPortBindings(List.of(
                    BROKER_PORT + ":" + BROKER_PORT,
                    (BROKER_PORT - 2) + ":" + (BROKER_PORT - 2)));
            return container;
        } catch (IOException e) {
            releasePortLockResources();
            throw new IllegalStateException("Prepare RocketMQ broker.conf failed", e);
        } catch (RuntimeException e) {
            releasePortLockResources();
            throw e;
        }
    }

    private static int availableBrokerPort() {
        for (int candidate = 20011; candidate < 30000; candidate += 10) {
            try (ServerSocket broker = new ServerSocket(candidate);
                 ServerSocket vip = new ServerSocket(candidate - 2)) {
                return candidate;
            } catch (IOException ignored) {
                // 试下一组端口对。
            }
        }
        releasePortLockResources();
        throw new IllegalStateException("No available RocketMQ broker/VIP port pair");
    }

    private static FileChannel openPortLockChannel() {
        try {
            return FileChannel.open(Path.of(System.getProperty("java.io.tmpdir"), "ddd4j-rocketmq-test.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("Open RocketMQ test port lock failed", e);
        }
    }

    private static FileLock acquirePortLock() {
        long deadline = System.nanoTime() + Duration.ofMinutes(2).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                FileLock lock = PORT_LOCK_CHANNEL.tryLock();
                if (Objects.nonNull(lock)) {
                    Runtime.getRuntime().addShutdownHook(new Thread(
                            RocketMqContainerClientIntegrationTest::releasePortLockResources));
                    return lock;
                }
            } catch (OverlappingFileLockException ignored) {
                // 同 JVM 内已有 RocketMQ 测试持有锁。
            } catch (IOException e) {
                releasePortLockResources();
                throw new IllegalStateException("Acquire RocketMQ test port lock failed", e);
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                releasePortLockResources();
                throw new IllegalStateException("Interrupted while acquiring RocketMQ test port lock", e);
            }
        }
        releasePortLockResources();
        throw new IllegalStateException("Acquire RocketMQ test port lock timed out after PT2M");
    }

    @AfterAll
    static void releasePortLock() throws IOException {
        try {
            ROCKETMQ.stop();
        } finally {
            releasePortLockResources();
        }
    }

    private static void releasePortLockResources() {
        try {
            if (Objects.nonNull(PORT_LOCK) && PORT_LOCK.isValid()) {
                PORT_LOCK.release();
            }
            if (PORT_LOCK_CHANNEL.isOpen()) {
                PORT_LOCK_CHANNEL.close();
            }
        } catch (IOException ignored) {
            // 尽力清理，同样会从 JVM shutdown hook 触发。
        }
    }

    /** 启动失败时同步释放端口锁，避免后续测试在锁上空等。 */
    private static final class LockedRocketMQContainer extends GenericContainer<LockedRocketMQContainer> {

        private LockedRocketMQContainer(DockerImageName imageName) {
            super(imageName);
        }

        @Override
        public void start() {
            try {
                super.start();
            } catch (RuntimeException e) {
                releasePortLockResources();
                throw e;
            }
        }
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        nameServerAddress = "127.0.0.1:" + ROCKETMQ.getMappedPort(9876);
        // 与生产一致：topic 由运维预创建（selector 发送路径没有自动建 topic 兜底）。
        // broker "boot success" 日志先于 namesrv 注册完成打印，mqadmin 未找到集群时
        // 打印 [error] 但退出码仍为 0，故必须按输出轮询直到建成功。
        String output = "";
        for (int i = 0; i < 20; i++) {
            org.testcontainers.containers.Container.ExecResult result =
                    ROCKETMQ.execInContainer("sh", "mqadmin", "updateTopic",
                            "-n", "127.0.0.1:9876", "-c", "DefaultCluster", "-t", TOPIC);
            output = result.getStdout();
            if (output.contains("success")) {
                break;
            }
            Thread.sleep(1000);
        }
        assertThat(output).contains("success");
    }

    @Test
    void shouldPublishAndConsumeEventThroughRealRocketMQContainer() throws Exception {
        // 手动装配（对齐 boot 客户端级测试的装配契约）：假定属性已绑定，broker 必须与 impl() 大小写一致
        // （MQClient#init 的守卫是 Objects.equals(properties.getBroker(), impl())，大小写敏感）。
        RocketMQProperties properties = new RocketMQProperties();
        properties.setEnabled(true);
        properties.setBroker("rocket");
        properties.setNameServer(nameServerAddress);
        properties.setProducerGroup("ddd4j-it-producer");

        OrderCreatedListener listenerBean = new OrderCreatedListener();
        MQListener listener = listenerDefinition(listenerBean);
        RocketMQClient client = new RocketMQClient(properties);
        try {
            client.init(List.of(listener), properties, new JsonMQEventSerialization(), null);

            // 装配契约：init 后生产者必须已注册到 BaseContext（key = impl()），否则 publish 只会 warn 后静默丢弃
            assertThat(client.impl()).isEqualTo("rocket");
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            assertThat(publishers).as("init 应把 RocketMQ 生产者注册到 BaseContext").isNotNull();
            assertThat(publishers).containsKey("rocket");

            // 订阅异步生效，先稳定等待再发布，避免 publish 早于 subscribe（对齐骨架 settle 约定）
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
     * 由测试在 init 时以 {@link MQListener#of} 注册）。group 必须非空：
     * {@code DefaultMQPushConsumer} 对空 group 直接拒绝。
     */
    public static class OrderCreatedListener {

        private final List<OrderCreatedEvent> received = new CopyOnWriteArrayList<>();

        @MQEventListener(topic = TOPIC, tags = TAG, group = "ddd4j-it")
        public void onOrderCreated(OrderCreatedEvent event) {
            received.add(event);
        }

        /** 是否已收到指定 orderId 的事件（过滤复用 broker 中残留的陈旧消息）。 */
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
