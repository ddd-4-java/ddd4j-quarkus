package io.ddd4j.quarkus.qlexpress.rule;

import java.util.List;
import java.util.Optional;

/**
 * 规则持久化 SPI。业务系统可以用 JPA、MyBatis 或远程配置中心替换默认实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public interface RuleRepository {

    /**
     * 保存。
     * @param rule  规则
     * @return 保存
     */
    RuleDefinition save(RuleDefinition rule);

    /**
     * findBy标识。
     * @param id  标识
     * @return findBy标识
     */
    Optional<RuleDefinition> findById(String id);

    /**
     * findBy编码。
     * @param code  编码
     * @return findBy编码
     */
    Optional<RuleDefinition> findByCode(String code);

    /**
     * 执行 findAll 操作。
     * @return 方法返回结果
     */
    List<RuleDefinition> findAll();

    /**
     * 删除By标识。
     * @param id  标识
     */
    void deleteById(String id);
}
