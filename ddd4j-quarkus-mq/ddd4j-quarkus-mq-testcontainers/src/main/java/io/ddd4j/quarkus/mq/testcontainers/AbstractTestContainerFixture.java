package io.ddd4j.quarkus.mq.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

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
 * <p>所有容器默认启用 {@code withReuse(true)}，CI 上多模块复用 Docker 实例，避免
 * testcontainers 拉镜像耗时瓶颈。
 */
public abstract class AbstractTestContainerFixture {

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
     * 容器镜像名（用于 {@code withReuse} 标识）。
     */
    protected abstract DockerImageName dockerImageName();

    /**
     * 启动容器并返回注入到 application.properties 的配置项。
     * 等价于 Quarkus 的 {@code QuarkusTestResourceLifecycleManager#start}，但保持在 main scope。
     */
    public Map<String, String> start() {
        GenericContainer<?> container = container();
        container.withReuse(true);
        if (waitStrategy() != null) {
            container.waitingFor(waitStrategy());
        }
        container.start();
        Map<String, String> props = new HashMap<>(exposedProperties());
        props.put("ddd4j.testcontainers.reuse", "true");
        return props;
    }

    /**
     * 停止容器。
     */
    public void stop() {
        // withReuse(true) 时 testcontainers 不会真正停止容器，留作 CI 全局清理
        // 子类如需强制 stop，可 override
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