package io.ddd4j.quarkus.auth.jwt;

import io.ddd4j.core.subject.AuthPrincipal;
import io.ddd4j.core.subject.AuthRequest;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link JwtSubject} 纯单元测试（不启动 Quarkus 容器）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>principal 从 JWT claim（upn/sub）+ HTTP Header 构建</li>
 *   <li>角色校验（基于 JWT groups）</li>
 *   <li>无状态语义：login/logout/kickout 抛 UnsupportedOperationException</li>
 *   <li>无 JWT 的公开端点场景（principal 非空但 loginId 来自 Header）</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class JwtSubjectTest {

    /**
     * 最小可用的 JsonWebToken 桩：按 claim 名返回预设值。
     */
    private static JsonWebToken jwtWith(String upn, Set<String> groups) {
        return new JsonWebToken() {
            @Override
            public String getName() {
                return upn;
            }

            @Override
            public Set<String> getGroups() {
                return groups;
            }

            @Override
            public Set<String> getClaimNames() {
                return Set.of(Claims.upn.name(), Claims.sub.name());
            }

            @Override
            public <T> T getClaim(String claimName) {
                if (Claims.upn.name().equals(claimName)) {
                    return (T) upn;
                }
                if (Claims.sub.name().equals(claimName)) {
                    return (T) upn;
                }
                return null;
            }

            @Override
            public String getRawToken() {
                return upn == null ? null : "mock-token";
            }
        };
    }

    private static JwtSubject.HeaderContext headers(String uid, String tenantId) {
        return name -> {
            if ("uid".equals(name)) return uid == null ? "" : uid;
            if ("tenant-id".equals(name) || "tenantId".equals(name) || "site".equals(name))
                return tenantId == null ? "" : tenantId;
            return "";
        };
    }

    @Test
    void principal_built_from_jwt_upn_claim() {
        JwtSubject subject = new JwtSubject(
                jwtWith("user-1001", Set.of("admin")),
                headers("uid-from-header", "tenant-A"));
        AuthPrincipal principal = subject.getPrincipal();

        assertNotNull(principal);
        assertEquals("user-1001", principal.getLoginId(), "loginId 应优先取 JWT upn claim");
        assertEquals("user-1001", principal.getUserId());
    }

    @Test
    void principal_falls_back_to_header_when_jwt_absent() {
        // 公开端点：无 JWT，uid 来自 HTTP Header（cloud-das 模式）
        JwtSubject subject = new JwtSubject(null, headers("uid-from-header", "tenant-A"));
        AuthPrincipal principal = subject.getPrincipal();

        assertNotNull(principal);
        assertEquals("uid-from-header", principal.getLoginId(), "无 JWT 时 loginId 回退到 Header uid");
        assertFalse(subject.isAuthenticated(), "无 JWT 时未认证");
    }

    @Test
    void tenant_id_written_into_profile() {
        JwtSubject subject = new JwtSubject(
                jwtWith("u1", Set.of("user")),
                headers("u1", "tenant-B"));
        AuthPrincipal principal = subject.getPrincipal();

        assertEquals("tenant-B", principal.getProfile().get("tenantId"));
    }

    @Test
    void has_role_checks_jwt_groups() {
        JwtSubject subject = new JwtSubject(
                jwtWith("u1", Set.of("admin", "operator")),
                headers("u1", "t1"));

        assertTrue(subject.hasRole("admin"));
        assertTrue(subject.hasRole("operator"));
        assertFalse(subject.hasRole("guest"));
        assertTrue(subject.hasAnyRole("guest", "admin"));
        assertFalse(subject.hasAllRole("admin", "guest"));
    }

    @Test
    void has_role_returns_false_without_jwt() {
        JwtSubject subject = new JwtSubject(null, headers("u1", "t1"));
        assertFalse(subject.hasRole("admin"));
    }

    @Test
    void is_authenticated_reflects_jwt_presence() {
        assertTrue(new JwtSubject(jwtWith("u", Set.of()), headers(null, null)).isAuthenticated());
        assertFalse(new JwtSubject(null, headers(null, null)).isAuthenticated());
    }

    @Test
    void stateful_operations_throw_unsupported() {
        JwtSubject subject = new JwtSubject(jwtWith("u", Set.of()), headers(null, null));

        assertThrows(UnsupportedOperationException.class, () -> subject.login(null),
                "login 在无状态 JWT 模式下应抛异常");
        assertThrows(UnsupportedOperationException.class, subject::logout);
        assertThrows(UnsupportedOperationException.class, () -> subject.kickout("u"));
        assertThrows(UnsupportedOperationException.class, subject::refresh);
        assertThrows(UnsupportedOperationException.class, () -> subject.disable("u", 60));
        assertThrows(UnsupportedOperationException.class, () -> subject.untieDisable("u"));
    }

    @Test
    void is_disabled_always_false_in_stateless_mode() {
        JwtSubject subject = new JwtSubject(jwtWith("u", Set.of()), headers(null, null));
        assertFalse(subject.isDisabled("u"), "无状态 JWT 无封禁存储，恒返回 false");
    }

    @Test
    void principal_cached_per_instance() {
        JwtSubject subject = new JwtSubject(jwtWith("u1", Set.of()), headers("u1", "t1"));
        AuthPrincipal first = subject.getPrincipal();
        AuthPrincipal second = subject.getPrincipal();
        assertSame(first, second, "同一 Subject 实例内 principal 应缓存");
    }
}
