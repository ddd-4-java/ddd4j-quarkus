package io.ddd4j.quarkus.web;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
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
}
