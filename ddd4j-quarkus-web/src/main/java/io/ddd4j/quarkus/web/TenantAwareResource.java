package io.ddd4j.quarkus.web;

import io.ddd4j.core.contract.R;
import io.ddd4j.core.contract.Page;
import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.function.Function;

/**
 * Quarkus JAX-RS 资源基类：从 Vert.x {@link HttpServerRequest} 解析租户、用户、语言、店铺等请求头。
 * <p>
 * 对标 ddd4j-web 的 {@code BaseResource}（Spring 拦截器方案），Quarkus 轨道采用 JAX-RS + Vert.x 方案。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Path("/api/users")
 * public class UserResource extends TenantAwareResource {
 *
 *     @GET
 *     public R<Page<UserVo>> list() {
 *         String tenantId = getTenantId();
 *         String lang = getLang();
 *         return R.ok(userService.list(tenantId, lang));
 *     }
 * }
 * }</pre>
 */
public class TenantAwareResource {

    @Context
    HttpServerRequest request;

    /**
     * 当前请求用户 ID：优先 {@code uid}，否则 {@code admin-id}（后台场景）。
     *
     * @return 用户 ID，可能为空
     */
    protected String getUid() {
        String uid = request.getHeader("uid");
        if (isEmpty(uid)) {
            uid = request.getHeader("admin-id");
        }
        return uid;
    }

    /**
     * 解析路径或业务中的「当前用户」：当 default 为空或为 {@code me} 时使用请求头 {@code uid}。
     *
     * @param defaultUid 默认 uid，可为 {@code me} 表示当前登录用户
     * @return 用户 ID
     */
    protected String getUid(String defaultUid) {
        if (isEmpty(defaultUid) || "me".equals(defaultUid)) {
            return request.getHeader("uid");
        }
        return defaultUid;
    }

    /**
     * 从请求头解析租户 ID：依次尝试 {@code site}、{@code tenant-id}、{@code tenant_id}、{@code tenantId}。
     *
     * @return 租户 ID，可能为空
     */
    protected String getTenantId() {
        String site = request.getHeader("site");
        if (isEmpty(site)) site = request.getHeader("tenant-id");
        if (isEmpty(site)) site = request.getHeader("tenant_id");
        if (isEmpty(site)) site = request.getHeader("tenantId");
        return site;
    }

    /**
     * 租户 ID：若 default 非空则直接返回，否则从请求头解析。
     *
     * @param defaultId 显式指定的租户 ID，可为空
     * @return 租户 ID
     */
    protected String getTenantId(String defaultId) {
        return isEmpty(defaultId) ? getTenantId() : defaultId;
    }

    /**
     * 当前店铺 ID：支持 {@code shop-id} / {@code shopId}，过滤 null、-1 等无效值。
     *
     * @return 店铺 ID，无则空字符串
     */
    protected String getShopId() {
        String shopId = request.getHeader("shop-id");
        if (isEmpty(shopId)) shopId = request.getHeader("shopId");
        if ("null".equalsIgnoreCase(shopId)) return "";
        return isEmpty(shopId) || "-1".equals(shopId) ? "" : shopId;
    }

    /**
     * 从 Accept-Language 解析当前语言代码。
     *
     * @return 语言标识（经 {@link WebUtils#getLang} 归一化）
     */
    protected String getLang() {
        return WebUtils.getLang(request);
    }

    /**
     * 按当前请求语言对文案 key 做国际化。
     *
     * @param key        文案 key
     * @param parameters 占位参数
     * @return 翻译后的字符串
     */
    protected String i18n(String key, Object... parameters) {
        return WebUtils.i18n(getLang(), key, parameters);
    }

    // ========== Response 构建快捷方法 ==========

    /** HTTP 200 + R.ok(data) */
    protected Response ok(Object data) {
        return Response.ok(R.ok(data)).build();
    }

    /** HTTP 200 + R.ok() */
    protected Response ok() {
        return Response.ok(R.ok()).build();
    }

    /** HTTP 200 + R.fail(code, message) */
    protected Response fail(int code, String message) {
        return Response.ok(R.fail(code, message)).build();
    }

    /** HTTP 200 + R.fail(message) */
    protected Response fail(String message) {
        return Response.ok(R.fail(message)).build();
    }

    /** HTTP 200 + 404 语义 */
    protected Response notFound(String message) {
        return Response.ok(R.fail(404, message)).build();
    }

    /** HTTP 200 + 401 语义 */
    protected Response unauthorized(String message) {
        return Response.ok(R.fail(401, message)).build();
    }

    /** HTTP 200 + 400 语义 */
    protected Response badRequest(String message) {
        return Response.ok(R.fail(400, message)).build();
    }

    /** HTTP 200 + 500 语义 */
    protected Response serverError(String message) {
        return Response.ok(R.fail(500, message)).build();
    }

    // ========== 分页便捷方法 ==========

    /** 将分页记录映射转换 */
    protected <T, R> Page<R> transfer(Page<T> page, Function<T, R> func) {
        List<R> results = page.getRecords().stream().map(func).toList();
        Page<R> p = new Page<>();
        p.setRecords(results);
        p.setTotal(page.getTotal());
        p.setCurrent(page.getCurrent());
        p.setSize(page.getSize());
        return p;
    }

    // ========== 内部工具 ==========

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
