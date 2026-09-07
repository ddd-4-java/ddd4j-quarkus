package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.Objects;

/**
 * ddd4j-quarkus 共享 testcontainers fixture 基类（testcontainers-only 形态）。
 *
 * <p>对齐 ddd4j-javalin-testcontainers 中
 * {@code AbstractTestContainerFixture} 的契约：提供 start/stop 生命周期，并允许子类
 * 暴露容器端口/凭据。
 *
 * <p>Quarkus 集成（{@code QuarkusTestResourceLifecycleManager}）放在 {@link QuarkusTestResourceLifecycleManagerWrapper}
 * 中（test scope），让本 fixture 可以在 main scope 使用，便于其他模块依赖本工具类。
 *
 * <p>fixture 拥有并关闭自己启动的容器。是否在本地启用实验性的容器复用由开发者环境决定，
 * 本基类不会强制修改复用策略。
 */
public abstract class AbstractTestContainerFixture {

    private GenericContainer<?> runningContainer;

    /**
     * 子类必须返回具体的容器实例。
     */
    protected abstract GenericContainer<?> container();

    /**
     * 子类必须返回容器启动后注入到 application.properties 的配置项。
     * 例如 Kafka fixture 返回 {@code {"ddd4j.mq.kafka.bootstrap-servers", "PLAINTEXT://..."}}。
     */
    protected abstract Map<String, String> exposedProperties();

    /**
     * 子类可选提供等待策略（默认 {@link WaitStrategy} 由 container 自身决定）。
     */
    protected WaitStrategy waitStrategy() {
        return null;
    }

    /**
     * 容器镜像名。
     */
    protected abstract DockerImageName dockerImageName();

    /**
     * 启动容器并返回注入到 application.properties 的配置项。
     * 等价于 Quarkus 的 {@code QuarkusTestResourceLifecycleManager#start}，但保持在 main scope。
     */
    public Map<String, String> start() {
        runningContainer = container();
        WaitStrategy strategy = waitStrategy();
        if (Objects.nonNull(strategy)) {
            runningContainer.waitingFor(strategy);
        }
        runningContainer.start();
        return Map.copyOf(exposedProperties());
    }

    /**
     * 停止容器。
     */
    public void stop() {
        if (Objects.nonNull(runningContainer)) {
            runningContainer.stop();
            runningContainer = null;
        }
    }

    /**
     * 实用方法：把容器第一个映射端口转为字符串。
     */
    protected String firstMappedPort(GenericContainer<?> container, int internalPort) {
        Integer mapped = container.getMappedPort(internalPort);
        return mapped == null ? null : String.valueOf(mapped);
    }

    /**
     * 实用方法：把容器 host:port 组合为字符串。
     */
    protected String hostPort(GenericContainer<?> container, int internalPort) {
        return container.getHost() + ":" + firstMappedPort(container, internalPort);
    }
}
