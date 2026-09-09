package io.ddd4j.quarkus.data.panache;

import io.ddd4j.kit.lang.IdKit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 通过上游 IP 缓存模拟 runner 地址，验证真实 Snowflake ID 的节点位。
 */
@ResourceLock("IdKit.LAST_IP")
class SnowflakeWorkerIdTest {

    @Test
    void uses_explicit_zero_worker_id() {
        IdGeneratorProducer producer = new IdGeneratorProducer();
        producer.snowflakeWorkerId = Optional.of(0L);
        assertEquals(0L, (producer.snowflakeStrategy().generate() >>> 12) & 31L);
    }

    @Test
    void rejects_negative_worker_id_when_creating_strategy() {
        assertInvalidWorkerId(-1);
    }

    @Test
    void rejects_worker_id_32_when_creating_strategy() {
        assertInvalidWorkerId(32);
    }

    private void assertInvalidWorkerId(long workerId) {
        IdGeneratorProducer producer = new IdGeneratorProducer();
        producer.snowflakeWorkerId = Optional.of(workerId);
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, producer::snowflakeStrategy);
        assertTrue(failure.getMessage().contains("ddd4j.quarkus.data.snowflake.worker-id"));
    }

    @Test
    void normalizes_runner_ip_115_to_worker_19() throws Exception {
        assertDerivedWorkerId((byte) 115, 19);
    }

    @Test
    void normalizes_runner_ip_171_signed_byte_to_worker_11() throws Exception {
        assertDerivedWorkerId((byte) 171, 11);
    }

    private void assertDerivedWorkerId(byte lastIp, long expectedWorkerId) throws Exception {
        // ddd4j 3.0.x 返回有符号 byte；只替换网络探测缓存，保留真实 ID 生成器。
        Field cachedIp = IdKit.class.getDeclaredField("LAST_IP");
        cachedIp.setAccessible(true);
        byte originalIp = cachedIp.getByte(null);
        try {
            cachedIp.setByte(null, lastIp);
            SnowflakeIdStrategy strategy = new SnowflakeIdStrategy();
            long first = assertDoesNotThrow(strategy::generate);
            long second = assertDoesNotThrow(strategy::generate);
            assertEquals(expectedWorkerId, (first >>> 12) & 31L);
            assertEquals(expectedWorkerId, (second >>> 12) & 31L);
            assertTrue(second > first);
        } finally {
            cachedIp.setByte(null, originalIp);
        }
    }
}
