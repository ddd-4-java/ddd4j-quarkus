package io.ddd4j.quarkus.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.ddd4j.quarkus.jackson.ser.NullTolerantBeanSerializerModifier;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.ConfigProvider;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * ddd4j-jackson ObjectMapper 定制器（对应 boot 的 {@code DefaultJacksonAutoConfiguration}）。
 *
 * <p>boot 版通过 {@code Jackson2ObjectMapperBuilderCustomizer + @Primary ObjectMapper} 装配；
 * Quarkus 无等价 builder，官方推荐方式是实现 {@link ObjectMapperCustomizer} —— Quarkus 会在
 * 构建期自动发现该类并应用到运行时 ObjectMapper（含 REST / JSON-B 等场景），无需手工暴露
 * {@code @Produces ObjectMapper}。</p>
 *
 * <p>定制内容与 boot 对齐：
 * <ul>
 *   <li>{@code simpleDateFormat}（日期时间格式）</li>
 *   <li>{@code FAIL_ON_EMPTY_BEANS / FAIL_ON_UNKNOWN_PROPERTIES} 关闭</li>
 *   <li>{@code USE_GETTERS_AS_SETTERS / ALLOW_FINAL_FIELDS_AS_MUTATORS} 开启</li>
 *   <li>注册官方 jsr310 {@link JavaTimeModule} 并覆盖 LocalDateTime/LocalDate/LocalTime
 *       序列化格式（boot 同款）</li>
 *   <li>注册 {@link NullTolerantBeanSerializerModifier}（null 默认序列化策略，6 个开关来自
 *       {@code ddd4j.jackson.default-null-*-serializer}；自包含实现——上游 jackson-extension
 *       3.0.x 已迁 Jackson 3 命名空间，与 Quarkus 的 Jackson 2 不兼容）</li>
 * </ul></p>
 *
 * <p><b>注意</b>：ObjectMapper 在 STATIC_INIT 阶段构建（早于 SmallRye ConfigMapping 注册），
 * 因此此处不注入 {@link JacksonConfig}（构造器/字段注入或 customize 内 Arc 获取都会抛
 * "SRCFG00027: Could not find a mapping"），改为从 MicroProfile Config 直接读取
 * （{@code ddd4j.jackson.*}，键名与 {@link JacksonConfig} 的 ConfigMapping 一致）。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
@IfBuildProperty(name = "ddd4j.jackson.enabled", stringValue = "true", enableIfMissing = true)
public class DefaultJacksonObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        var config = ConfigProvider.getConfig();
        String dateTimePattern = config.getOptionalValue("ddd4j.jackson.date-time-pattern", String.class)
                .orElse("yyyy-MM-dd HH:mm:ss");
        String datePattern = config.getOptionalValue("ddd4j.jackson.date-pattern", String.class)
                .orElse("yyyy-MM-dd");
        String timePattern = config.getOptionalValue("ddd4j.jackson.time-pattern", String.class)
                .orElse("HH:mm:ss");

        objectMapper.setDateFormat(new SimpleDateFormat(dateTimePattern));

        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.enable(MapperFeature.USE_GETTERS_AS_SETTERS);
        objectMapper.enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS);

        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(dateTimePattern)));
        module.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(datePattern)));
        module.addSerializer(LocalTime.class,
                new LocalTimeSerializer(DateTimeFormatter.ofPattern(timePattern)));
        objectMapper.registerModule(module);

        NullTolerantBeanSerializerModifier myBeanSerializerModifier = new NullTolerantBeanSerializerModifier(
                config.getOptionalValue("ddd4j.jackson.default-null-array-serializer", Boolean.class).orElse(true),
                config.getOptionalValue("ddd4j.jackson.default-null-number-serializer", Boolean.class).orElse(false),
                config.getOptionalValue("ddd4j.jackson.default-null-string-serializer", Boolean.class).orElse(true),
                config.getOptionalValue("ddd4j.jackson.default-null-date-serializer", Boolean.class).orElse(true),
                config.getOptionalValue("ddd4j.jackson.default-null-boolean-serializer", Boolean.class).orElse(false),
                config.getOptionalValue("ddd4j.jackson.default-null-json-object-serializer", Boolean.class).orElse(true));
        objectMapper.setSerializerFactory(
                objectMapper.getSerializerFactory().withSerializerModifier(myBeanSerializerModifier));
    }
}
