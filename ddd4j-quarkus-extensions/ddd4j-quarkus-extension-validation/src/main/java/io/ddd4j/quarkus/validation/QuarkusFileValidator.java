package io.ddd4j.quarkus.validation;

import io.ddd4j.extension.validation.FileValidationPolicy;
import io.ddd4j.extension.validation.FileValidationResult;
import io.ddd4j.extension.validation.FileValidationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.Objects;

/**
 * Quarkus 上传文件的编程式校验入口。
 */
@ApplicationScoped
public class QuarkusFileValidator {

    @Inject
    FileValidationService validationService;

    /**
     * 校验 Quarkus 上传文件。
     *
     * @param fileUpload 上传文件，可以为空
     * @param policy 校验策略
     * @return 校验结果
     */
    public FileValidationResult validate(FileUpload fileUpload, FileValidationPolicy policy) {
        Objects.requireNonNull(policy, "policy must not be null");
        return validationService.validate(Objects.isNull(fileUpload) ? null : new QuarkusFileUploadAdapter(fileUpload),
                policy);
    }
}
