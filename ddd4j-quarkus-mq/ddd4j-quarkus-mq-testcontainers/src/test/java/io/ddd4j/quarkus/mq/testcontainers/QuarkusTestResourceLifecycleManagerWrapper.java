package io.ddd4j.quarkus.mq.testcontainers;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

/**
 * Quarkus test-scope 桥接：把 {@link AbstractTestContainerFixture} 的 start/stop
 * 行为适配到 Quarkus 的 {@link QuarkusTestResourceLifecycleManager}。
 *
 * <p>放在 src/test/java 是因为 {@code QuarkusTestResourceLifecycleManager} 是
 * {@code quarkus-junit} 的 test-scope 类，不能放到 main scope。
 *
 * <p>业务模块的测试用法：
 * <pre>{@code
 * @QuarkusTest
 * @QuarkusTestResource(KafkaQuarkusTestResourceWrapper.class)
 * class KafkaQuarkusIntegrationTest { ... }
 * }</pre>
 *
 * @param <F> 子类 fixture 类型
 */
public class QuarkusTestResourceLifecycleManagerWrapper<F extends AbstractTestContainerFixture>
        implements QuarkusTestResourceLifecycleManager {

    private final F fixture;

    /**
     * 以给定 fixture 构造包装器。
     *
     * @param fixture 容器 fixture 实例
     */
    public QuarkusTestResourceLifecycleManagerWrapper(F fixture) {
        this.fixture = fixture;
    }

    /**
     * 反射无参构造：供 {@code @QuarkusTestResource} 注解实例化，内部经 {@link #createFixture()} 创建 fixture。
     */
    public QuarkusTestResourceLifecycleManagerWrapper() {
        // 反射构造，用于 QuarkusTestResource 注解
        this.fixture = createFixture();
    }

    /**
     * 子类必须重写：返回具体的 fixture 实例。
     * 例如 {@code return new KafkaQuarkusTestResource();}
     */
    protected F createFixture() {
        throw new UnsupportedOperationException(
                "Subclass must override createFixture() to instantiate its fixture");
    }

    @Override
    public Map<String, String> start() {
        return fixture.start();
    }

    @Override
    public void stop() {
        fixture.stop();
    }

    /**
     * 返回当前 fixture 实例。
     *
     * @return 容器 fixture 实例
     */
    public F fixture() {
        return fixture;
    }
}