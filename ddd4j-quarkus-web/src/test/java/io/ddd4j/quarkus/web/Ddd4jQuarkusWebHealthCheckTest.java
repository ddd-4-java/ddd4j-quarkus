package io.ddd4j.quarkus.web;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.Status;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Quarkus 集成测试：验证 {@link Ddd4jQuarkusWebHealthCheck} 正确暴露到
 * SmallRye Health 的 {@code /q/health/ready} 端点。
 *
 * <p>对齐 ddd4j-boot 中 {@code Ddd4jWebMvcAutoConfigurationTest.shouldBackOffOutsideServletApplication}
 * 的验证模式：用 {@code @QuarkusTest} 启动最小运行时，断言健康端点可访问。
 */
@QuarkusTest
class Ddd4jQuarkusWebHealthCheckTest {

    @Inject
    Ddd4jQuarkusWebHealthCheck healthCheck;

    @Test
    void shouldReportUpWithQuarkusRestRuntime() {
        HealthCheckResponse response = healthCheck.call();
        assertThat(response.getName()).isEqualTo("ddd4j-quarkus-web");
        assertThat(response.getStatus()).isEqualTo(Status.UP);
        assertThat(response.getData().orElseThrow()).containsEntry("runtime", "quarkus-rest");
    }

    @Test
    void shouldExposeReadyEndpoint() {
        given()
            .when().get("/q/health/ready")
            .then()
            .statusCode(200);
    }
}