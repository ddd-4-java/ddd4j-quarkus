package io.ddd4j.quarkus.cache;

import io.ddd4j.cache.CacheKit;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Static-init recorder for ddd4j cache configuration.
 *
 * <p>Because {@link CacheKit} exposes mutable static state ({@code defaultType}), the value
 * computed at build time must be replayed during the {@code STATIC_INIT} execution phase,
 * before the first CDI observer or REST endpoint touches {@link CacheKit}.
 *
 * <p>This pattern mirrors {@code Ddd4jCacheAutoConfiguration#CacheProperties#setDefaultType}
 * in ddd4j-boot, where the Spring property binding handler invokes the same setter on bean
 * post-processing.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Recorder
public class CacheRecorder {

    /**
     * Configure the default local cache implementation used by {@link CacheKit}.
     *
     * <p>Returns a {@link RuntimeValue} (Quarkus convention for static-init produced handles)
     * so that downstream build steps can reference the configured type if needed.
     *
     * @param defaultType the local cache implementation to use as default
     * @return a runtime value carrying the configured type
     */
    public RuntimeValue<CacheKit.LocalCacheType> configureDefaultType(CacheKit.LocalCacheType defaultType) {
        CacheKit.setDefaultType(defaultType);
        return new RuntimeValue<>(defaultType);
    }
}