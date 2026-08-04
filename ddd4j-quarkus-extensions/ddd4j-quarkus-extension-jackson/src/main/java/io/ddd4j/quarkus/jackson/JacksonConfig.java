package io.ddd4j.quarkus.jackson;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * ddd4j-quarkus-jackson 配置（绑定 {@code ddd4j.jackson.*}）。
 *
 * <p>对应 boot 版 {@code DefaultJacksonAutoConfiguration} 读取的
 * {@code spring.jackson.default-null-*-serializer} 系列开关（Spring Boot 命名空间），
 * Quarkus 版收敛到 ddd4j 命名空间 {@code ddd4j.jackson.*}，默认值与 boot 完全一致。
 *
 * <p>配置示例（application.properties）：
 * <pre>{@code
 * ddd4j.jackson.enabled=true
 * ddd4j.jackson.default-null-array-serializer=true
 * ddd4j.jackson.default-null-number-serializer=false
 * ddd4j.jackson.default-null-string-serializer=true
 * ddd4j.jackson.default-null-date-serializer=true
 * ddd4j.jackson.default-null-boolean-serializer=false
 * ddd4j.jackson.default-null-json-object-serializer=true
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ConfigMapping(prefix = "ddd4j.jackson")
public interface JacksonConfig {

    /**
     * 是否启用 ddd4j-jackson 定制（总开关，默认 true）。
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * null 数组默认序列化为 {@code []}（默认 true）。
     */
    @WithDefault("true")
    boolean defaultNullArraySerializer();

    /**
     * null 数字默认序列化为 {@code 0}（默认 false）。
     */
    @WithDefault("false")
    boolean defaultNullNumberSerializer();

    /**
     * null 字符串默认序列化为 {@code ""}（默认 true）。
     */
    @WithDefault("true")
    boolean defaultNullStringSerializer();

    /**
     * null 日期默认序列化为空串（默认 true）。
     */
    @WithDefault("true")
    boolean defaultNullDateSerializer();

    /**
     * null 布尔默认序列化为 {@code false}（默认 false）。
     */
    @WithDefault("false")
    boolean defaultNullBooleanSerializer();

    /**
     * null JSON 对象默认序列化为 {@code {}}（默认 true）。
     */
    @WithDefault("true")
    boolean defaultNullJsonObjectSerializer();

    /**
     * LocalDateTime 序列化格式（默认 {@code yyyy-MM-dd HH:mm:ss}，
     * 对应 boot 的 {@code DateFormats.DATE_LONGFORMAT}）。
     */
    @WithDefault("yyyy-MM-dd HH:mm:ss")
    String dateTimePattern();

    /**
     * LocalDate 序列化格式（默认 {@code yyyy-MM-dd}，
     * 对应 boot 的 {@code DatePattern.NORM_DATE_PATTERN}）。
     */
    @WithDefault("yyyy-MM-dd")
    String datePattern();

    /**
     * LocalTime 序列化格式（默认 {@code HH:mm:ss}，
     * 对应 boot 的 {@code DatePattern.NORM_TIME_PATTERN}）。
     */
    @WithDefault("HH:mm:ss")
    String timePattern();
}
