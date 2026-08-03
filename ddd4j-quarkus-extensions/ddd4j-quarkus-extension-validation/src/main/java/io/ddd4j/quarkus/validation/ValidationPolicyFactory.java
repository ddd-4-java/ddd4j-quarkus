package io.ddd4j.quarkus.validation;

import io.ddd4j.extension.validation.FileValidationPolicy;

/**
 * 将声明式约束参数转换成通用文件校验策略。
 */
final class ValidationPolicyFactory {

    private ValidationPolicyFactory() {
    }

    static FileValidationPolicy from(ValidFileUpload annotation) {
        return FileValidationPolicy.builder()
                .required(annotation.required())
                .strict(annotation.strict())
                .maxSizeBytes(annotation.maxSizeBytes())
                .allowedExtensions(annotation.extensions())
                .allowedMimeTypes(annotation.mimeTypes())
                .build();
    }
}
