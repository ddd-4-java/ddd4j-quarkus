package io.ddd4j.quarkus.data.panache;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link IdGeneratorProducer} 配置切换测试：{@code ddd4j.quarkus.data.id-strategy=uuid}
 * 时装配的策略应为 UUID（可注入 {@code IdGenerationStrategy<String>}，返回 32 位字符串）。
 *
 * <p>通过 {@link QuarkusTestProfile} 覆盖构建期配置，触发独立应用实例
 * （独立于 {@link IdGeneratorProducerQuarkusTest} 的默认 snowflake 装配）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@QuarkusTest
@TestProfile(IdGeneratorProducerUuidQuarkusTest.UuidStrategyProfile.class)
class IdGeneratorProducerUuidQuarkusTest {

    @Inject
    IdGenerationStrategy<String> idStrategy;

    @Test
    void uuid_strategy_selected_by_config() {
        String id = idStrategy.generate();
        assertEquals(32, id.length(), "无横线 UUID 应为 32 位");
        assertEquals(-1, id.indexOf('-'), "UUID 不应含横线");
    }

    /**
     * 切换 ID 策略为 uuid 的测试 profile。
     */
    public static class UuidStrategyProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(IdGeneratorProducer.ID_STRATEGY_CONFIG, "uuid");
        }
    }
}
