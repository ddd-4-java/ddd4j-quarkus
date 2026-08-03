package io.ddd4j.quarkus.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 校验 Quarkus REST 上传文件的扩展名、大小、MIME 与真实文件内容。
 */
@Documented
@Constraint(validatedBy = {FileUploadConstraintValidator.class, FileUploadListConstraintValidator.class})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE,
        ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFileUpload {

    String message() default "上传文件不符合安全要求";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String[] extensions() default {"doc", "docx", "xls", "xlsx", "pdf"};

    String[] mimeTypes() default {
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/pdf"
    };

    long maxSizeBytes() default 10L * 1024L * 1024L;

    boolean required() default true;

    boolean strict() default true;
}
