package io.ddd4j.quarkus.data.panache;

import io.ddd4j.kit.lang.IdKit;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 验证显式 worker-id=31 的 CDI 配置与生成 ID 节点位。
 */
@QuarkusTest
@QuarkusTestResource(value = IdGeneratorProducerWorker31QuarkusTest.FallbackIpTestResource.class,
        restrictToAnnotatedClass = true)
@TestProfile(IdGeneratorProducerWorker31QuarkusTest.WorkerProfile.class)
@ResourceLock("IdKit.LAST_IP")
class IdGeneratorProducerWorker31QuarkusTest {

    private static final byte FALLBACK_LAST_IP = (byte) 115;
    private static final long FALLBACK_WORKER_ID = 19L;

    @Inject
    IdGenerationStrategy<Long> idStrategy;

    @Test
    void uses_configured_worker_id_instead_of_deterministic_ip_fallback() {
        assertNotEquals(FALLBACK_WORKER_ID, 31L);
        assertEquals(31L, (idStrategy.generate() >>> 12) & 31L);
    }

    /**
     * 在 Quarkus 创建 CDI 应用前固定 IdKit 的 IP 缓存，并在停止时恢复测试进程状态。
     */
    public static class FallbackIpTestResource implements QuarkusTestResourceLifecycleManager {

        private Field cachedIp;
        private byte originalIp;
        private boolean cachedIpOverridden;

        @Override
        public Map<String, String> start() {
            try {
                cachedIp = IdKit.class.getDeclaredField("LAST_IP");
                cachedIp.setAccessible(true);
                originalIp = cachedIp.getByte(null);
                cachedIp.setByte(null, FALLBACK_LAST_IP);
                cachedIpOverridden = true;
                return Map.of();
            } catch (ReflectiveOperationException exception) {
                restoreCachedIp();
                throw new IllegalStateException("Cannot prepare deterministic IdKit IP fallback", exception);
            }
        }

        @Override
        public void stop() {
            restoreCachedIp();
        }

        private void restoreCachedIp() {
            if (!cachedIpOverridden) {
                return;
            }
            try {
                cachedIp.setByte(null, originalIp);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Cannot restore IdKit IP cache", exception);
            } finally {
                cachedIpOverridden = false;
            }
        }
    }

    /** 测试配置覆盖，保留 snowflake 策略的真实 CDI 装配。 */
    public static class WorkerProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("ddd4j.quarkus.data.snowflake.worker-id", "31");
        }
    }
}
