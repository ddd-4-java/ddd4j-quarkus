package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

/**
 * Aliyun ONS testcontainers fixture for Quarkus tests.
 *
 * <p>ONS 客户端协议基于 RocketMQ，因此复用 {@code apache/rocketmq:5.3.0} 镜像
 * （namesrv 9876），以本地 RocketMQ 充当 ONS 协议端点。
 * 暴露属性：{@code ddd4j.mq.ons.endpoint}（namesrv 地址 {@code host:9876}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class OnsQuarkusTestResource extends AbstractTestContainerFixture {

    private static final DockerImageName IMAGE = DockerImageName.parse("apache/rocketmq:5.3.0");
    private static final int NAMESRV_PORT = 9876;

    private GenericContainer<?> container;

    @Override
    protected GenericContainer<?> container() {
        container = new GenericContainer<>(IMAGE)
                .withExposedPorts(NAMESRV_PORT)
                // 镜像 Cmd 为占位符 "dummy"（entrypoint 会 exec dummy 而 exit 127），
                // 需显式指定启动命令：以 Name Server 模式启动（WorkingDir 为 bin 目录）
                .withCommand("sh", "mqnamesrv");
        return container;
    }

    @Override
    protected org.testcontainers.containers.wait.strategy.WaitStrategy waitStrategy() {
        return Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2));
    }

    @Override
    protected DockerImageName dockerImageName() {
        return IMAGE;
    }

    @Override
    protected Map<String, String> exposedProperties() {
        return Map.of(
                "ddd4j.mq.ons.endpoint", hostPort(container, NAMESRV_PORT),
                "ddd4j.mq.broker", "ONS"
        );
    }
}
