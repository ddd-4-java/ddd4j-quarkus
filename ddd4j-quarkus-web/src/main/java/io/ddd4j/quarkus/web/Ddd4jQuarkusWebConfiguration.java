package io.ddd4j.quarkus.web;

import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.web.core.AuthenticationMode;
import io.ddd4j.web.core.PathWebAccessPolicy;
import io.ddd4j.web.core.WebAccessPolicy;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Quarkus Web 配置（ddd4j 桥接层），并兼容早期 {@code ddd4j.quarkus.web.*} 访问控制属性。
 *
 * <p>对齐 ddd4j-boot 的 {@code Ddd4jWebMvcProperties}。本类由 {@link Ddd4jQuarkusWebFilter}
 * 在构造期一次性加载，配置变更不会热更新（与 Spring Boot 不同）。
 */
@Getter
@Setter
public class Ddd4jQuarkusWebConfiguration {

    private static final String PREFIX = "ddd4j.web.";
    private static final String LEGACY_PREFIX = "ddd4j.quarkus.web.";

    private List<String> publicPaths = new ArrayList<>(List.of("/health", "/q/health/**"));
    private AuthenticationMode defaultAuthenticationMode = AuthenticationMode.REQUIRED;
    private boolean standardAccessConfigured;
    private boolean legacyBearerRequired;
    private String legacyProtectedPathPrefix = "/";
    private boolean trustForwardedHeaders;
    private boolean idempotencyEnabled = true;
    private String idempotencyCacheName = "ddd4j-web-idempotency";
    private Duration idempotencyTtl = Duration.ofMinutes(5);

    public static Ddd4jQuarkusWebConfiguration load() {
        return from(ConfigProvider.getConfig());
    }

    public static Ddd4jQuarkusWebConfiguration from(Config config) {
        Config source = Objects.requireNonNull(config, "config must not be null");
        Ddd4jQuarkusWebConfiguration configuration = new Ddd4jQuarkusWebConfiguration();
        source.getOptionalValues(PREFIX + "public-paths", String.class).ifPresent(values -> {
            configuration.setPublicPaths(new ArrayList<>(values));
            configuration.setStandardAccessConfigured(true);
        });
        source.getOptionalValue(PREFIX + "authentication-mode", String.class).ifPresent(value -> {
            configuration.setDefaultAuthenticationMode(
                    AuthenticationMode.valueOf(value.toUpperCase(Locale.ROOT)));
            configuration.setStandardAccessConfigured(true);
        });
        source.getOptionalValue(LEGACY_PREFIX + "bearer-required", Boolean.class)
                .ifPresent(configuration::setLegacyBearerRequired);
        source.getOptionalValue(LEGACY_PREFIX + "protected-path-prefix", String.class)
                .ifPresent(configuration::setLegacyProtectedPathPrefix);
        source.getOptionalValue(PREFIX + "trust-forwarded-headers", Boolean.class)
                .ifPresent(configuration::setTrustForwardedHeaders);
        source.getOptionalValue(PREFIX + "idempotency.enabled", Boolean.class)
                .ifPresent(configuration::setIdempotencyEnabled);
        source.getOptionalValue(PREFIX + "idempotency.cache-name", String.class)
                .ifPresent(configuration::setIdempotencyCacheName);
        source.getOptionalValue(PREFIX + "idempotency.ttl", String.class)
                .map(Duration::parse)
                .ifPresent(configuration::setIdempotencyTtl);
        return configuration;
    }

    public WebAccessPolicy accessPolicy() {
        if (standardAccessConfigured) {
            return new PathWebAccessPolicy(publicPaths, defaultAuthenticationMode);
        }
        if (!legacyBearerRequired) {
            return WebAccessPolicy.disabled();
        }
        String protectedPrefix = StrKit.isBlank(legacyProtectedPathPrefix) ? "/" : legacyProtectedPathPrefix;
        PathWebAccessPolicy healthPolicy = new PathWebAccessPolicy(publicPaths, AuthenticationMode.REQUIRED);
        return context -> context.path().startsWith(protectedPrefix)
                ? healthPolicy.authenticationMode(context) : AuthenticationMode.DISABLED;
    }
}