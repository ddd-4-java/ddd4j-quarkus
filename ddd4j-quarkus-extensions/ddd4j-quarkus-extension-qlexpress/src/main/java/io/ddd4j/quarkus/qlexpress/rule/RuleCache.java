package io.ddd4j.quarkus.qlexpress.rule;

/**
 * 规则缓存 SPI。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public interface RuleCache {

    /**
     * 获取 get。
     * @param code  编码
     * @return 获取
     */
    RuleDefinition get(String code);

    /**
     * 放入。
     * @param code  编码
     * @param rule  规则
     */
    void put(String code, RuleDefinition rule);

    /**
     * 淘汰。
     * @param code  编码
     */
    void evict(String code);

    /**
     * 清空。
     */
    void clear();
}
