package io.ddd4j.quarkus.cache;

import io.ddd4j.cache.CacheKit;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;

/**
 * Quarkus build-step producer for the ddd4j cache integration.
 *
 * <p>Two responsibilities, mirroring {@code Ddd4jCacheAutoConfiguration} from ddd4j-boot:
 *
 * <ol>
 *   <li>Register {@link Ddd4jCacheConfig} as a CDI bean so application code can inject the
 *       configuration mapping (Quarkus equivalent of {@code @EnableConfigurationProperties}).</li>
 *   <li>Replay the resolved default type at {@link ExecutionTime#STATIC_INIT} via
 *       {@link CacheRecorder#configureDefaultType(CacheKit.LocalCacheType)}, so
 *       {@link CacheKit#setDefaultType(CacheKit.LocalCacheType)} runs before any runtime
 *       access to {@link CacheKit}.</li>
 * </ol>
 *
 * <p>The bean is registered unconditionally because {@link CacheKit} is always on the
 * classpath when this extension is present, and the configuration mapping is harmless when
 * the user has not set {@code ddd4j.cache.default-type}.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jCacheBuildItemProducer {

    /**
     * Expose {@link Ddd4jCacheConfig} as an application-scoped CDI bean.
     */
    @BuildStep
    public AdditionalBeanBuildItem exposeCacheConfig() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(Ddd4jCacheConfig.class)
                .setUnremovable()
                .build();
    }

    /**
     * Resolve the build-time {@code ddd4j.cache.default-type} string into a
     * {@link CacheKit.LocalCacheType} and replay it via the {@link CacheRecorder}
     * during static initialization.
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public CacheStaticInitBuildItem configureCacheDefaultType(Ddd4jCacheConfig config, CacheRecorder recorder) {
        CacheKit.LocalCacheType type;
        try {
            type = CacheKit.LocalCacheType.valueOf(config.defaultType());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalStateException(
                    "Invalid ddd4j.cache.default-type '" + config.defaultType()
                            + "'. Valid values: CAFFEINE, GUAVA, HUTOOL.", e);
        }
        recorder.configureDefaultType(type);
        return new CacheStaticInitBuildItem(type);
    }
}