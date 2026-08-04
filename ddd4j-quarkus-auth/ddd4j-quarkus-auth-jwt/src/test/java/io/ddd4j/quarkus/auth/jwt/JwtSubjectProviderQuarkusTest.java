package io.ddd4j.quarkus.auth.jwt;

import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.core.subject.Subject;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * {@link JwtSubject}/{@link JwtSubjectProvider} Quarkus 集成测试。
 *
 * <p>基于 {@code quarkus-smallrye-jwt} + 测试 RSA 密钥对（{@code src/test/resources}），
 * 用 SmallRye {@link Jwt} 构建真实签名令牌，验证：
 * <ul>
 *   <li>请求作用域注入的 {@link JwtSubject}：principal 从 JWT claim（upn）+ Header（site/uid）构建</li>
 *   <li>{@link SubjectKit#getSubject()} 静态门面在请求内解析到同一主体（JwtSubjectProvider 桥接 Arc）</li>
 *   <li>角色校验（JWT groups → {@code @RolesAllowed} 一致）</li>
 *   <li>无令牌访问受保护端点返回 401</li>
 *   <li>非请求线程下 provider 兜底返回空 Subject（principal 非空、未认证）</li>
 * </ul>
 */
@QuarkusTest
class JwtSubjectProviderQuarkusTest {

    private static final String ISSUER = "https://example.com/issuer";

    private static String adminToken() {
        return Jwt.issuer(ISSUER)
                .upn("user-1001")
                .groups("admin")
                .claim("uid", "u-1001")
                .sign();
    }

    @Test
    void requestScopedJwtSubjectInjectedWithPrincipalFromTokenAndHeaders() {
        String token = adminToken();

        given()
                .header("Authorization", "Bearer " + token)
                .header("site", "tenant-A")
                .when().get("/jwt-subject")
                .then()
                .statusCode(200)
                // principal 来自 JWT upn claim
                .body(containsString("\"loginId\":\"user-1001\""))
                // 角色来自 JWT groups
                .body(containsString("\"roles\":\"admin\""))
                .body(containsString("\"hasAdminRole\":true"))
                // SubjectKit 静态门面与 provider 解析到同一主体
                .body(containsString("\"kitLoginId\":\"user-1001\""))
                .body(containsString("\"providerLoginId\":\"user-1001\""))
                // 租户从请求头 site 写入 profile
                .body(containsString("\"tenantId\":\"tenant-A\""));
    }

    @Test
    void missingTokenRejectedByRolesAllowed() {
        given()
                .when().get("/jwt-subject")
                .then()
                .statusCode(401);
    }

    @Test
    void providerFallsBackToEmptySubjectOutsideRequestScope() {
        JwtSubjectProvider provider = new JwtSubjectProvider();

        Subject subject = provider.getSubject();

        assertThat(subject).isNotNull();
        // 非请求线程兜底：principal 非空但 loginId 为空、未认证、无角色
        AuthPrincipal principal = subject.getPrincipal();
        assertThat(principal).isNotNull();
        assertThat(principal.getLoginId()).isNull();
        assertThat(subject.isAuthenticated()).isFalse();
        assertThat(subject.hasRole("admin")).isFalse();
    }
}
