package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.Map;

/**
 * RocketMQ testcontainers fixture for Quarkus tests.
 *
 * <p>镜像：{@code apache/rocketmq:5.3.2}。5.3.x 镜像 entrypoint 单进程启动，RocketMQ 5.x
 * 要求 namesrv 与 broker 两个进程，故覆盖命令为单容器双进程模式（对齐
 * ddd4j-javalin {@code RocketMqTestContainerFixture} 先例）：
 * <ul>
 *   <li>挂载 {@code rocketmq/broker.conf}（{@code brokerIP1=127.0.0.1} + 自动建 topic +
 *       自动建订阅组），broker 以 {@code 127.0.0.1:9876} 注册 namesrv</li>
 *   <li>broker 10911 通过 {@link FixedHostPortGenericContainer} 绑定宿主机固定端口——
 *       客户端（宿主机 JVM）经 namesrv 拿到 {@code 127.0.0.1:10911} 后直连 broker，
 *       必须有宿主机端口映射；10909 为 proxy VIP 端口（无 proxy 不监听）不暴露，
 *       否则 {@code Wait.forListeningPort()} 永远超时</li>
 *   <li>{@code JAVA_OPT_EXT} 收紧 namesrv+broker JVM 堆，避免默认大堆被 OOMKill</li>
 * </ul>
 * 暴露 namesrv 端口 9876（随机映射），供客户端 {@code nameServer} 接入。
 * 暴露属性：{@code ddd4j.mq.rocketmq.namesrv-addr}。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class RocketMqQuarkusTestResource extends AbstractTestContainerFixture {

    private static final DockerImageName IMAGE = DockerImageName.parse("apache/rocketmq:5.3.2");
    private static final String IMAGE_NAME = "apache/rocketmq:5.3.2";
    private static final int NAMESRV_PORT = 9876;
    private static final int BROKER_PORT = 10911;
    private static final String BROKER_CONF = "/home/rocketmq/rocketmq-5.3.2/conf/broker.conf";

    private GenericContainer<?> container;

    @Override
    protected GenericContainer<?> container() {
        container = new FixedHostPortGenericContainer<>(IMAGE_NAME)
                // broker 向 namesrv 公告 127.0.0.1:10911，客户端直连宿主机固定映射端口
                .withFixedExposedPort(BROKER_PORT, BROKER_PORT)
                .withExposedPorts(NAMESRV_PORT)
                .withEnv("JAVA_OPT_EXT", "-Xmx512m -Xms512m -Xmn128m")
                .withCopyFileToContainer(MountableFile.forClasspathResource("rocketmq/broker.conf"), BROKER_CONF)
                // namesrv 后台启动，broker 稍后注册（等待 namesrv 就绪），wait 保持前台
                .withCommand("sh", "-c",
                        "sh mqnamesrv & sleep 10; sh mqbroker -n 127.0.0.1:9876 -c " + BROKER_CONF + " & wait");
        return container;
    }

    @Override
    protected org.testcontainers.containers.wait.strategy.WaitStrategy waitStrategy() {
        // QEMU/arm64 首次拉起 broker 可能较慢，放宽到 3 分钟。
        // 不能只等端口：9876（namesrv）先于 broker 启动即监听，须等 broker boot success
        // 日志，客户端 route 查询才能拿到 broker（注册约在 boot 后数秒内完成）
        return Wait.forLogMessage(".*The broker\\[.*boot success.*", 1)
                .withStartupTimeout(Duration.ofMinutes(3));
    }

    @Override
    protected DockerImageName dockerImageName() {
        return IMAGE;
    }

    @Override
    protected Map<String, String> exposedProperties() {
        return Map.of(
                "ddd4j.mq.rocketmq.namesrv-addr", hostPort(container, NAMESRV_PORT),
                "ddd4j.mq.broker", "ROCKET"
        );
    }
}
