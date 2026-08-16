package io.ddd4j.quarkus.data.panache;

import io.ddd4j.core.api.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.GenericGenerator;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * Quarkus Panache 多租户实体基类（固定主键版）：使用租户 ID + 雪花 ID 的复合主键（{@link TenantAwareId}），
 * 并提供通用列表查询、分页与按租户删除等能力。
 * <p>
 * 对标 ddd4j-data 的 {@code BaseRepositoryImpl}（MyBatis Plus 四泛型方案），
 * Quarkus 轨道采用 Hibernate ORM Panache + JPA {@link IdClass} 方案。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Entity
 * @IdClass(TenantAwareId.class)
 * public class UserAsset extends TenantAwareEntity {
 *
 *     public String name;
 *     public long assetType;
 *     public double current;
 * }
 *
 * // 查询（list/search/toPage/remove 等模板方法继承自 TenantAwareEntityBase）
 * Page<UserAsset> page = UserAsset.search(filters, sorting, 1, 20, UserAsset.class);
 * }</pre>
 *
 * <h2>复合主键灵活性</h2>
 * 若业务需要非 {@code (tenantId, Long id)} 的复合主键（如四字段主键），
 * 请改用 {@link TenantAwareEntityBase}（不在基类声明 {@code @IdClass}，由子类自定义）。
 *
 * @see TenantAwareEntityBase
 * @see TenantAwareId
 */
@MappedSuperclass
@IdClass(TenantAwareId.class)
public abstract class TenantAwareEntity extends TenantAwareEntityBase {

    private static final Logger logger = Logger.getLogger(TenantAwareEntity.class);

    /**
     * 业务主键（雪花 ID 自动生成）。
     */
    @Id
    @GeneratedValue(generator = "ddd4j-snowflake")
    @GenericGenerator(name = "ddd4j-snowflake", strategy = "io.ddd4j.quarkus.data.panache.SnowflakeIdGenerator")
    public Long id;

    /**
     * 租户 ID：本类声明并标注 {@code @Id}，与 {@link #id} 构成固定的两字段复合主键
     * （基类 {@link TenantAwareEntityBase} 不声明主键字段）。
     */
    @Id
    public String tenantId;

    @Override
    public String toString() {
        return String.format("%s<%s:%s>", this.getClass().getSimpleName(), tenantId, id);
    }
}
