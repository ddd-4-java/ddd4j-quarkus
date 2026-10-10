package io.ddd4j.quarkus.data.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hibernate ORM + PostgreSQL 容器级 round-trip 集成测试。
 *
 * <p>与既有 {@link PostgresJdbcContainerSmokeTest}（裸 JDBC 建表/写入/读取）互补：
 * 本测试在真实 PostgreSQL 容器上以 {@link SessionFactory}（{@code EntityManagerFactory}）
 * 走完整 JPA 管道——实体元数据 → DDL（create-drop）→ 事务提交/回滚 → JPQL 计数 →
 * 更新 → 删除，对齐 ddd4j-javalin 家族 {@code Ddd4jJpaJavalinPostgresIT} 的
 * commit/rollback/close 契约；同时与 {@link JpaCdiProducerTest}（{@code @QuarkusTest} +
 * H2 devservices 的 CDI 注入冒烟）互补——后者验证 Bean 装配，本测试验证容器上的持久化语义。
 *
 * <p><b>为何不用 persistence.xml</b>：测试 classpath 上的 {@code META-INF/persistence.xml}
 * 会被 Quarkus ORM 增强（{@code @QuarkusTest}）一起扫描，存在干扰既有测试的风险；
 * 故采用 Hibernate 原生引导（{@code MetadataSources#addAnnotatedClass} 显式注册实体），
 * 零副作用、无需 XML，且对 {@code hbm2ddl}/JDBC 连接参数完全可控。
 *
 * <p>Docker daemon 不可达时由 {@code @Testcontainers(disabledWithoutDocker = true)}
 * 优雅跳过（不计入失败）；有 Docker 的环境（含 CI）真实启动容器执行往返。
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgresHibernateContainerIntegrationTest {

    /** 钉版镜像（固定补丁标签，非浮动别名），与既有冒烟测试同版。 */
    // 测试镜像来源: https://testcontainers.com/modules/postgresql/
    private static final String IMAGE = "postgres:16.15-alpine";

    // Testcontainers 2.x 的 org.testcontainers.postgresql.PostgreSQLContainer 是非泛型类
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(IMAGE)
            .withDatabaseName("ddd4j_test")
            .withUsername("test")
            .withPassword("test");

    private static SessionFactory sessionFactory;

    @BeforeAll
    static void bootstrapSessionFactory() {
        // Hibernate 原生引导：只给 JDBC 四要素 + hbm2ddl，方言由 Hibernate 从连接元数据探测
        ServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("jakarta.persistence.jdbc.url", postgres.getJdbcUrl())
                .applySetting("jakarta.persistence.jdbc.user", postgres.getUsername())
                .applySetting("jakarta.persistence.jdbc.password", postgres.getPassword())
                .applySetting("jakarta.persistence.jdbc.driver", "org.postgresql.Driver")
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .build();
        Metadata metadata = new MetadataSources(registry)
                .addAnnotatedClass(JpaSmokeRecord.class)
                .buildMetadata();
        sessionFactory = metadata.buildSessionFactory();
    }

    @AfterAll
    static void closeSessionFactory() {
        if (Objects.nonNull(sessionFactory)) {
            sessionFactory.close();
        }
    }

    @Test
    void shouldPersistReloadAndCountEntityAfterCommit() {
        persist(new JpaSmokeRecord("commit-1", "visible"));

        // 提交后清空一级缓存再读，验证走的是真实数据库而非会话缓存
        JpaSmokeRecord reloaded = find("commit-1");
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getValue()).isEqualTo("visible");

        // JPQL 计数（带主键过滤，与执行顺序无关）
        assertThat(countById("commit-1")).isEqualTo(1L);
        assertThat(countById("rollback-1")).isZero();
    }

    @Test
    void shouldDiscardEntityRolledBackInTransaction() {
        EntityManager entityManager = sessionFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(new JpaSmokeRecord("rollback-1", "hidden"));
            // 显式回滚：未提交数据不得落库
            transaction.rollback();
        } finally {
            entityManager.close();
        }

        assertThat(find("rollback-1")).isNull();
        assertThat(countById("rollback-1")).isZero();
    }

    @Test
    void shouldUpdateAndDeleteEntityRoundTrip() {
        persist(new JpaSmokeRecord("crud-1", "before"));

        // 更新：读出 → 改值 → 提交 → 重新读出验证
        EntityManager updateManager = sessionFactory.createEntityManager();
        EntityTransaction updateTransaction = updateManager.getTransaction();
        try {
            updateTransaction.begin();
            JpaSmokeRecord record = updateManager.find(JpaSmokeRecord.class, "crud-1");
            assertThat(record).isNotNull();
            record.setValue("after");
            updateTransaction.commit();
        } finally {
            updateManager.close();
        }
        assertThat(find("crud-1").getValue()).isEqualTo("after");

        // 删除：提交后不可再查到
        EntityManager deleteManager = sessionFactory.createEntityManager();
        EntityTransaction deleteTransaction = deleteManager.getTransaction();
        try {
            deleteTransaction.begin();
            JpaSmokeRecord record = deleteManager.find(JpaSmokeRecord.class, "crud-1");
            deleteManager.remove(record);
            deleteTransaction.commit();
        } finally {
            deleteManager.close();
        }
        assertThat(find("crud-1")).isNull();
        assertThat(countById("crud-1")).isZero();
    }

    @Test
    void shouldKeepSessionFactoryOpenForReuseAndCloseCleanly() {
        // SessionFactory 复用契约：多次 open/close EntityManager 不影响 factory 可用
        assertThat(sessionFactory.isOpen()).isTrue();
        for (int i = 0; i < 3; i++) {
            EntityManager entityManager = sessionFactory.createEntityManager();
            assertThat(entityManager.isOpen()).isTrue();
            entityManager.close();
            assertThat(entityManager.isOpen()).isFalse();
        }
        assertThat(sessionFactory.isOpen()).isTrue();
    }

    // ========================= 工具方法 =========================

    /** 在独立事务中持久化实体（提交式）。 */
    private static void persist(JpaSmokeRecord record) {
        EntityManager entityManager = sessionFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(record);
            transaction.commit();
        } finally {
            entityManager.close();
        }
    }

    /** 按主键读取实体（无事务读，读不到返回 {@code null}）。 */
    private static JpaSmokeRecord find(String id) {
        EntityManager entityManager = sessionFactory.createEntityManager();
        try {
            return entityManager.find(JpaSmokeRecord.class, id);
        } finally {
            entityManager.close();
        }
    }

    /** 按主键统计实体数（JPQL 参数化计数）。 */
    private static long countById(String id) {
        EntityManager entityManager = sessionFactory.createEntityManager();
        try {
            return entityManager
                    .createQuery("select count(r) from JpaSmokeRecord r where r.id = :id", Long.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } finally {
            entityManager.close();
        }
    }
}
