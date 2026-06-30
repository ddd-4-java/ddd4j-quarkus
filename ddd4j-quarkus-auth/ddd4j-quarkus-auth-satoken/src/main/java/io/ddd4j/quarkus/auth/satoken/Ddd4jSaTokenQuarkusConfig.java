package io.ddd4j.quarkus.auth.satoken;

import cn.dev33.satoken.exception.SaTokenException;
import io.ddd4j.auth.satoken.subject.SaTokenSubjectProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * ddd4j-quarkus + sa-token CDI 整合配置。
 *
 * <p>职责：
 * <ul>
 *   <li>注册 {@link SubjectProvider}（SaTokenSubjectProvider）为 CDI Bean</li>
 *   <li>启动时写回 {@link SubjectKit} 全局注册中心</li>
 *   <li>提供 Sa-Token 异常的 JAX-RS ExceptionMapper</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class Ddd4jSaTokenQuarkusConfig {

    /**
     * 提供 SubjectProvider CDI Bean，同时写回 SubjectKit。
     */
    @Singleton
    public SubjectProvider subjectProvider() {
        SaTokenSubjectProvider provider = new SaTokenSubjectProvider();
        SubjectKit.register(provider);
        return provider;
    }

    /**
     * Sa-Token 异常映射器（JAX-RS ExceptionMapper）。
     */
    @Provider
    @Singleton
    public static class SaTokenExceptionMapper implements ExceptionMapper<SaTokenException> {

        @Override
        public Response toResponse(SaTokenException ex) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("code", ex.getCode(), "msg", ex.getMessage()))
                    .build();
        }
    }

}
