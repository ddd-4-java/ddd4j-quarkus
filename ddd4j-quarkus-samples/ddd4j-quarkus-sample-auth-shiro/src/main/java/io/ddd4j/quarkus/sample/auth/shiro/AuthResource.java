package io.ddd4j.quarkus.sample.auth.shiro;

import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.core.auth.AuthRequest;
import io.ddd4j.core.util.SubjectKit;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 鉴权示例资源：演示 SubjectKit 统一鉴权入口（Shiro 底层）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Path("/auth")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    SampleShiroRuntime runtime;

    @Context
    HttpHeaders headers;

    @AroundInvoke
    Object withSession(InvocationContext invocation) throws Exception {
        return runtime.invoke(invocation, headers.getHeaderString("X-Session-Id"));
    }

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

    @POST
    @Path("/logout")
    public Map<String, Object> logout() {
        SubjectKit.logout();
        return Map.of("success", true);
    }

    @GET
    @Path("/me")
    public Map<String, Object> me() {
        AuthPrincipal principal = SubjectKit.getPrincipal();
        if (Objects.isNull(principal)) {
            return Map.of("authenticated", false);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("authenticated", true);
        result.put("loginId", principal.getLoginId());
        result.put("userId", principal.getUserId());
        return result;
    }

    @GET
    @Path("/check/permission")
    public Map<String, Object> checkPermission(@QueryParam("permission") String permission) {
        boolean has = SubjectKit.hasPermission(permission);
        return Map.of("permission", permission, "has", has);
    }

    @GET
    @Path("/check/role")
    public Map<String, Object> checkRole(@QueryParam("role") String role) {
        boolean has = SubjectKit.hasRole(role);
        return Map.of("role", role, "has", has);
    }

    @GET
    @Path("/status")
    public Map<String, Object> status() {
        return Map.of("login", SubjectKit.isLogin());
    }

    /**
     * 主动触发异常，用于演示 Shiro SubjectThreadState 在失败路径也会恢复。
     */
    @GET
    @Path("/fail")
    public void fail() {
        throw new IllegalStateException("sample auth request failed");
    }

}
