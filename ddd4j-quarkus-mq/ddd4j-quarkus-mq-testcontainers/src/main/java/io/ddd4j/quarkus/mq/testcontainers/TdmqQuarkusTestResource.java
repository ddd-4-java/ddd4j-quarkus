package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

/**
 * Tencent Cloud TDMQ testcontainers fixture for Quarkus tests.
 *
 * <p>TDMQ 客户端协议兼容 Pulsar，因此复用 {@code apachepulsar/pulsar:3.2.0} 镜像
 * （standalone，broker 端口 6650），以本地 Pulsar 充当 TDMQ 协议端点。
 * 暴露属性：{@code ddd4j.mq.tdmq.service-url}（{@code pulsar://host:6650}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class TdmqQuarkusTestResource extends AbstractTestContainerFixture {

    private static final DockerImageName IMAGE = DockerImageName.parse("apachepulsar/pulsar:3.2.0");
    private static final int BROKER_PORT = 6650;

    private GenericContainer<?> container;

    @Override
    protected GenericContainer<?> container() {
        container = new GenericContainer<>(IMAGE)
                .withExposedPorts(BROKER_PORT)
                // 镜像 Cmd 为 /bin/bash（无入口命令，容器会立即退出），
                // 需显式指定 standalone 模式启动（WorkingDir 为 /pulsar）
                .withCommand("bin/pulsar", "standalone");
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
                "ddd4j.mq.tdmq.service-url", String.format("pulsar://%s:%s",
                        container.getHost(), firstMappedPort(container, BROKER_PORT)),
                "ddd4j.mq.broker", "TDMQ"
        );
    }
}
