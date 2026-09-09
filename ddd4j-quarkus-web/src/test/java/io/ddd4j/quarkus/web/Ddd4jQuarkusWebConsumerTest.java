package io.ddd4j.quarkus.web;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证已发布 ddd4j 2.0.x Quarkus Web 适配器在本仓消费者中的请求上下文契约。
 */
@QuarkusTest
class Ddd4jQuarkusWebConsumerTest {

    @Test
    void shouldPropagateTenantGenerateRequestIdAndNotLeakTenantIntoNextRequest() {
        Response tenantResponse = given()
                .header("X-Tenant-Id", "tenant-consumer")
                .when()
                .get("/ddd4j/contract")
                .then()
                .statusCode(200)
                .extract()
                .response();

        assertThat(tenantResponse.getHeader("X-Request-Id")).isNotBlank();
        assertThat(tenantResponse.jsonPath().getString("tenantId")).isEqualTo("tenant-consumer");

        Response noTenantResponse = given()
                .when()
                .get("/ddd4j/contract")
                .then()
                .statusCode(200)
                .extract()
                .response();

        Object tenantId = noTenantResponse.jsonPath().get("tenantId");
        assertThat(tenantId).isNull();
    }

    @Test
    void shouldClearRequestContextOnTheResourceServiceThreadAfterResponse() {
        Response response = given()
                .header("X-Tenant-Id", "tenant-cleanup")
                .header("X-Request-Id", "request-cleanup")
                .header("Authorization", "Bearer cleanup-contract")
                .when()
                .get("/ddd4j/contract")
                .then()
                .statusCode(200)
                .extract()
                .response();

        assertThat(response.jsonPath().getString("serviceThread"))
                .isEqualTo(response.getHeader("X-Ddd4j-Test-Post-Response-Service-Thread"));
        assertThat(response.getHeader("X-Ddd4j-Test-Post-Response-Tenant-Cleared")).isEqualTo("true");
        assertThat(response.getHeader("X-Ddd4j-Test-Post-Response-Request-Id-Cleared")).isEqualTo("true");
        assertThat(response.getHeader("X-Ddd4j-Test-Post-Response-Authorization-Cleared")).isEqualTo("true");
    }
}
