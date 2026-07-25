package io.ddd4j.quarkus.cache;

import io.ddd4j.cache.CacheKit;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * ddd4j-quarkus cache configuration mapping.
 *
 * <p>Mirrors {@code ddd4j-boot-cache.CacheProperties} (Spring Boot
 * {@code @ConfigurationProperties(prefix = "ddd4j.cache")}) but expressed as a Quarkus
 * SmallRye {@link io.smallrye.config.ConfigMapping} so it can be consumed at build time
 * by {@link Ddd4jCacheBuildItemProducer}.
 *
 * <p>The default value is intentionally mapped from string ("CAFFEINE"/"GUAVA"/"HUTOOL")
 * to {@link CacheKit.LocalCacheType} by the build step, isolating the framework-neutral
 * API from the Quarkus-specific configuration shape.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@io.smallrye.config.ConfigMapping(prefix = "ddd4j.cache")
public interface Ddd4jCacheConfig {

    /**
     * Default local cache implementation.
     *
     * <p>Valid values map to {@link CacheKit.LocalCacheType}:
     * {@code CAFFEINE} (default), {@code GUAVA}, {@code HUTOOL}.
     */
    @WithName("default-type")
    @WithDefault("CAFFEINE")
    String defaultType();
}