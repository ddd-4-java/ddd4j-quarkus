package io.ddd4j.quarkus.qlexpress.rule.support;

import io.ddd4j.cache.CacheKit;
import io.ddd4j.quarkus.qlexpress.rule.RuleCache;
import io.ddd4j.quarkus.qlexpress.rule.RuleDefinition;

import java.util.Objects;

/**
 * {@link CacheKit} 规则缓存适配器（Quarkus 版）。
 *
 * <p>对应 boot 模块的 {@code SpringCacheRuleCache}：boot 版基于 Spring Cache
 * （{@code CacheManager.getCache(cacheName)}），Quarkus 版改为基于 ddd4j 的
 * {@link CacheKit} 门面，按 {@code biz}（规则缓存名，默认 {@code ddd4j:qlexpress:rules}）
 * 操作。底层可以是 Caffeine/Guava/Hutool 本地缓存，也可以是注册到 CacheKit 的远程缓存。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public final class CacheKitRuleCache implements RuleCache {

    private final String biz;

    /**
     * @param biz CacheKit 业务标识（对应 {@code ddd4j.qlexpress.rules.cache-name}）
     */
    public CacheKitRuleCache(String biz) {
        this.biz = Objects.requireNonNull(biz, "biz 不能为空");
    }

    @Override
    public RuleDefinition get(String code) {
        return CacheKit.get(biz, code);
    }

    @Override
    public void put(String code, RuleDefinition rule) {
        if (Objects.nonNull(code) && Objects.nonNull(rule)) {
            CacheKit.put(biz, code, rule);
        }
    }

    @Override
    public void evict(String code) {
        if (Objects.nonNull(code)) {
            CacheKit.invalidate(biz, code);
        }
    }

    @Override
    public void clear() {
        CacheKit.invalidateAll(biz);
    }
}
