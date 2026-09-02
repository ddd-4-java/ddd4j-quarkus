package io.ddd4j.quarkus.mq.testcontainers;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * JUnit 5 {@link ExecutionCondition}：Docker 守护不可达时禁用测试类（优雅跳过）。
 *
 * <p>对齐 ddd4j-javalin {@code Ddd4jTestContainersExtension} 的语义，但探测方式更严格：
 * 不是只看 {@code DOCKER_HOST} 环境变量格式，而是真正探测一次 daemon 可达性，结果按 JVM
 * 缓存（探测有副作用与耗时，无理由重复）。
 *
 * <p><b>探测实现</b>：优先执行 {@code docker info} 子进程（10s 超时，exit 0 = 可达）。
 * 不用 {@code DockerClientFactory#isDockerAvailable()} 做首选探测的原因：它依赖
 * {@code ServiceLoader} 发现 {@code DockerClientProviderStrategy}，在 Quarkus 测试的
 * 多层 classloader（surefire launcher 层 vs QuarkusClassLoader）下会抛
 * {@code ServiceConfigurationError: ... not a subtype}（策略接口与实现被不同 loader 加载），
 * 而 {@code @QuarkusTestResource} 启动容器时（Quarkus loader 内）同样的探测是正常的。
 * docker CLI 不存在时回落 {@code DockerClientFactory}（此时通常单一层级 classloader，可正常工作）。
 *
 * <p><b>Quarkus 关键语义</b>：JUnit 5 的 ExecutionCondition 在所有扩展的
 * {@code BeforeAllCallback} 之前求值，而 {@code @QuarkusTest} 的
 * {@code QuarkusTestResource} 容器正是在 {@code QuarkusTestExtension#beforeAll} 中启动——
 * 因此本条件 disable 测试类时，{@code @QuarkusTestResource} 容器根本不会启动，
 * 无 Docker 环境下不会以容器启动失败的形式误报测试失败。
 *
 * <p>配合 {@link JunitJupiterQuarkusTestContainers} 注解使用。
 */
public class Ddd4jQuarkusTestContainersExtension implements ExecutionCondition {

    private static final String DISABLE_REASON = "Docker daemon not reachable";

    /** JVM 级缓存：探测有副作用与耗时，一次即可。 */
    private static volatile Boolean dockerAvailable;

    /** 探测失败时的根因（用于 disabled reason 排障）。 */
    private static volatile String failureMessage;

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (dockerAvailable()) {
            return ConditionEvaluationResult.enabled("Docker daemon reachable");
        }
        return ConditionEvaluationResult.disabled(DISABLE_REASON
                + (failureMessage == null ? "" : ": " + failureMessage));
    }

    /**
     * 探测 Docker daemon 是否可达（{@code docker info} 语义），结果按 JVM 缓存。
     */
    static boolean dockerAvailable() {
        Boolean cached = dockerAvailable;
        if (cached != null) {
            return cached;
        }
        synchronized (Ddd4jQuarkusTestContainersExtension.class) {
            if (dockerAvailable == null) {
                dockerAvailable = probe();
            }
            return dockerAvailable;
        }
    }

    private static boolean probe() {
        // 1) docker info 子进程（对齐任务语义；CLI 自身尊重 DOCKER_HOST/context）
        Process process = null;
        try {
            process = new ProcessBuilder("docker", "info").redirectErrorStream(true).start();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                failureMessage = "docker info timed out after 10s";
                return false;
            }
            if (process.exitValue() == 0) {
                return true;
            }
            failureMessage = "docker info exited non-zero";
            return false;
        } catch (IOException cliMissing) {
            // 2) docker CLI 不存在：回落 testcontainers 探测（无 CLI 但配置了 DOCKER_HOST 的环境）
            try {
                DockerClientFactory.instance().client();
                return true;
            } catch (Throwable throwable) {
                Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                failureMessage = throwable.getClass().getSimpleName() + ": " + throwable.getMessage()
                        + " / " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
                return false;
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            failureMessage = "docker info interrupted";
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
