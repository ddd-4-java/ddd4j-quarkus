package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

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
                "ddd4j.mq.tdmq.service-url", container.getPulsarBrokerUrl(),
                "ddd4j.mq.broker", "TDMQ"
        );
    }
}
