package io.ddd4j.quarkus.web;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class Ddd4jQuarkusWebConsumerTest {

    @Test
    void shouldDiscoverMainRepositoryWebFilterAndPropagateTenant() {
        given()
                .header("X-Tenant-Id", "tenant-consumer")
                .when().get("/ddd4j/contract")
                .then()
                .statusCode(200)
                .header("X-Request-Id", not(blankOrNullString()))
                .body("tenantId", equalTo("tenant-consumer"));

        given()
                .when().get("/ddd4j/contract")
                .then()
                .statusCode(200)
                .body("tenantId", nullValue());
    }

    @Test
    void shouldClearRequestStateBeforePostResponseProbeRuns() {
        Response response = given()
                .header("X-Tenant-Id", "tenant-lifecycle")
                .header("X-Request-Id", "request-lifecycle")
                .header("Authorization", "Bearer lifecycle-token")
                .when().get("/ddd4j/contract")
                .then()
                .statusCode(200)
                .body("tenantId", equalTo("tenant-lifecycle"))
                .extract().response();

        assertThat(response.header("X-Ddd4j-Test-Post-Response-Thread"),
                not(blankOrNullString()));
        assertThat(response.path("serviceThread"),
                equalTo(response.header("X-Ddd4j-Test-Post-Response-Thread")));
        assertThat(response.header("X-Ddd4j-Test-Post-Response-Tenant-Cleared"),
                equalTo("true"));
        assertThat(response.header("X-Ddd4j-Test-Post-Response-Request-Id-Cleared"),
                equalTo("true"));
        assertThat(response.header("X-Ddd4j-Test-Post-Response-Authorization-Cleared"),
                equalTo("true"));
    }
}
