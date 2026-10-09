package io.ddd4j.quarkus.data.panache;

import java.io.Serializable;
import java.util.Objects;

/**
 * 多租户复合主键值对象，由实体主键与租户标识组成，用于 {@link TenantAwareEntity} 等实体的 {@code @IdClass} 映射。
 * <p>
 * 对标 ddd4j-data 的 {@code BaseRepositoryImpl} 四泛型方案中的主键抽象。
 * </p>
 */
public class TenantAwareId implements Serializable {

    /**
     * 标识。
     */
    private Long id;
    /**
     * 租户标识。
     */
    private String tenantId;

    /**
     * 构造 TenantAwareId 实例。
     */
    public TenantAwareId() {
    }

    /**
     * 构造 TenantAwareId 实例。
     * @param id  标识
     * @param tenantId  租户标识
     */
    public TenantAwareId(Long id, String tenantId) {
        this.id = id;
        this.tenantId = tenantId;
    }

    /**
     * 执行 of 操作。
     * @param id  标识
     * @param tenantId  租户标识
     * @return 方法返回结果
     */
    public static TenantAwareId of(Long id, String tenantId) {
        return new TenantAwareId(id, tenantId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantAwareId that = (TenantAwareId) o;
        return Objects.equals(id, that.id) && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tenantId);
    }
}
