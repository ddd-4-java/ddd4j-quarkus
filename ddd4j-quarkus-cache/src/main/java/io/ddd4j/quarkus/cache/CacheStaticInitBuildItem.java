package io.ddd4j.quarkus.cache;

import io.ddd4j.cache.CacheKit;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker build item produced by {@link Ddd4jCacheBuildItemProducer} after the cache
 * default type has been configured at static-init time.
 *
 * <p>Carrying the resolved {@link CacheKit.LocalCacheType} allows downstream build steps
 * to depend on the cache being configured without re-reading application configuration.
 *
 * <p>Last-instances-are-final: Quarkus only emits a single build step producing this item
 * per deployment, so consumers can rely on it being unique.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public final class CacheStaticInitBuildItem extends SimpleBuildItem {

    private final CacheKit.LocalCacheType defaultType;

    CacheStaticInitBuildItem(CacheKit.LocalCacheType defaultType) {
        this.defaultType = defaultType;
    }

    public CacheKit.LocalCacheType defaultType() {
        return defaultType;
    }
}