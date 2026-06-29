package io.ddd4j.quarkus.cache;

import io.ddd4j.cache.CacheKit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * ddd4j-quarkus 缓存配置。
 *
 * <p>{@link CacheKit} 是静态工具类（private 构造器），无需注册为 Bean。
 * 本类仅通过 MicroProfile Config 读取默认缓存类型并设置到 CacheKit。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class Ddd4jCacheConfig {

    @ConfigProperty(name = "ddd4j.cache.default-type", defaultValue = "CAFFEINE")
    public void setDefaultType(String defaultType) {
        CacheKit.setDefaultType(CacheKit.LocalCacheType.valueOf(defaultType));
    }

}
