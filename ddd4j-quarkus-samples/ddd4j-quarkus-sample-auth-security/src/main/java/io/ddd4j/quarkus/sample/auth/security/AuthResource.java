package io.ddd4j.quarkus.sample.auth.security;

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
 * 鉴权示例资源：演示 SubjectKit 统一鉴权入口（Quarkus Security 底层）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

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
        if (principal == null) {
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
    public Map<String, Object> checkPermission(String permission) {
        boolean has = SubjectKit.hasPermission(permission);
        return Map.of("permission", permission, "has", has);
    }

    @GET
    @Path("/check/role")
    public Map<String, Object> checkRole(String role) {
        boolean has = SubjectKit.hasRole(role);
        return Map.of("role", role, "has", has);
    }

    @GET
    @Path("/status")
    public Map<String, Object> status() {
        return Map.of("login", SubjectKit.isLogin());
    }

}
