package io.ddd4j.quarkus.data.panache;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证显式 worker-id=31 的 CDI 配置与生成 ID 节点位。
 */
@QuarkusTest
@TestProfile(IdGeneratorProducerWorker31QuarkusTest.WorkerProfile.class)
class IdGeneratorProducerWorker31QuarkusTest {

    @Inject
    IdGenerationStrategy<Long> idStrategy;

    @Test
    void uses_configured_worker_id() {
        assertEquals(31L, (idStrategy.generate() >>> 12) & 31L);
    }

    /** 测试配置覆盖，保留 snowflake 策略的真实 CDI 装配。 */
    public static class WorkerProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("ddd4j.quarkus.data.snowflake.worker-id", "31");
        }
    }
}
