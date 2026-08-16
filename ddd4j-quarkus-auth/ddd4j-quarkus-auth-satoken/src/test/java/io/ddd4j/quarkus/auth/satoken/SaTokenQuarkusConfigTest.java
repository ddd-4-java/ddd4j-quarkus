package io.ddd4j.quarkus.auth.satoken;

import cn.dev33.satoken.exception.SaTokenException;
import io.ddd4j.auth.satoken.subject.SaTokenSubjectProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Ddd4jSaTokenQuarkusConfig} CDI 装配测试。
 *
 * <p>验证：
 * <ul>
 *   <li>{@code subjectProvider()} 以 @Produces 暴露为 CDI Bean（可注入且为 SaTokenSubjectProvider）</li>
 *   <li>启动时写回 {@link SubjectKit} 全局注册中心</li>
 *   <li>{@link Ddd4jSaTokenQuarkusConfig.SaTokenExceptionMapper} 映射 401</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@QuarkusTest
class SaTokenQuarkusConfigTest {

    @Inject
    SubjectProvider subjectProvider;

    @Inject
    Ddd4jSaTokenQuarkusConfig.SaTokenExceptionMapper saTokenExceptionMapper;

    @Test
    void subjectProviderExposedAsCdiBeanAndRegisteredInSubjectKit() {
        assertThat(subjectProvider).isInstanceOf(SaTokenSubjectProvider.class);
        assertThat(SubjectKit.subjectProvider).isSameAs(subjectProvider);
    }

    @Test
    void saTokenExceptionMappedToUnauthorized() {
        var response = saTokenExceptionMapper.toResponse(new SaTokenException("token invalid"));
        assertThat(response.getStatus()).isEqualTo(401);
    }
}
