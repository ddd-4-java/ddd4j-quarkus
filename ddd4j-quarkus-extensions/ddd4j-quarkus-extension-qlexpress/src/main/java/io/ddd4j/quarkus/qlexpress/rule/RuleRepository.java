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

    RuleDefinition save(RuleDefinition rule);

    Optional<RuleDefinition> findById(String id);

    Optional<RuleDefinition> findByCode(String code);

    List<RuleDefinition> findAll();

    void deleteById(String id);
}
