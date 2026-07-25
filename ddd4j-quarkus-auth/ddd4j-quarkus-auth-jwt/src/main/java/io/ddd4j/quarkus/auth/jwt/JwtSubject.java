package io.ddd4j.quarkus.auth.jwt;

import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.core.auth.AuthRequest;
import io.ddd4j.core.subject.Subject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 基于 SmallRye JWT 的无状态 {@link Subject} 实现。
 *
 * <p>认证主体（{@link AuthPrincipal}）的构建策略（按 cloud-das 实际技术栈对齐）：
 * <ul>
 *   <li>{@code loginId} / {@code userId}：优先从 JWT claim（{@code upn}/{@code sub}/{@code uid}）获取，
 *       兜底从 HTTP 请求头 {@code uid} 解析（由 {@link JwtSubjectProvider} 注入 HeaderContext）</li>
 *   <li>{@code userType}：从 JWT claim {@code userType} 或 Header {@code utype} 读取</li>
 *   <li>角色（{@code roles}）：直接取 JWT 的 {@link JsonWebToken#getGroups()}（即 SmallRye JWT 的 groups claim，
 *       对齐 {@code @RolesAllowed}）</li>
 *   <li>权限（{@code perms}）：无状态 JWT 不携带细粒度权限，留空（由业务层自行实现权限校验）</li>
 *   <li>扩展信息（{@code profile}）：JWT 的其余 claim 全部写入 profile，可通过 {@code Subject.getExtra(key)} 访问</li>
 * </ul>
 *
 * <h2>无状态语义</h2>
 * SmallRye JWT 是<strong>纯验证型</strong>（Verify-only），不管理会话生命周期。
 * 因此会话类方法（{@link #login}、{@link #logout}、{@link #kickout}、{@link #refresh}、{@link #disable}、{@link #untieDisable}）
 * 抛出 {@link UnsupportedOperationException}，调用方应改用「签发新 JWT / 缩短有效期 / 黑名单」等无状态方案。
 *
 * <h2>与 satoken/security/shiro 的区别</h2>
 * 这三套是有状态会话型鉴权（管理 Session/Token 存储），本类是无状态验证型，
 * 适用于「JWT Bearer Token + @RolesAllowed + 网关鉴权」的云原生场景（如 cloud-das）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class JwtSubject implements Subject {

    private static final Logger logger = Logger.getLogger(JwtSubject.class);

    /**
     * 当前请求的 JWT（可能为空 —— 公开端点无 JWT）。
     */
    private final JsonWebToken jwt;

    /**
     * 当前请求的 HTTP 头上下文（tenantId/uid 等多租户信息）。
     */
    private final HeaderContext headers;

    /**
     * 已解析并缓存的 principal（每请求一个 Subject 实例，缓存安全）。
     */
    private volatile AuthPrincipal principal;

    public JwtSubject(JsonWebToken jwt, HeaderContext headers) {
        this.jwt = jwt;
        this.headers = headers;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AuthPrincipal> T getPrincipal() {
        AuthPrincipal result = principal;
        if (result == null) {
            synchronized (this) {
                result = principal;
                if (result == null) {
                    result = principal = buildPrincipal();
                }
            }
        }
        return (T) result;
    }

    @Override
    public <T extends AuthPrincipal> T getPrincipalByLoginId(Object loginId) {
        // 无状态：无法按 loginId 反查历史 principal，仅返回当前请求的 principal
        return getPrincipal();
    }

    @Override
    public <T extends AuthPrincipal> T getPrincipalByToken(String tokenValue) {
        // 无状态：无法按 token 反查（token 验证由 SmallRye JWT 容器完成）
        return getPrincipal();
    }

    @Override
    public boolean isPermitted(String permission) {
        // JWT 不携带细粒度权限，业务层自行实现（如查库 / RBAC 表）
        AuthPrincipal p = getPrincipal();
        return p != null && p.getPerms() != null && p.getPerms().contains(permission);
    }

    @Override
    public boolean isPermitted(Object loginId, String permission) {
        return isPermitted(permission);
    }

    @Override
    public boolean[] isPermitted(String... permissions) {
        boolean[] results = new boolean[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            results[i] = isPermitted(permissions[i]);
        }
        return results;
    }

    @Override
    public boolean[] isPermitted(Object loginId, String... permissions) {
        return isPermitted(permissions);
    }

    @Override
    public boolean isPermittedAny(String... permissions) {
        for (String p : permissions) {
            if (isPermitted(p)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPermittedAny(Object loginId, String... permissions) {
        return isPermittedAny(permissions);
    }

    @Override
    public boolean isPermittedAll(String... permissions) {
        for (String p : permissions) {
            if (!isPermitted(p)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isPermittedAll(Object loginId, String... permissions) {
        return isPermittedAll(permissions);
    }

    @Override
    public boolean hasRole(String roleIdentifier) {
        if (jwt == null || roleIdentifier == null) {
            return false;
        }
        return jwt.getGroups() != null && jwt.getGroups().contains(roleIdentifier);
    }

    @Override
    public boolean hasRole(Object loginId, String roleIdentifier) {
        return hasRole(roleIdentifier);
    }

    @Override
    public boolean[] hasRoles(String... roleIdentifiers) {
        boolean[] results = new boolean[roleIdentifiers.length];
        for (int i = 0; i < roleIdentifiers.length; i++) {
            results[i] = hasRole(roleIdentifiers[i]);
        }
        return results;
    }

    @Override
    public boolean[] hasRoles(Object loginId, String... roleIdentifiers) {
        return hasRoles(roleIdentifiers);
    }

    @Override
    public boolean hasAnyRole(String... roleIdentifiers) {
        for (String r : roleIdentifiers) {
            if (hasRole(r)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnyRole(Object loginId, String... roleIdentifiers) {
        return hasAnyRole(roleIdentifiers);
    }

    @Override
    public boolean hasAllRole(String... roleIdentifiers) {
        for (String r : roleIdentifiers) {
            if (!hasRole(r)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasAllRole(Object loginId, String... roleIdentifiers) {
        return hasAllRole(roleIdentifiers);
    }

    @Override
    public boolean isAuthenticated() {
        return jwt != null && jwt.getRawToken() != null;
    }

    @Override
    public boolean isAuthenticated(Object loginId) {
        return isAuthenticated();
    }

    @Override
    public boolean isRemembered() {
        // JWT 无 remember-me 概念
        return false;
    }

    @Override
    public boolean isTrustDeviceId(String deviceId) {
        return false;
    }

    @Override
    public boolean isTrustDeviceId(Object userId, String deviceId) {
        return false;
    }

    // ==================== 会话生命周期（无状态 JWT 不支持，抛 UnsupportedOperationException）====================

    @Override
    public String login(AuthRequest request) {
        throw new UnsupportedOperationException(
                "JwtSubject is stateless; issue a new JWT at the auth endpoint instead of calling login()");
    }

    @Override
    public void logout() {
        throw new UnsupportedOperationException(
                "JwtSubject is stateless; shorten JWT TTL or maintain a blacklist to implement logout()");
    }

    @Override
    public void logout(Object loginId) {
        logout();
    }

    @Override
    public void kickout(Object loginId) {
        throw new UnsupportedOperationException(
                "JwtSubject is stateless; use a token blacklist to implement kickout()");
    }

    @Override
    public String refresh() {
        throw new UnsupportedOperationException(
                "JwtSubject is stateless; issue a new JWT with extended TTL to implement refresh()");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AuthPrincipal> T verify(String token) {
        // JWT 验证由 SmallRye JWT 容器在请求入口完成，业务层无需重复验证
        // 此处返回当前已验证的 principal（若 token 已被容器验证并注入）
        return getPrincipal();
    }

    @Override
    public void disable(Object loginId, long timeout) {
        throw new UnsupportedOperationException(
                "JwtSubject is stateless; use a token blacklist or short TTL to implement disable()");
    }

    @Override
    public boolean isDisabled(Object loginId) {
        // 无状态：无封禁存储，返回 false（业务可自行查询黑名单）
        return false;
    }

    @Override
    public void untieDisable(Object loginId) {
        throw new UnsupportedOperationException(
                "JwtSubject is stateless; remove from token blacklist to implement untieDisable()");
    }

    // ==================== 构建 AuthPrincipal ====================

    /**
     * 从 JWT claim + HTTP Header 构建认证主体。
     */
    private AuthPrincipal buildPrincipal() {
        AuthPrincipal p = new AuthPrincipal();

        // 1. loginId / userId：优先 JWT claim，兜底 HTTP Header（cloud-das 模式）
        Object loginId = resolveClaimOrHeader(new String[]{"upn", "sub", "uid"}, "uid");
        p.setLoginId(loginId);
        p.setUserId(loginId);

        // 2. userType
        Object userType = resolveClaimOrHeader(new String[]{"userType", "utype"}, "utype");
        if (userType != null) {
            p.setUserType(String.valueOf(userType));
        }

        // 3. 角色：直接取 JWT groups（对齐 @RolesAllowed）
        if (jwt != null && jwt.getGroups() != null) {
            Set<String> roles = new HashSet<>(jwt.getGroups());
            String roleCodes = String.join(",", roles);
            AuthPrincipal.RolePair rolePair = new AuthPrincipal.RolePair();
            rolePair.setRoleId(roleCodes);
            rolePair.setRoleCode(roleCodes);
            rolePair.setRoleName(roleCodes);
            rolePair.setVerify(true);
            p.setRoles(Collections.singletonList(rolePair));
        }

        // 4. 扩展信息：JWT 全部 claim 写入 profile（便于 getExtra 访问）
        if (jwt != null && jwt.getClaimNames() != null) {
            Map<String, Object> profile = p.getProfile();
            for (String name : jwt.getClaimNames()) {
                profile.put(name, jwt.getClaim(name));
            }
        }
        // 同时把 header 的 tenantId 写入 profile（多租户场景常需）
        if (headers != null) {
            String tenantId = headers.getTenantId();
            if (tenantId != null) {
                p.getProfile().put("tenantId", tenantId);
            }
        }
        p.setVerify(true);
        return p;
    }

    /**
     * 按顺序尝试 JWT claim，全部为空则取 HTTP Header。
     */
    private Object resolveClaimOrHeader(String[] claimNames, String headerName) {
        if (jwt != null) {
            for (String name : claimNames) {
                Object value = jwt.getClaim(name);
                if (value != null && !"".equals(value)) {
                    return value;
                }
            }
        }
        if (headers != null) {
            return headers.get(headerName);
        }
        return null;
    }

    /**
     * HTTP 请求头上下文：由 {@link JwtSubjectProvider} 从 Vert.x HttpServerRequest 解析并注入。
     *
     * <p>解耦 Subject 与 JAX-RS/Vert.x API，便于测试。
     */
    public interface HeaderContext {
        /**
         * 按头名取值。
         */
        String get(String name);

        /**
         * 租户 ID（兼容 site/tenant-id/tenant_id/tenantId 多种头名）。
         */
        default String getTenantId() {
            String[] candidates = {"site", "tenant-id", "tenant_id", "tenantId"};
            for (String c : candidates) {
                String v = get(c);
                if (v != null && !v.isEmpty()) {
                    return v;
                }
            }
            return null;
        }
    }
}
