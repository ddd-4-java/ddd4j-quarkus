package io.ddd4j.quarkus.qlexpress;

import io.ddd4j.extension.qlexpress.model.QLExpressExecutionOptions;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * QLExpress Quarkus 配置（替代 boot 版的 {@code QLExpressProperties}）。
 *
 * <p>前缀 {@code ddd4j.qlexpress}，通过 SmallRye {@link ConfigMapping} 映射。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ConfigMapping(prefix = "ddd4j.qlexpress")
public interface QLExpressConfig {

    @WithDefault("true")
    boolean enabled();

    @WithDefault("true")
    boolean builtInFunctions();

    @WithDefault("false")
    boolean allowPrivateAccess();

    @WithDefault("false")
    boolean traceExpression();

    /**
     * 单次执行超时（毫秒），默认值与 {@link QLExpressExecutionOptions#DEFAULT_TIMEOUT_MILLIS} 一致。
     */
    @WithDefault("3000")
    long timeoutMillis();

    @WithDefault("true")
    boolean cache();

    @WithDefault("false")
    boolean precise();

    @WithDefault("false")
    boolean avoidNullPointer();

    /**
     * 数组长度上限，默认值与 {@link QLExpressExecutionOptions#DEFAULT_MAX_ARRAY_LENGTH} 一致。
     */
    @WithDefault("10000")
    int maxArrayLength();

    /**
     * 可选规则管理能力配置（{@code ddd4j.qlexpress.rules.*}）。
     */
    Rules rules();

    /**
     * 规则管理子配置。
     */
    interface Rules {

        @WithDefault("true")
        boolean enabled();

        @WithDefault("ddd4j:qlexpress:rules")
        String cacheName();
    }
}
