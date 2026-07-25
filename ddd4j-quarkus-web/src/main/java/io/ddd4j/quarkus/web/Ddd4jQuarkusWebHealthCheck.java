package io.ddd4j.quarkus.web;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * ddd4j Quarkus Web 健康指示器，对齐 ddd4j-boot 中的 {@code ddd4jWebHealthIndicator}。
 *
 * <p>Quarkus 端通过 SmallRye Health 暴露，访问 {@code /q/health/ready} 时返回 UP 状态，
 * 并附带 {@code runtime=quarkus-rest} 标识，便于运维区分运行时栈。
 *
 * <p>注意：本检查仅验证 Web 集成层可用，不探测底层业务接口（数据库、Redis、MQ 等）
 * ——这些由 ddd4j 对应启动器自带 HealthCheck 提供。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Readiness
@ApplicationScoped
public class Ddd4jQuarkusWebHealthCheck implements HealthCheck {

    private static final String NAME = "ddd4j-quarkus-web";

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named(NAME)
                .up()
                .withData("runtime", "quarkus-rest")
                .build();
    }
}