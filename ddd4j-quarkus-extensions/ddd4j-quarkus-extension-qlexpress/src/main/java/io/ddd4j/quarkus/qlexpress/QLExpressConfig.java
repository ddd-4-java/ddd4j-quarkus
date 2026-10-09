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

    /**
     * 是否启用 QLExpress 扩展装配（总开关）。
     *
     * @return 启用开关取值
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * 是否启用内置函数库。
     *
     * @return 内置函数开关取值
     */
    @WithDefault("true")
    boolean builtInFunctions();

    /**
     * 是否允许反射访问私有成员。
     *
     * @return 私有访问开关取值
     */
    @WithDefault("false")
    boolean allowPrivateAccess();

    /**
     * 是否开启表达式执行 trace 日志。
     *
     * @return trace 开关取值
     */
    @WithDefault("false")
    boolean traceExpression();

    /**
     * 单次执行超时（毫秒），默认值与 {@link QLExpressExecutionOptions#DEFAULT_TIMEOUT_MILLIS} 一致。
     *
     * @return 超时毫秒数
     */
    @WithDefault("3000")
    long timeoutMillis();

    /**
     * 是否启用表达式执行结果缓存。
     *
     * @return 缓存开关取值
     */
    @WithDefault("true")
    boolean cache();

    /**
     * 是否启用精确计算模式（BigDecimal）。
     *
     * @return 精确计算开关取值
     */
    @WithDefault("false")
    boolean precise();

    /**
     * 是否将空指针转为求值异常避免直接 NPE。
     *
     * @return 空指针规避开关取值
     */
    @WithDefault("false")
    boolean avoidNullPointer();

    /**
     * 数组长度上限，默认值与 {@link QLExpressExecutionOptions#DEFAULT_MAX_ARRAY_LENGTH} 一致。
     *
     * @return 数组长度上限
     */
    @WithDefault("10000")
    int maxArrayLength();

    /**
     * 可选规则管理能力配置（{@code ddd4j.qlexpress.rules.*}）。
     *
     * @return 规则管理子配置
     */
    Rules rules();

    /**
     * 规则管理子配置。
     */
    interface Rules {

        /**
         * 是否启用规则管理能力。
         *
         * @return 启用开关取值
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * 规则缓存名称（CacheKit 缓存池名）。
         *
         * @return 缓存名称
         */
        @WithDefault("ddd4j:qlexpress:rules")
        String cacheName();
    }
}
