package io.ddd4j.quarkus.auth.security;

import io.ddd4j.auth.security.subject.SecuritySubjectProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * ddd4j-quarkus + Spring Security CDI 整合配置。
 *
 * <p>注册 SecuritySubjectProvider 为 CDI Bean 并写回 SubjectKit。
 *
 * <p>注意：Spring Security 依赖 Spring 生态，Quarkus 环境推荐使用 sa-token。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @deprecated Since 4.1.0. This module is deprecated for removal.
 *             Spring Security requires Spring ecosystem; Quarkus recommends
 *             <code>ddd4j-quarkus-auth-satoken</code> instead.
 *
 *             <p>Migration path:
 *             <ol>
 *               <li>Replace dependency <code>io.ddd4j.quarkus:ddd4j-quarkus-auth-security</code>
 *                   with <code>io.ddd4j.quarkus:ddd4j-quarkus-auth-satoken</code>.</li>
 *               <li>Replace {@code SecuritySubjectProvider} usage with satoken
 *                   {@code SaTokenSubjectProvider} (or implement your own {@link SubjectProvider}).</li>
 *               <li>Remove any spring-security-core imports from your project.</li>
 *             </ol>
 *
 *             <p>See <code>docs/MIGRATION-auth-security-to-satoken.md</code> for details.
 */
@Deprecated(since = "4.1.0", forRemoval = true)
@ApplicationScoped
public class Ddd4jSecurityQuarkusConfig {

    /**
     * @deprecated use {@code ddd4j-quarkus-auth-satoken} module
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Produces
    @Singleton
    public SubjectProvider subjectProvider() {
        SecuritySubjectProvider provider = new SecuritySubjectProvider();
        SubjectKit.register(provider);
        return provider;
    }

}
