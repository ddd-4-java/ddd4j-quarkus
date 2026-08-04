package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * Disruptor testcontainers fixture for Quarkus tests.
 *
 * <p>Disruptor 是进程内 LMAX 环形队列，无需任何容器：{@link #start()} 不启动容器，
 * 仅返回 broker 标识配置 {@code ddd4j.mq.broker=DISRUPTOR}（无容器相关信息，可视为空 Map）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class DisruptorQuarkusTestResource extends AbstractTestContainerFixture {

    @Override
    protected GenericContainer<?> container() {
        return null;
    }

    @Override
    protected Map<String, String> exposedProperties() {
        return Map.of("ddd4j.mq.broker", "DISRUPTOR");
    }

    @Override
    protected DockerImageName dockerImageName() {
        return null;
    }

    /**
     * 无容器：直接返回 broker 标识配置，不触发任何 Docker 操作。
     */
    @Override
    public Map<String, String> start() {
        return exposedProperties();
    }
}
