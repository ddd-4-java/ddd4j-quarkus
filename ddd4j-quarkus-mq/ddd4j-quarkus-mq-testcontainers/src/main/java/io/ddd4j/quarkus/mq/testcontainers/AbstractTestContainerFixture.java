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
 * <p>Quarkus 集成（{@code QuarkusTestResourceLifecycleManager}）放在
 * {@code QuarkusTestResourceLifecycleManagerWrapper}
 * 中（test scope），让本 fixture 可以在 main scope 使用，便于其他模块依赖本工具类。
 *
 * <p>夹具拥有自己启动的容器，负责幂等启动、停止与部分启动失败后的回收；不强制启用复用。
 */
public abstract class AbstractTestContainerFixture {

    private GenericContainer<?> runningContainer;
    private Map<String, String> runningProperties;

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
    public synchronized Map<String, String> start() {
        if (Objects.nonNull(runningProperties)) {
            return runningProperties;
        }
        // 上一次清理失败时保留所有权，必须先回收旧资源才能创建下一只容器。
        if (Objects.nonNull(runningContainer)) {
            stop();
        }
        runningContainer = Objects.requireNonNull(container(), "fixture container");
        try {
            WaitStrategy strategy = waitStrategy();
            if (Objects.nonNull(strategy)) {
                runningContainer.waitingFor(strategy);
            }
            runningContainer.start();
            runningProperties = Map.copyOf(exposedProperties());
            return runningProperties;
        } catch (RuntimeException | Error failure) {
            // start 或配置提取失败均可能留下 Docker 资源，保留原始失败并附带清理失败。
            try {
                stop();
            } catch (RuntimeException | Error cleanupFailure) {
                if (cleanupFailure != failure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }

    /**
     * 停止容器。
     */
    public synchronized void stop() {
        runningProperties = null;
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
        return Objects.isNull(mapped) ? null : String.valueOf(mapped);
    }

    /**
     * 实用方法：把容器 host:port 组合为字符串。
     */
    protected String hostPort(GenericContainer<?> container, int internalPort) {
        return container.getHost() + ":" + firstMappedPort(container, internalPort);
    }
}
