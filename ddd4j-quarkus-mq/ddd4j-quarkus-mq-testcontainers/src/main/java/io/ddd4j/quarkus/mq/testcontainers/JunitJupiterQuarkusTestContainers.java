package io.ddd4j.quarkus.mq.testcontainers;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Quarkus 集成测试一键注解：组合 {@code @ExtendWith(Ddd4jQuarkusTestContainersExtension)}，
 * Docker daemon 不可达时整个测试类优雅跳过（含 {@code @QuarkusTestResource} 容器启动——
 * ExecutionCondition 先于 {@code QuarkusTestExtension#beforeAll} 求值）。
 *
 * <p>对齐 ddd4j-javalin {@code JunitJupiterTestContainers} 的 Quarkus 版语义。
 * 用法（与 {@code @QuarkusTest}、{@code @QuarkusTestResource} 同用）：
 * <pre>{@code
 * @QuarkusTest
 * @QuarkusTestResource(RabbitMqQuarkusIntegrationTest.RabbitMqTestResource.class)
 * @JunitJupiterQuarkusTestContainers
 * class RabbitMqQuarkusIntegrationTest extends AbstractMqQuarkusIntegrationTest<RabbitMQProperties> { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(Ddd4jQuarkusTestContainersExtension.class)
public @interface JunitJupiterQuarkusTestContainers {
}
