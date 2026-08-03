package io.ddd4j.quarkus.validation;

import io.ddd4j.extension.validation.FileValidationResult;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;

/**
 * 统一构造包含失败码的 Bean Validation 约束信息。
 */
final class ConstraintViolationSupport {

    private ConstraintViolationSupport() {
    }

    static boolean accept(FileValidationResult result, String message, ConstraintValidatorContext context) {
        if (result.valid()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        String failure = Objects.isNull(result.failure()) ? "UNKNOWN" : result.failure().name();
        context.buildConstraintViolationWithTemplate(message + " [" + failure + "]")
                .addConstraintViolation();
        return false;
    }
}
