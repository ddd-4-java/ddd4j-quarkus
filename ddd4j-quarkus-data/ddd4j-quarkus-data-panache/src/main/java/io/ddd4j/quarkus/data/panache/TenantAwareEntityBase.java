package io.ddd4j.quarkus.data.panache;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.ddd4j.core.api.Page;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.MappedSuperclass;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多租户实体通用基类（灵活复合主键版）。
 *
 * <p>与 {@link TenantAwareEntity}（固定 {@code 租户ID + 雪花ID} 两字段复合主键）不同，
 * 本类 <b>不声明</b> {@code @IdClass}，由子类自行定义主键字段与 {@code @IdClass}，
 * 以支持任意复合主键（如 cloud-das 的 {@code (tenantId, uid, utype, assetType)} 四字段主键）。
 *
 * <p>本类仅提供：
 * <ul>
 *   <li>审计字段 {@code createdTime} / {@code updatedTime}</li>
 *   <li>通用列表查询 {@link #list}、分页查询 {@link #search} / {@link #toPage}、
 *       按租户+用户删除 {@link #remove} 等模板方法（基于 {@link RepositoryUtil}）</li>
 * </ul>
 *
 * <p><b>注意</b>：本类不声明主键字段（包括 {@code tenantId}），
 * 由子类根据自身复合主键需求自行声明并标注 {@code @Id}。
 *
 * <h2>使用示例（对标 cloud-das 的 UserAssetBaseEntity 四字段主键）</h2>
 * <pre>{@code
 * @Entity
 * @IdClass(UserAssetId.class) // 业务自定义四字段主键类
 * public class UserAsset extends TenantAwareEntityBase {
 *
 *     @Id
 *     public String uid;
 *
 *     @Id
 *     public Integer utype;
 *
 *     @Id
 *     public Long assetType;
 *
 *     // tenantId 已由基类提供，子类用 @Id 标注即可（通过 @AttributeOverride 或直接 @Column 映射）
 *     public String name;
 *     public double current;
 * }
 *
 * // 查询：与 TenantAwareEntity 用法一致
 * Page<UserAsset> page = UserAsset.search(filters, sorting, 1, 20, UserAsset.class);
 * }</pre>
 *
 * <h2>与 {@link TenantAwareEntity} 的选择</h2>
 * <ul>
 *   <li>主键 = {@code (tenantId, Long id)}：直接用 {@link TenantAwareEntity}（自带雪花 ID）</li>
 *   <li>主键 = 任意复合主键：继承本类 {@link TenantAwareEntityBase}，自行声明 {@code @IdClass}</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @see TenantAwareEntity
 * @see RepositoryUtil
 * @since 3.3.x
 */
@MappedSuperclass
public abstract class TenantAwareEntityBase extends PanacheEntityBase {

    private static final Logger logger = Logger.getLogger(TenantAwareEntityBase.class);

    /**
     * 创建时间（审计字段）。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LocalDateTime createdTime;

    /**
     * 更新时间（审计字段）。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LocalDateTime updatedTime;

    /**
     * 按过滤条件与排序查询列表（不分页）。
     *
     * @param filters 查询条件（由 {@link RepositoryUtil#formQuery} 解析键前缀）
     * @param sorting 排序字段列表（{@code +field} 升序 / {@code -field} 降序 / {@code field} 默认升序）
     * @param c       实体类型
     * @param <T>     实体泛型
     * @return 实体列表
     */
    public static <T extends TenantAwareEntityBase> List<T> list(Map<String, Object> filters, List<String> sorting, Class<T> c) {
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
    public static <T extends TenantAwareEntityBase> Page<T> search(Map<String, Object> filters, List<String> sorting, long page, long pageSize, Class<T> c) {
        Map<String, Object> params = new HashMap<>();
        String query = RepositoryUtil.formQuery(filters, params);
        query = "from " + c.getSimpleName() + " where " + query;
        Sort sort = RepositoryUtil.from(sorting);
        logger.infof("query: %s, sort: %s, filters:%s", query, sorting, filters);
        return toPage(query, params, sort, page, pageSize);
    }

    /**
     * 通用分页：先 count 再取当前页数据。
     *
     * @param query    HQL 查询语句（含 {@code from ... where ...}）
     * @param params   命名参数
     * @param sort     排序
     * @param page     页码（从 1 开始）
     * @param pageSize 每页条数
     * @param <T>      实体泛型
     * @return 分页结果
     */
    public static <T extends TenantAwareEntityBase> Page<T> toPage(String query, Map<String, Object> params, Sort sort, long page, long pageSize) {
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

    /**
     * 使用 Panache {@link Parameters} 的分页重载。
     */
    public static <T extends TenantAwareEntityBase> Page<T> toPage(String query, Parameters params, Sort sort, long page, long pageSize) {
        return toPage(query, params.map(), sort, page, pageSize);
    }

    /**
     * 按租户与用户删除记录（适用于含 {@code uid} 字段的实体）。
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
        return String.format("%s", this.getClass().getSimpleName());
    }
}
