package io.ddd4j.quarkus.qlexpress;

import io.ddd4j.cache.CacheKit;
import io.ddd4j.extension.qlexpress.QLExpress;
import io.ddd4j.extension.qlexpress.QLExpressEngine;
import io.ddd4j.extension.qlexpress.QLExpressEngineBuilder;
import io.ddd4j.extension.qlexpress.function.NamedQLFunction;
import io.ddd4j.extension.qlexpress.model.QLExpressExecutionOptions;
import io.ddd4j.quarkus.qlexpress.rule.RuleCache;
import io.ddd4j.quarkus.qlexpress.rule.RuleChangedEvent;
import io.ddd4j.quarkus.qlexpress.rule.RuleRepository;
import io.ddd4j.quarkus.qlexpress.rule.RuleService;
import io.ddd4j.quarkus.qlexpress.rule.support.CacheKitRuleCache;
import io.ddd4j.quarkus.qlexpress.rule.support.InMemoryRuleCache;
import io.ddd4j.quarkus.qlexpress.rule.support.InMemoryRuleRepository;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.util.Objects;

/**
 * QLExpress 工具引擎与可选规则管理能力的 Quarkus CDI Producer
 * （替代 boot 版的 {@code Ddd4jQLExpressBootAutoConfiguration}）。
 *
 * <p>与 boot 版差异：
 * <ul>
 *   <li>{@code @AutoConfiguration + @ConditionalOnProperty} → {@code @IfBuildProperty}（构建期条件）；</li>
 *   <li>Spring {@code CacheManager} 适配 → ddd4j {@link CacheKit} 门面（见
 *       {@link CacheKitRuleCache}），缓存按 {@code rules.cacheName} 自动创建；</li>
 *   <li>{@code ApplicationEventPublisher} → Quarkus CDI {@link Event}；</li>
 *   <li>自定义 {@link NamedQLFunction} 通过 CDI {@link Instance} 收集。</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
@IfBuildProperty(name = "ddd4j.qlexpress.enabled", stringValue = "true", enableIfMissing = true)
public class QLExpressCdiProducer {

    /**
     * 规则缓存默认过期时间（秒），本地缓存兜底值。
     */
    private static final long DEFAULT_RULES_CACHE_EXPIRE_SECONDS = 3600L;

    /**
     * 装配 QLExpress 引擎（含用户自定义函数）。
     *
     * @param config        QLExpress 配置
     * @param functionBeans CDI 容器中的自定义函数
     * @return QLExpressEngine 实例
     */
    @Produces
    @Singleton
    public QLExpressEngine qlExpressEngine(QLExpressConfig config, Instance<NamedQLFunction> functionBeans) {
        QLExpressExecutionOptions executionOptions = QLExpressExecutionOptions.builder()
                .timeoutMillis(config.timeoutMillis())
                .cache(config.cache())
                .precise(config.precise())
                .avoidNullPointer(config.avoidNullPointer())
                .maxArrayLength(config.maxArrayLength())
                .traceExpression(config.traceExpression())
                .build();

        QLExpressEngineBuilder builder = QLExpress.builder()
                .builtInFunctions(config.builtInFunctions())
                .allowPrivateAccess(config.allowPrivateAccess())
                .traceExpression(config.traceExpression())
                .defaultExecutionOptions(executionOptions);
        functionBeans.forEach(builder::function);
        return builder.build();
    }

    /**
     * 装配规则仓储（无外部存储时使用进程内实现）。
     *
     * @return RuleRepository 实例
     */
    @Produces
    @Singleton
    @IfBuildProperty(name = "ddd4j.qlexpress.rules.enabled", stringValue = "true", enableIfMissing = true)
    public RuleRepository ruleRepository() {
        return new InMemoryRuleRepository();
    }

    /**
     * 装配规则缓存：优先使用 ddd4j {@link CacheKit}（biz = rules.cacheName），
     * 未注册时自动创建本地缓存；boot 版此位置为 Spring Cache 适配。
     *
     * @param config QLExpress 配置
     * @return RuleCache 实例
     */
    @Produces
    @Singleton
    @IfBuildProperty(name = "ddd4j.qlexpress.rules.enabled", stringValue = "true", enableIfMissing = true)
    public RuleCache ruleCache(QLExpressConfig config) {
        String cacheName = config.rules().cacheName();
        if (Objects.isNull(CacheKit.getCache(cacheName))) {
            CacheKit.build(cacheName, DEFAULT_RULES_CACHE_EXPIRE_SECONDS);
        }
        return new CacheKitRuleCache(cacheName);
    }

    /**
     * 装配规则服务（规则 CRUD、缓存协调、执行和 CDI 事件发布）。
     *
     * @param repository     规则仓储
     * @param cache          规则缓存
     * @param engine         QLExpress 引擎
     * @param eventPublisher 规则变更事件发布器（CDI Event）
     * @return RuleService 实例
     */
    @Produces
    @Singleton
    @IfBuildProperty(name = "ddd4j.qlexpress.rules.enabled", stringValue = "true", enableIfMissing = true)
    public RuleService ruleService(RuleRepository repository,
                                   RuleCache cache,
                                   QLExpressEngine engine,
                                   Event<RuleChangedEvent> eventPublisher) {
        return new RuleService(repository, cache, engine, eventPublisher);
    }
}
