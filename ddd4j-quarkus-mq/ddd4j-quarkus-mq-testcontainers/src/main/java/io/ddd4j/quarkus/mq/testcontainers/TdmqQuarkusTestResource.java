package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.pulsar.PulsarContainer;
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

    // 测试镜像来源: https://testcontainers.com/modules/pulsar/
    // 已钉版 3.2.0（本任务前即为固定标签）；本任务改用官方第一方容器 PulsarContainer
    // 替代 GenericContainer（PulsarContainer 内部即以 standalone 启动并探活 broker，
    // 与原 withCommand("bin/pulsar","standalone") + health 探测等价）
    private static final DockerImageName IMAGE = DockerImageName.parse("apachepulsar/pulsar:3.2.0");
    private static final int BROKER_PORT = 6650;

    private PulsarContainer container;

    @Override
    protected GenericContainer<?> container() {
        // 官方第一方容器：PulsarContainer 内部以 standalone 模式启动并探活 broker，
        // 取代原 GenericContainer + withCommand("bin/pulsar","standalone") 手工拼装
        container = new PulsarContainer(IMAGE);
        return container;
    }

    @Override
    protected org.testcontainers.containers.wait.strategy.WaitStrategy waitStrategy() {
        return Wait.forHttp("/admin/v2/brokers/health").forPort(8080).forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(3));
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
