package io.ddd4j.quarkus.data.panache;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.ddd4j.core.contract.Page;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.GenericGenerator;
import org.jboss.logging.Logger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quarkus Panache 多租户实体基类：使用租户 ID + 雪花 ID 的复合主键（{@link TenantAwareId}），
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
 * // 查询
 * Page<UserAsset> page = UserAsset.search(filters, sorting, 1, 20, UserAsset.class);
 * }</pre>
 */
@MappedSuperclass
@IdClass(TenantAwareId.class)
public abstract class TenantAwareEntity extends PanacheEntityBase {

    private static final Logger logger = Logger.getLogger(TenantAwareEntity.class);

    @Id
    @GeneratedValue(generator = "ddd4j-snowflake")
    @GenericGenerator(name = "ddd4j-snowflake", strategy = "io.ddd4j.quarkus.data.SnowflakeIdGenerator")
    public Long id;

    @Id
    public String tenantId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LocalDateTime createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LocalDateTime updatedTime;

    /**
     * 按过滤条件与排序查询列表（不分页）。
     *
     * @param filters 查询条件（由 {@link RepositoryUtil#formQuery} 解析）
     * @param sorting 排序字段列表
     * @param c       实体类型
     * @param <T>     实体泛型
     * @return 实体列表
     */
    public static <T extends TenantAwareEntity> List<T> list(Map<String, Object> filters, List<String> sorting, Class<T> c) {
        Map<String, Object> params = new HashMap<>();
        String query = RepositoryUtil.formQuery(filters, params);
        query = "from " + c.getSimpleName() + " where " + query;
        Sort sort = RepositoryUtil.from(sorting);
        logger.infof("query: %s, sort: %s, filters:%s", query, sorting, filters);
        return find(query, sort, params).list();
    }

    /**
     * 按过滤条件分页查询。
     *
     * @param filters  查询条件
     * @param sorting  排序
     * @param page     页码（从 1 开始）
     * @param pageSize 每页条数
     * @param c        实体类型
     * @param <T>      实体泛型
     * @return 分页结果
     */
    public static <T extends TenantAwareEntity> Page<T> search(Map<String, Object> filters, List<String> sorting, long page, long pageSize, Class<T> c) {
        Map<String, Object> params = new HashMap<>();
        String query = RepositoryUtil.formQuery(filters, params);
        query = "from " + c.getSimpleName() + " where " + query;
        Sort sort = RepositoryUtil.from(sorting);
        logger.infof("query: %s, sort: %s, filters:%s", query, sorting, filters);
        return toPage(query, params, sort, page, pageSize);
    }

    /**
     * 通用分页：先 count 再取当前页数据。
     */
    public static <T extends TenantAwareEntity> Page<T> toPage(String query, Map<String, Object> params, Sort sort, long page, long pageSize) {
        long total = count(query, params);
        PanacheQuery<T> queryResults = find(query, sort, params);
        List<T> result = queryResults.page((int) (page - 1), (int) pageSize).list();
        Page<T> p = new Page<>();
        p.setRecords(result);
        p.setTotal(total);
        p.setCurrent(page);
        p.setSize(pageSize);
        return p;
    }

    /** 使用 Panache Parameters 的分页重载。 */
    public static <T extends TenantAwareEntity> Page<T> toPage(String query, Parameters params, Sort sort, long page, long pageSize) {
        return toPage(query, params.map(), sort, page, pageSize);
    }

    /**
     * 按租户与用户删除记录。
     *
     * @param tenantId 租户 ID
     * @param uid      用户 ID
     * @return 删除条数
     */
    public static long remove(String tenantId, String uid) {
        return delete("tenantId=:tenantId and uid=:uid", Parameters.with("uid", uid).and("tenantId", tenantId));
    }

    @Override
    public String toString() {
        return String.format("%s<%s:%s>", this.getClass().getSimpleName(), tenantId, id);
    }
}
