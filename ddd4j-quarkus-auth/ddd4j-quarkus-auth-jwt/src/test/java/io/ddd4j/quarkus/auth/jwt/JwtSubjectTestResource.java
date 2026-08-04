package io.ddd4j.quarkus.auth.jwt;

import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.core.subject.Subject;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * 测试用 JAX-RS 资源：{@code GET /jwt-subject} 需要 {@code admin} 角色。
 *
 * <p>在请求作用域内读取 {@link JwtSubject}（由 {@link Ddd4jJwtQuarkusConfig} 的
 * {@code @RequestScoped} 生产者构建），并验证 {@link SubjectKit#getSubject()} 静态门面
 * 能解析到同一主体。
 */
@Path("/jwt-subject")
@ApplicationScoped
public class JwtSubjectTestResource {

    @Inject
    JwtSubject jwtSubject;

    /**
     * 注入 SubjectProvider 强制触发 {@link Ddd4jJwtQuarkusConfig#subjectProvider()} 单例生产者，
     * 完成 {@link SubjectKit#register}。
     */
    @Inject
    SubjectProvider subjectProvider;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("admin")
    public String current() {
        AuthPrincipal principal = jwtSubject.getPrincipal();
        String roles = principal.getRoles() == null || principal.getRoles().isEmpty()
                ? "" : principal.getRoles().get(0).getRoleCode();

        // 静态门面（SubjectKit）与注入的 provider 应解析到同一请求主体
        Subject kitSubject = SubjectKit.getSubject();
        Object kitLoginId = kitSubject.getPrincipal().getLoginId();
        Object providerLoginId = subjectProvider.getSubject().getPrincipal().getLoginId();

        Object tenantId = principal.getProfile().getOrDefault("tenantId", "");
        return "{\"loginId\":\"" + principal.getLoginId() + "\",\"roles\":\"" + roles
                + "\",\"kitLoginId\":\"" + kitLoginId + "\",\"providerLoginId\":\"" + providerLoginId
                + "\",\"tenantId\":\"" + tenantId + "\",\"hasAdminRole\":" + jwtSubject.hasRole("admin") + "}";
    }
}
