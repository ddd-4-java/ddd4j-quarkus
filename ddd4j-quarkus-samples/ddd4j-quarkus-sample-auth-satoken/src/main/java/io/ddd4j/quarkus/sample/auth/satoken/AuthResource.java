package io.ddd4j.quarkus.sample.auth.satoken;

import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.core.auth.AuthRequest;
import io.ddd4j.core.util.SubjectKit;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.Map;

/**
 * 鉴权示例资源：演示 SubjectKit 统一鉴权入口（sa-token 底层，JAX-RS 端点）。
 *
 * <p>本资源的业务逻辑与 Spring Boot / Javalin 示例完全一致，
 * 证明切换底层框架时业务代码零改动（仅端点声明方式不同）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    /**
     * 登录：SubjectKit.login(AuthRequest)
     */
    @POST
    @Path("/login")
    public Map<String, Object> login(String userId) {
        AuthPrincipal principal = new AuthPrincipal()
                .setLoginId(userId)
                .setUserId(userId)
                .setRoleCode("user");

        AuthRequest request = AuthRequest.of(userId).setTimeout(7200);
        request.setPrincipal(principal);
        String token = SubjectKit.login(request);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("principal", principal);
        return result;
    }

    /**
     * 登出：SubjectKit.logout()
     */
    @POST
    @Path("/logout")
    public Map<String, Object> logout() {
        SubjectKit.logout();
        return Map.of("success", true);
    }

    /**
     * 当前用户：SubjectKit.getPrincipal()
     */
    @GET
    @Path("/me")
    public Map<String, Object> me() {
        AuthPrincipal principal = SubjectKit.getPrincipal();
        if (principal == null) {
            return Map.of("authenticated", false);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("authenticated", true);
        result.put("loginId", principal.getLoginId());
        result.put("userId", principal.getUserId());
        result.put("roleCode", principal.getRoleCode());
        return result;
    }

    /**
     * 权限校验：SubjectKit.hasPermission()
     */
    @GET
    @Path("/check/permission")
    public Map<String, Object> checkPermission(String permission) {
        boolean has = SubjectKit.hasPermission(permission);
        return Map.of("permission", permission, "has", has);
    }

    /**
     * 角色校验：SubjectKit.hasRole()
     */
    @GET
    @Path("/check/role")
    public Map<String, Object> checkRole(String role) {
        boolean has = SubjectKit.hasRole(role);
        return Map.of("role", role, "has", has);
    }

    /**
     * 登录状态：SubjectKit.isLogin()
     */
    @GET
    @Path("/status")
    public Map<String, Object> status() {
        return Map.of("login", SubjectKit.isLogin());
    }

}
