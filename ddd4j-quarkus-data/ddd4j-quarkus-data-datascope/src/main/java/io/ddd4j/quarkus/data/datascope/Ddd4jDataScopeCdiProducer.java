package io.ddd4j.quarkus.data.datascope;

import io.ddd4j.data.datascope.DataScopeProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * ddd4j-quarkus 数据权限 CDI 生产者。
 *
 * <p>通过 CDI {@code @Produces} 暴露 DataScopeProvider 到 Quarkus 容器，
 * 替代 Spring Boot 的 auto-config。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jDataScopeCdiProducer {

    @Produces
    @ApplicationScoped
    public DataScopeProvider dataScopeProvider() {
        return new DataScopeProvider() {
        };
    }

}
