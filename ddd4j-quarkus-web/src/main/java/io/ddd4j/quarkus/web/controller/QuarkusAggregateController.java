package io.ddd4j.quarkus.web.controller;

import io.ddd4j.core.contract.Model;
import io.ddd4j.core.contract.Page;
import io.ddd4j.core.contract.R;
import io.ddd4j.quarkus.web.TenantAwareResource;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.io.Serializable;

/**
 * Quarkus JAX-RS 聚合根资源模板（对标 Spring 版 {@code BaseAggregateController}）。
 *
 * <p>业务项目的聚合根 Resource 应继承此类，类型参数：
 * <ul>
 *   <li>{@code M}：聚合根领域模型（{@link Model} 子接口）</li>
 *   <li>{@code Q}：查询参数对象</li>
 *   <li>{@code ID}：主键类型</li>
 * </ul>
 *
 * <p>标准路由：
 * <pre>
 *   GET    /                  - 分页列表
 *   GET    /{id}              - 详情
 *   POST   /                  - 新增
 *   PUT    /{id}              - 修改
 *   DELETE /{id}              - 删除
 *   POST   /{id}:disable      - 禁用（业务行为）
 *   POST   /{id}:enable       - 启用（业务行为）
 * </pre>
 *
 * @param <M>  聚合根模型
 * @param <Q>  查询参数
 * @param <ID> 主键
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public abstract class QuarkusAggregateController<M extends Model, Q, ID extends Serializable>
        extends TenantAwareResource {

    /**
     * 分页查询
     */
    @GET
    public R<Page<M>> page(@QueryParam("pageNum") @DefaultValue("1") int pageNum,
                           @QueryParam("pageSize") @DefaultValue("20") int pageSize,
                           @BeanParam Q query) {
        return R.ok(listPage(pageNum, pageSize, query));
    }

    /**
     * 详情
     */
    @GET
    @Path("/{id}")
    public R<M> getById(@PathParam("id") ID id) {
        return R.ok(detail(id));
    }

    /**
     * 新增
     */
    @POST
    public R<M> create(M model) {
        return R.ok(save(model));
    }

    /**
     * 修改
     */
    @PUT
    @Path("/{id}")
    public R<M> update(@PathParam("id") ID id, M model) {
        return R.ok(modify(id, model));
    }

    /**
     * 删除
     */
    @DELETE
    @Path("/{id}")
    public R<Void> delete(@PathParam("id") ID id) {
        remove(id);
        return R.ok();
    }

    /**
     * 禁用（业务行为）
     */
    @POST
    @Path("/{id}:disable")
    public R<Void> disable(@PathParam("id") ID id) {
        doDisable(id);
        return R.ok();
    }

    /**
     * 启用（业务行为）
     */
    @POST
    @Path("/{id}:enable")
    public R<Void> enable(@PathParam("id") ID id) {
        doEnable(id);
        return R.ok();
    }

    // ========== 抽象方法：业务项目必须实现 ==========

    /**
     * 分页查询（业务实现）
     */
    protected abstract Page<M> listPage(int pageNum, int pageSize, Q query);

    /**
     * 详情查询（业务实现）
     */
    protected abstract M detail(ID id);

    /**
     * 新增（业务实现）
     */
    protected abstract M save(M model);

    /**
     * 修改（业务实现）
     */
    protected abstract M modify(ID id, M model);

    /**
     * 删除（业务实现）
     */
    protected abstract void remove(ID id);

    /**
     * 禁用（业务实现，默认空操作）
     */
    protected void doDisable(ID id) {
        // 业务项目按需覆盖
    }

    /**
     * 启用（业务实现，默认空操作）
     */
    protected void doEnable(ID id) {
        // 业务项目按需覆盖
    }
}
