package io.ddd4j.quarkus.web;

import io.ddd4j.web.core.AuthenticationMode;
import io.ddd4j.web.core.WebRequestContext;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Ddd4jQuarkusWebConfiguration#from(Config)} 配置解析测试。
 *
 * <p>用 MicroProfile Config（SmallRyeConfigBuilder）构造配置源，验证：
 * <ul>
 *   <li>{@code ddd4j.web.public-paths} 列表解析与 {@code standardAccessConfigured} 标记</li>
 *   <li>{@code ddd4j.web.authentication-mode} 大小写不敏感解析</li>
 *   <li>{@code ddd4j.web.idempotency.*} 幂等配置（enabled / cache-name / ttl）</li>
 *   <li>兼容遗留 {@code ddd4j.quarkus.web.*} 访问控制属性</li>
 *   <li>无配置时的默认值</li>
 * </ul>
 *
 * <p>纯单元测试（{@code from()} 不依赖 CDI 容器），不启动 Quarkus。
 */
class Ddd4jQuarkusWebConfigurationTest {

    private static Config config(String... keyValues) {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        for (int i = 0; i < keyValues.length; i += 2) {
            builder.withDefaultValue(keyValues[i], keyValues[i + 1]);
        }
        return builder.build();
    }

    private static WebRequestContext request(String path) {
        return new WebRequestContext("req-1", "trace-1", null, null, Locale.CHINA, null, "GET", path);
    }

    @Test
    void defaults_whenNothingConfigured() {
        Ddd4jQuarkusWebConfiguration configuration = Ddd4jQuarkusWebConfiguration.from(config());

        assertThat(configuration.getPublicPaths()).containsExactly("/health", "/q/health/**");
        assertThat(configuration.getDefaultAuthenticationMode()).isEqualTo(AuthenticationMode.REQUIRED);
        assertThat(configuration.isStandardAccessConfigured()).isFalse();
        assertThat(configuration.isTrustForwardedHeaders()).isFalse();
        assertThat(configuration.isIdempotencyEnabled()).isTrue();
        assertThat(configuration.getIdempotencyCacheName()).isEqualTo("ddd4j-web-idempotency");
        assertThat(configuration.getIdempotencyTtl()).isEqualTo(Duration.ofMinutes(5));
        // 未配置 standard 与 legacy 时默认关闭访问控制
        assertThat(configuration.accessPolicy().authenticationMode(request("/anything"))).isEqualTo(AuthenticationMode.DISABLED);
    }

    @Test
    void parsesPublicPathsAndAuthenticationMode() {
        Ddd4jQuarkusWebConfiguration configuration = Ddd4jQuarkusWebConfiguration.from(config(
                "ddd4j.web.public-paths", "/public/**,/open/api/**",
                "ddd4j.web.authentication-mode", "optional"));

        assertThat(configuration.getPublicPaths()).containsExactly("/public/**", "/open/api/**");
        assertThat(configuration.getDefaultAuthenticationMode()).isEqualTo(AuthenticationMode.OPTIONAL);
        assertThat(configuration.isStandardAccessConfigured()).isTrue();
    }

    @Test
    void authenticationModeParsingIsCaseInsensitive() {
        Ddd4jQuarkusWebConfiguration configuration = Ddd4jQuarkusWebConfiguration.from(config(
                "ddd4j.web.authentication-mode", "rEqUiReD"));
        assertThat(configuration.getDefaultAuthenticationMode()).isEqualTo(AuthenticationMode.REQUIRED);
    }

    @Test
    void parsesIdempotencyConfiguration() {
        Ddd4jQuarkusWebConfiguration configuration = Ddd4jQuarkusWebConfiguration.from(config(
                "ddd4j.web.idempotency.enabled", "false",
                "ddd4j.web.idempotency.cache-name", "my-idem-cache",
                "ddd4j.web.idempotency.ttl", "PT2H"));

        assertThat(configuration.isIdempotencyEnabled()).isFalse();
        assertThat(configuration.getIdempotencyCacheName()).isEqualTo("my-idem-cache");
        assertThat(configuration.getIdempotencyTtl()).isEqualTo(Duration.ofHours(2));
    }

    @Test
    void standardAccessPolicyTreatsPublicPathsAsDisabled() {
        Ddd4jQuarkusWebConfiguration configuration = Ddd4jQuarkusWebConfiguration.from(config(
                "ddd4j.web.public-paths", "/public/**,/q/health/**",
                "ddd4j.web.authentication-mode", "required"));

        assertThat(configuration.accessPolicy().authenticationMode(request("/public/open"))).isEqualTo(AuthenticationMode.DISABLED);
        assertThat(configuration.accessPolicy().authenticationMode(request("/public"))).isEqualTo(AuthenticationMode.DISABLED);
        assertThat(configuration.accessPolicy().authenticationMode(request("/q/health/ready"))).isEqualTo(AuthenticationMode.DISABLED);
        assertThat(configuration.accessPolicy().authenticationMode(request("/private/resource"))).isEqualTo(AuthenticationMode.REQUIRED);
    }

    @Test
    void legacyBearerRequiredEnforcesProtectedPrefix() {
        Ddd4jQuarkusWebConfiguration configuration = Ddd4jQuarkusWebConfiguration.from(config(
                "ddd4j.quarkus.web.bearer-required", "true",
                "ddd4j.quarkus.web.protected-path-prefix", "/api"));

        assertThat(configuration.isLegacyBearerRequired()).isTrue();
        assertThat(configuration.getLegacyProtectedPathPrefix()).isEqualTo("/api");
        // 受保护前缀内 REQUIRED，前缀外与健康检查路径 DISABLED
        assertThat(configuration.accessPolicy().authenticationMode(request("/api/orders"))).isEqualTo(AuthenticationMode.REQUIRED);
        assertThat(configuration.accessPolicy().authenticationMode(request("/health"))).isEqualTo(AuthenticationMode.DISABLED);
        assertThat(configuration.accessPolicy().authenticationMode(request("/login"))).isEqualTo(AuthenticationMode.DISABLED);
    }

    @Test
    void parsesTrustForwardedHeaders() {
        Ddd4jQuarkusWebConfiguration configuration = Ddd4jQuarkusWebConfiguration.from(config(
                "ddd4j.web.trust-forwarded-headers", "true"));
        assertThat(configuration.isTrustForwardedHeaders()).isTrue();
    }
}
