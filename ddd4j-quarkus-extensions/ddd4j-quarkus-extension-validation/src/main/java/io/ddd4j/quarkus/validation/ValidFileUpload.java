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

    /**
     * 消息。
     * @return 消息字符串内容
     */
    String message() default "上传文件不符合安全要求";

    /**
     * 执行 groups 操作。
     * @return 方法返回结果
     */
    Class<?>[] groups() default {};

    /**
     * 执行 payload 操作。
     * @return 方法返回结果
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 执行 extensions 操作。
     * @return 方法返回结果
     */
    String[] extensions() default {"doc", "docx", "xls", "xlsx", "pdf"};

    /**
     * 执行 mimeTypes 操作。
     * @return 方法返回结果
     */
    String[] mimeTypes() default {
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/pdf"
    };

    /**
     * 最大大小Bytes。
     * @return 最大大小Bytes
     */
    long maxSizeBytes() default 10L * 1024L * 1024L;

    /**
     * 执行 required 操作。
     * @return 条件是否满足
     */
    boolean required() default true;

    /**
     * 执行 strict 操作。
     * @return 条件是否满足
     */
    boolean strict() default true;
}
