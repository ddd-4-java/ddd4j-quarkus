package io.ddd4j.quarkus.validation;

import io.ddd4j.extension.validation.FileContentCheckProvider;
import io.ddd4j.extension.validation.FileValidationService;
import io.ddd4j.extension.validation.OfficeFileTypeDetector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * 提供框架无关文件校验服务的 CDI Bean。
 */
@ApplicationScoped
public class QuarkusFileValidationProducer {

    /**
     * 无参构造函数。
     */
    public QuarkusFileValidationProducer() {
    }

    /**
     * 文件校验服务。
     * @param providers  参数
     * @return 文件校验服务
     */
    @Produces
    @Singleton
    public FileValidationService fileValidationService(Instance<FileContentCheckProvider> providers) {
        List<FileContentCheckProvider> contentCheckProviders = providers.stream().toList();
        return new FileValidationService(new OfficeFileTypeDetector(), contentCheckProviders);
    }
}
