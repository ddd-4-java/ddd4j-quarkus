package io.ddd4j.quarkus.auth.security;

import io.ddd4j.auth.security.subject.SecuritySubjectProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Ddd4jSecurityQuarkusConfig} CDI 装配测试。
 *
 * <p>验证 {@code subjectProvider()} 以 @Produces 暴露为 CDI Bean
 * （SecuritySubjectProvider），并写回 {@link SubjectKit} 全局注册中心。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@QuarkusTest
@Disabled("Module is deprecated since 4.1.0; see docs/MIGRATION-auth-security-to-satoken.md")
class SecurityQuarkusConfigTest {

    @Inject
    SubjectProvider subjectProvider;

    @Test
    void subjectProviderExposedAsCdiBeanAndRegisteredInSubjectKit() {
        assertThat(subjectProvider).isInstanceOf(SecuritySubjectProvider.class);
        assertThat(SubjectKit.subjectProvider).isSameAs(subjectProvider);
    }
}
