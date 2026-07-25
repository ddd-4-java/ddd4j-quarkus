package io.ddd4j.quarkus.data.jpa;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Quarkus 集成测试：验证 {@link JpaCdiProducer} 暴露的 EntityManager Bean 可被 CDI 解析。
 *
 * <p>对齐 ddd4j-boot 中 {@code Ddd4jMybatisAutoConfigurationTest} 的契约验证思路：
 * 启动 Quarkus runtime + Hibernate ORM DevServices（H2 内存库），断言 EntityManager
 * 已就绪。
 */
@QuarkusTest
class JpaCdiProducerTest {

    @Inject
    EntityManager entityManager;

    @Test
    void shouldInjectEntityManagerBean() {
        assertThat(entityManager).isNotNull();
        // H2 数据库（devservices 默认）应处于打开状态，可执行最简单的查询
        Object result = entityManager.createNativeQuery("SELECT 1").getSingleResult();
        assertThat(result).isEqualTo(1);
    }
}