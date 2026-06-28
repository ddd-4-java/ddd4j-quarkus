package io.ddd4j.quarkus.auth.shiro;

import io.ddd4j.auth.shiro.subject.ShiroSubjectProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;

import java.util.Map;

/**
 * ddd4j-quarkus + Apache Shiro CDI 整合配置。
 *
 * <p>职责：
 * <ul>
 *   <li>注册 {@link SubjectProvider}（ShiroSubjectProvider）为 CDI Bean</li>
 *   <li>启动时写回 {@link SubjectKit} 全局注册中心</li>
 *   <li>提供 Shiro 异常的 JAX-RS ExceptionMapper</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class Ddd4jShiroQuarkusConfig {

    @Singleton
    public SubjectProvider subjectProvider() {
        ShiroSubjectProvider provider = new ShiroSubjectProvider();
        SubjectKit.register(provider);
        return provider;
    }

    /**
     * Shiro 认证异常映射器。
     */
    @Provider
    @Singleton
    public static class ShiroAuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException> {
        @Override
        public Response toResponse(AuthenticationException ex) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("code", 401, "msg", "未登录或登录已过期"))
                .build();
        }
    }

    /**
     * Shiro 授权异常映射器。
     */
    @Provider
    @Singleton
    public static class ShiroAuthorizationExceptionMapper implements ExceptionMapper<AuthorizationException> {
        @Override
        public Response toResponse(AuthorizationException ex) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of("code", 403, "msg", "无权限访问"))
                .build();
        }
    }

}
