package io.ddd4j.quarkus.qlexpress.rule;

/**
 * 规则缓存 SPI。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public interface RuleCache {

    RuleDefinition get(String code);

    void put(String code, RuleDefinition rule);

    void evict(String code);

    void clear();
}
