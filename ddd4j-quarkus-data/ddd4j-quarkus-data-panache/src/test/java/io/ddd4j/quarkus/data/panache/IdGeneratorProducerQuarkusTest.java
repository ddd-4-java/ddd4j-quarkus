package io.ddd4j.quarkus.data.panache;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link IdGeneratorProducer} 的 CDI 装配测试。
 *
 * <p>验证 producer（raw Bean 类型，可赋值给参数化注入点）可通过 CDI 注入解析，
 * 且默认策略为雪花（返回正数 Long）。
 *
 * <p>测试配置禁用数据源与 Hibernate ORM（本测试只关注 producer 的 Bean 语义，
 * 不触达持久层；DevServices 关闭避免无谓拉起数据库容器）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@QuarkusTest
class IdGeneratorProducerQuarkusTest {

    @Inject
    IdGenerationStrategy<Long> idStrategy;

    @Test
    void producer_resolves_as_cdi_bean_with_default_snowflake() {
        assertNotNull(idStrategy, "IdGenerationStrategy 应可通过 CDI 注入解析");

        Long id = idStrategy.generate();
        assertNotNull(id, "默认策略（snowflake）不应返回 null");
        assertTrue(id > 0, "雪花 ID 应为正数");
    }
}
