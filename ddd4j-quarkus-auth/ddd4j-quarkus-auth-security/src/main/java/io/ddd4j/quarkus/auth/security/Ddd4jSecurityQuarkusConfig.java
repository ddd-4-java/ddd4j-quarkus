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
 */
@ApplicationScoped
public class Ddd4jSecurityQuarkusConfig {

    @Produces
    @Singleton
    public SubjectProvider subjectProvider() {
        SecuritySubjectProvider provider = new SecuritySubjectProvider();
        SubjectKit.register(provider);
        return provider;
    }

}
