package io.ddd4j.quarkus.qlexpress.rule.support;

import io.ddd4j.quarkus.qlexpress.rule.RuleCache;
import io.ddd4j.quarkus.qlexpress.rule.RuleDefinition;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内规则缓存。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public final class InMemoryRuleCache implements RuleCache {

    private final ConcurrentHashMap<String, RuleDefinition> cache = new ConcurrentHashMap<>();

    @Override
    public RuleDefinition get(String code) {
        return cache.get(code);
    }

    @Override
    public void put(String code, RuleDefinition rule) {
        if (Objects.nonNull(code) && Objects.nonNull(rule)) {
            cache.put(code, rule);
        }
    }

    @Override
    public void evict(String code) {
        if (Objects.nonNull(code)) {
            cache.remove(code);
        }
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
