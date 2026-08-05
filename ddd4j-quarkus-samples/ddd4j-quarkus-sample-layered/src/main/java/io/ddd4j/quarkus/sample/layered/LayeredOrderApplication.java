package io.ddd4j.quarkus.sample.layered;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * 分层架构示例主类（Quarkus 启动入口）。
 *
 * <p>本模块聚合主要分层：<b>client / app / domain / adapter / common</b>，
 * 展示 DDD 分层架构在 Quarkus 下的组装方式：</p>
 *
 * <ol>
 *   <li><b>client</b>（契约层）：{@code OrderClientService}/{@code OrderClientDTO} 对外 API 契约</li>
 *   <li><b>app</b>（应用层）：{@code OrderApplicationService} 用例编排 + {@code OrderMapper} 对象转换</li>
 *   <li><b>domain</b>（领域层）：{@code Order} 聚合根（充血模型）+ {@code OrderStatus} 状态机</li>
 *   <li><b>adapter</b>（适配层）：{@code OrderResource} JAX-RS 资源 + 异常映射器</li>
 *   <li><b>common</b>（通用层）：通用工具/响应封装</li>
 *   <li><b>infrastructure</b>（基础设施层，可选）：仓储持久化实现默认使用
 *       {@code LayeredSampleConfig} 装配的内存版（开箱即跑）；需要 Panache 持久化时
 *       引入 {@code ddd4j-quarkus-sample-infrastructure} 模块并移除内存仓储装配即可</li>
 * </ol>
 *
 * <p>启动后访问 {@code http://localhost:8080/api/orders} 即可体验分层示例
 * （创建/查询/支付/发货/取消/分页）。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusMain
public class LayeredOrderApplication {

    /**
     * Quarkus 启动入口。
     *
     * @param args 启动参数（如 {@code -Dquarkus.http.port=8080}）
     */
    public static void main(String[] args) {
        Quarkus.run(args);
    }
}
