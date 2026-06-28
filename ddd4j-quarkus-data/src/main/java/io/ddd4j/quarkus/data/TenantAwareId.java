package io.ddd4j.quarkus.data;

import java.io.Serializable;
import java.util.Objects;

/**
 * 多租户复合主键值对象，由实体主键与租户标识组成，用于 {@link TenantAwareEntity} 等实体的 {@code @IdClass} 映射。
 * <p>
 * 对标 ddd4j-data 的 {@code BaseRepositoryImpl} 四泛型方案中的主键抽象。
 * </p>
 */
public class TenantAwareId implements Serializable {

    private Long id;
    private String tenantId;

    public TenantAwareId() {
    }

    public TenantAwareId(Long id, String tenantId) {
        this.id = id;
        this.tenantId = tenantId;
    }

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
