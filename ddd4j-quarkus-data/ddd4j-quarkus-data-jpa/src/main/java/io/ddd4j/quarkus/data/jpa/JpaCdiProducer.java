package io.ddd4j.quarkus.data.jpa;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Quarkus CDI Producer：将 Hibernate ORM 的 {@link EntityManager} 暴露为应用作用域 Bean，
 * 供业务侧 {@code JpaAggregateRepository} 子类在构造期通过 {@code @Inject} 注入。
 *
 * <p>对齐 ddd4j-boot 的 JPA 自动装配行为，但用 CDI Producer 而非 Spring Bean 工厂。
 * {@link EntityManager} 本身是线程绑定的（事务作用域），生产代码应在 {@code @Transactional}
 * 方法内调用。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class JpaCdiProducer {

    @Produces
    @PersistenceContext(unitName = "<default>")
    public EntityManager defaultEntityManager() {
        // Hibernate ORM Quarkus 引擎会自动注入实际 EntityManager。
        // 本方法仅作为 CDI 桥接占位，业务项目应在 application.properties 配置
        // quarkus.hibernate-orm.database.generation=drop-and-create 等参数。
        return null;
    }
}