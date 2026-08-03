package io.ddd4j.quarkus.validation;

import io.ddd4j.extension.validation.FileValidationPolicy;
import io.ddd4j.extension.validation.FileValidationResult;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/**
 * 单个 Quarkus REST 上传文件约束校验器。
 */
public class FileUploadConstraintValidator implements ConstraintValidator<ValidFileUpload, FileUpload> {

    @Inject
    QuarkusFileValidator fileValidator;

    private FileValidationPolicy policy;
    private String message;

    @Override
    public void initialize(ValidFileUpload annotation) {
        this.policy = ValidationPolicyFactory.from(annotation);
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(FileUpload value, ConstraintValidatorContext context) {
        FileValidationResult result = fileValidator.validate(value, policy);
        return ConstraintViolationSupport.accept(result, message, context);
    }
}
