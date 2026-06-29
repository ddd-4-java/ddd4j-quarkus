package io.ddd4j.quarkus.auth.jwt;

import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

/**
 * SmallRye JWT 版 {@link SubjectProvider} CDI 配置。
 *
 * <p>职责：
 * <ul>
 *   <li>产出一个 {@link RequestScoped} 的 {@link JwtSubject}，每请求从 {@link JsonWebToken}（可选）
 *       + Vert.x {@link HttpServerRequest} 解析 Header 构建</li>
 *   <li>注册 {@link JwtSubjectProvider} 为 {@link SubjectProvider}，写回 {@link SubjectKit} 全局注册中心</li>
 *   <li>提供 JWT 验证异常的 JAX-RS ExceptionMapper</li>
 * </ul>
 *
 * <h2>使用方式</h2>
 * <ol>
 *   <li>引入 {@code ddd4j-quarkus-auth-jwt} 依赖（与 satoken/security/shiro 互斥，只选其一）</li>
 *   <li>配置 SmallRye JWT（{@code mp.jwt.verify.publickey} / {@code smallrye.jwt.sign.key-location}）</li>
 *   <li>业务代码通过 {@code SubjectKit.getSubject()} 或 {@code @Inject Subject} 获取当前请求主体</li>
 * </ol>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class Ddd4jJwtQuarkusConfig {

    private static final Logger logger = Logger.getLogger(Ddd4jJwtQuarkusConfig.class);

    /**
     * 产出 {@link SubjectProvider}：每请求创建 {@link JwtSubject} 并写回全局注册中心。
     *
     * <p>使用 {@code @ApplicationScoped} 保证只注册一次到 {@link SubjectKit}，
     * 内部通过 {@link Arc} 容器按需获取当前请求的 {@link JwtSubject}。
     */
    @Produces
    @Singleton
    public SubjectProvider subjectProvider() {
        SubjectProvider provider = new JwtSubjectProvider();
        SubjectKit.register(provider);
        logger.info("Registered JwtSubjectProvider to SubjectKit");
        return provider;
    }

    /**
     * 产出 {@link RequestScoped} 的 {@link JwtSubject}：每请求构建。
     *
     * <p>JWT Bean（{@link JsonWebToken}）由 SmallRye JWT 在请求入口注入（公开端点时为空）；
     * HTTP Header 从 Vert.x {@link HttpServerRequest} 解析。
     */
    @Produces
    @RequestScoped
    public JwtSubject jwtSubject(
            JsonWebToken jwt,
            Instance<HttpServerRequest> requestInstance) {

        JwtSubject.HeaderContext headers = name -> {
            if (requestInstance.isResolvable()) {
                String value = requestInstance.get().getHeader(name);
                return value == null ? "" : value;
            }
            return "";
        };
        return new JwtSubject(jwt, headers);
    }
}
