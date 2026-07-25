package io.ddd4j.quarkus.data.logs;

import io.ddd4j.data.logs.ApiOperationLogProvider;
import io.ddd4j.data.logs.DefaultApiOperationLogProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * ddd4j-quarkus API 操作日志 CDI 生产者。
 *
 * <p>通过 CDI {@code @Produces} 暴露 ApiOperationLogProvider 到 Quarkus 容器，
 * 替代 Spring Boot 的 auto-config。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jLogsCdiProducer {

    @Produces
    @ApplicationScoped
    public ApiOperationLogProvider apiOperationLogProvider() {
        return new DefaultApiOperationLogProvider();
    }

}
