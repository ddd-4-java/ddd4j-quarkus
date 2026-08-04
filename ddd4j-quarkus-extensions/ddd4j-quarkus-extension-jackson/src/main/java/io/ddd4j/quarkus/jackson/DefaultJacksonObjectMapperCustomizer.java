package io.ddd4j.quarkus.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import io.github.hiwepy.jackson.JavaTimeModule;
import io.github.hiwepy.jackson.ser.MyBeanSerializerModifier;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
 * {@code @Produces ObjectMapper}。
 *
 * <p>定制内容与 boot 对齐：
 * <ul>
 *   <li>{@code simpleDateFormat}（日期时间格式）</li>
 *   <li>{@code FAIL_ON_EMPTY_BEANS / FAIL_ON_UNKNOWN_PROPERTIES} 关闭</li>
 *   <li>{@code USE_GETTERS_AS_SETTERS / ALLOW_FINAL_FIELDS_AS_MUTATORS} 开启</li>
 *   <li>注册 {@link JavaTimeModule}（hiwepy 权威实现）并覆盖 LocalDateTime/LocalDate/LocalTime
 *       序列化格式（boot 同款）</li>
 *   <li>注册 {@link MyBeanSerializerModifier}（null 默认序列化策略，6 个开关来自
 *       {@code ddd4j.jackson.default-null-*-serializer}）</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
@IfBuildProperty(name = "ddd4j.jackson.enabled", stringValue = "true", enableIfMissing = true)
public class DefaultJacksonObjectMapperCustomizer implements ObjectMapperCustomizer {

    private final JacksonConfig config;

    @Inject
    public DefaultJacksonObjectMapperCustomizer(JacksonConfig config) {
        this.config = config;
    }

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.setDateFormat(new SimpleDateFormat(config.dateTimePattern()));

        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.enable(MapperFeature.USE_GETTERS_AS_SETTERS);
        objectMapper.enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS);

        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(config.dateTimePattern())));
        module.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(config.datePattern())));
        module.addSerializer(LocalTime.class,
                new LocalTimeSerializer(DateTimeFormatter.ofPattern(config.timePattern())));
        objectMapper.registerModule(module);

        MyBeanSerializerModifier myBeanSerializerModifier = new MyBeanSerializerModifier(
                config.defaultNullArraySerializer(),
                config.defaultNullNumberSerializer(),
                config.defaultNullStringSerializer(),
                config.defaultNullDateSerializer(),
                config.defaultNullBooleanSerializer(),
                config.defaultNullJsonObjectSerializer());
        objectMapper.setSerializerFactory(
                objectMapper.getSerializerFactory().withSerializerModifier(myBeanSerializerModifier));
    }
}
