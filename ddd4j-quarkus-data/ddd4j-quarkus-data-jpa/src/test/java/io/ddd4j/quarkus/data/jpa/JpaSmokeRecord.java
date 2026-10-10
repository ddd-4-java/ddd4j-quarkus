package io.ddd4j.quarkus.data.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 容器 round-trip 用 JPA 实体（测试域，对齐 javalin 家族
 * {@code JpaTransactionRecord} 先例）。
 *
 * <p>由 {@link PostgresHibernateContainerIntegrationTest} 以 Hibernate 原生引导
 * （{@code MetadataSources#addAnnotatedClass}）显式注册，不依赖 classpath 扫描，
 * 避免污染 {@code @QuarkusTest} 的 Quarkus ORM 增强路径。
 */
@Entity
@Table(name = "ddd4j_jpa_smoke_record")
public class JpaSmokeRecord {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "val", nullable = false, length = 256)
    private String value;

    /** JPA 规范要求的受保护无参构造。 */
    protected JpaSmokeRecord() {
    }

    /**
     * 全参构造。
     *
     * @param id    主键
     * @param value 业务值
     */
    public JpaSmokeRecord(String id, String value) {
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
