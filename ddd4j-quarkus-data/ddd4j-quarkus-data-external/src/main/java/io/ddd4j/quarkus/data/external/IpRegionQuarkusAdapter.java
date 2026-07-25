package io.ddd4j.quarkus.data.external;

import io.ddd4j.data.external.region.IpRegionTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Quarkus CDI Producer：把主仓 ddd4j-data-external 的 IP 归属地查询 Template 暴露为
 * CDI Bean，让业务侧可以直接 {@code @Inject IpRegionTemplate} 使用。
 *
 * <p>对齐 ddd4j-boot 中的 {@code Ddd4jExternalAutoConfiguration}：
 * - 默认 {@link IpRegionTemplate#none()} 兜底实现（不依赖网络）
 * - 业务项目如需启用 {@code PconlineRegionTemplate}，请自行构造 HttpClient Bean 并在
 *   {@code @Alternative} 或 {@code @DefaultBean} 层面替换本 Producer 输出
 * - 真正的 HTTP 客户端适配应该使用 Quarkus REST Client（{@code quarkus-rest-client-reactive}），
 *   而非 Vert.x WebClient；本适配器仅满足"有 Bean 可注入"的最小契约
 *
 * <p>注意：主仓 {@code PconlineRegionTemplate} 需要 {@code java.net.http.HttpClient}
 * 而非 Spring 的 RestClient，因此无需 Vert.x 适配。
 */
@ApplicationScoped
public class IpRegionQuarkusAdapter {

    /**
     * 暴露兜底 IP 归属地查询 Template（never returns null）。
     */
    @Produces
    @ApplicationScoped
    public IpRegionTemplate ipRegionTemplate() {
        return IpRegionTemplate.none();
    }
}