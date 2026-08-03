package io.ddd4j.quarkus.validation;

import io.ddd4j.extension.validation.FileValidationPolicy;
import io.ddd4j.extension.validation.FileValidationResult;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.List;
import java.util.Objects;

/**
 * Quarkus REST 上传文件集合约束校验器。
 */
public class FileUploadListConstraintValidator implements ConstraintValidator<ValidFileUpload, List<FileUpload>> {

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
    public boolean isValid(List<FileUpload> values, ConstraintValidatorContext context) {
        if (Objects.isNull(values) || values.isEmpty()) {
            FileValidationResult result = fileValidator.validate(null, policy);
            return ConstraintViolationSupport.accept(result, message, context);
        }
        for (FileUpload value : values) {
            FileValidationResult result = fileValidator.validate(value, policy);
            if (!ConstraintViolationSupport.accept(result, message, context)) {
                return false;
            }
        }
        return true;
    }
}
