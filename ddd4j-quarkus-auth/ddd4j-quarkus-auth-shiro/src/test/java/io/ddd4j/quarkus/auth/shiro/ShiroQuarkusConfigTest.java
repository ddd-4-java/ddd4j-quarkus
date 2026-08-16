package io.ddd4j.quarkus.auth.shiro;

import io.ddd4j.auth.shiro.subject.ShiroSubjectProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Ddd4jShiroQuarkusConfig} CDI 装配测试。
 *
 * <p>验证：
 * <ul>
 *   <li>{@code subjectProvider()} 以 @Produces 暴露为 CDI Bean（ShiroSubjectProvider）</li>
 *   <li>启动时写回 {@link SubjectKit} 全局注册中心</li>
 *   <li>认证异常 → 401、授权异常 → 403 的 ExceptionMapper 映射</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@QuarkusTest
class ShiroQuarkusConfigTest {

    @Inject
    SubjectProvider subjectProvider;

    @Inject
    Ddd4jShiroQuarkusConfig.ShiroAuthenticationExceptionMapper authenticationMapper;

    @Inject
    Ddd4jShiroQuarkusConfig.ShiroAuthorizationExceptionMapper authorizationMapper;

    @Test
    void subjectProviderExposedAsCdiBeanAndRegisteredInSubjectKit() {
        assertThat(subjectProvider).isInstanceOf(ShiroSubjectProvider.class);
        assertThat(SubjectKit.subjectProvider).isSameAs(subjectProvider);
    }

    @Test
    void authenticationExceptionMappedToUnauthorized() {
        var response = authenticationMapper.toResponse(new AuthenticationException("not logged in"));
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void authorizationExceptionMappedToForbidden() {
        var response = authorizationMapper.toResponse(new AuthorizationException("no permission"));
        assertThat(response.getStatus()).isEqualTo(403);
    }
}
