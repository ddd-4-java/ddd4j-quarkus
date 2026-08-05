package io.ddd4j.quarkus.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DefaultJacksonObjectMapperCustomizer} Quarkus 集成测试：
 * 验证 customizer 对 Quarkus ObjectMapper 的定制生效（日期格式、
 * 未知字段容错、空 Bean 序列化等）。
 */
@QuarkusTest
class JacksonQuarkusTest {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    JacksonConfig config;

    @Test
    void configMappingShouldBeResolvable() {
        // 直接注入 ConfigMapping（排除 customizer 构造器链路的干扰）
        assertNotNull(config);
    }

    @Test
    void customizerShouldApply() {
        assertNotNull(objectMapper);
        assertNotNull(config);
        // 定制项：禁用 FAIL_ON_EMPTY_BEANS（空 Bean 可序列化）
        assertFalse(objectMapper.getSerializationConfig()
                .isEnabled(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS));
        // 定制项：禁用 FAIL_ON_UNKNOWN_PROPERTIES（未知字段容错）
        assertFalse(objectMapper.getDeserializationConfig()
                .isEnabled(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    @Test
    void dateTimeShouldUseConfiguredPattern() throws Exception {
        LocalDateTime dateTime = LocalDateTime.of(2026, 8, 5, 10, 30, 0);
        String json = objectMapper.writeValueAsString(dateTime);
        assertTrue(json.contains("2026-08-05 10:30:00"), "实际输出: " + json);
    }

    @Test
    void unknownPropertyShouldBeTolerated() throws Exception {
        // FAIL_ON_UNKNOWN_PROPERTIES 已禁用：反序列化未知字段不抛异常
        Map<String, Object> map = objectMapper.readValue("{\"known\":1,\"unknown\":2}", Map.class);
        assertEquals(1, map.get("known"));
    }
}
