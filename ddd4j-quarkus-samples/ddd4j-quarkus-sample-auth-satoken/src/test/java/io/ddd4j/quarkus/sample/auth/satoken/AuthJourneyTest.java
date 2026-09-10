package io.ddd4j.quarkus.sample.auth.satoken;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 * 验证真实 HTTP 登录、跨请求会话恢复、授权与注销，防止只启动成功却无法使用的示例。
 */
@QuarkusTest
class AuthJourneyTest {

    @Test
    void loginShouldRestoreOnlyItsOwnSessionAndLogoutShouldRevokeToken() {
        given().get("/auth/status").then().statusCode(200).body("login", equalTo(false));
        given().get("/auth/me").then().statusCode(200).body("authenticated", equalTo(false));

        String token = given().contentType("text/plain").body("alice").post("/auth/login")
                .then().statusCode(200).body("token", not(blankOrNullString()))
                .body("principal.loginId", equalTo("alice")).extract().path("token");
        try {
            given().header("satoken", token).get("/auth/status")
                    .then().statusCode(200).body("login", equalTo(true));
            given().header("satoken", token).get("/auth/me")
                    .then().statusCode(200).body("authenticated", equalTo(true))
                    .body("loginId", equalTo("alice")).body("userId", equalTo("alice"));
            given().header("satoken", token).queryParam("role", "user").get("/auth/check/role")
                    .then().statusCode(200).body("role", equalTo("user")).body("has", equalTo(true));
            given().header("satoken", token).queryParam("role", "admin").get("/auth/check/role")
                    .then().statusCode(200).body("has", equalTo(false));
            given().header("satoken", token).queryParam("permission", "profile:read")
                    .get("/auth/check/permission").then().statusCode(200)
                    .body("permission", equalTo("profile:read")).body("has", equalTo(true));
            given().header("satoken", token).queryParam("permission", "admin:write")
                    .get("/auth/check/permission").then().statusCode(200).body("has", equalTo(false));

            given().get("/auth/status").then().statusCode(200).body("login", equalTo(false));
            given().get("/auth/me").then().statusCode(200).body("authenticated", equalTo(false));
            given().header("satoken", "invalid-token").get("/auth/me")
                    .then().statusCode(200).body("authenticated", equalTo(false));
        } finally {
            given().header("satoken", token).post("/auth/logout")
                    .then().statusCode(200).body("success", equalTo(true));
        }
        given().header("satoken", token).get("/auth/status")
                .then().statusCode(200).body("login", equalTo(false));
        given().header("satoken", token).get("/auth/me")
                .then().statusCode(200).body("authenticated", equalTo(false));
        given().header("satoken", token).queryParam("permission", "profile:read")
                .get("/auth/check/permission").then().statusCode(200).body("has", equalTo(false));
    }
}
