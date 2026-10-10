package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * Pulsar testcontainers fixture for Quarkus tests.
 *
 * <p>镜像：{@code apachepulsar/pulsar:3.2.0}（默认 entrypoint 以 standalone 模式启动，
 * 内置 broker + bookkeeper）。暴露 broker 端口 6650（Binary 协议）与 8080（HTTP admin）。
 * 暴露属性：{@code ddd4j.mq.pulsar.service-url}（{@code pulsar://host:6650}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class PulsarQuarkusTestResource extends AbstractTestContainerFixture {

    // 测试镜像来源: https://testcontainers.com/modules/pulsar/
    // 已钉版 3.2.0（本任务前即为固定标签；官方第一方容器 PulsarContainer）
    private static final DockerImageName IMAGE = DockerImageName.parse("apachepulsar/pulsar:3.2.0");
    private PulsarContainer container;

    @Override
    protected GenericContainer<?> container() {
        container = new PulsarContainer(IMAGE);
        return container;
    }

    @Override
    protected DockerImageName dockerImageName() {
        return IMAGE;
    }

    @Override
    protected Map<String, String> exposedProperties() {
        return Map.of(
                "ddd4j.mq.pulsar.service-url", container.getPulsarBrokerUrl(),
                "ddd4j.mq.broker", "PULSAR"
        );
    }
}
