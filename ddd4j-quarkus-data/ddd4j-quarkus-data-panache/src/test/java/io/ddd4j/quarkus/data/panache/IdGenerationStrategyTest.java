package io.ddd4j.quarkus.data.panache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link IdGenerationStrategy} 及其内置实现的纯单元测试。
 *
 * <p>不依赖 Panache/Hibernate 运行时，仅验证策略语义：
 * <ul>
 *   <li>{@link SnowflakeIdStrategy}：生成正数 Long，且两次调用递增</li>
 *   <li>{@link AutoIncrementIdStrategy}：返回 null（表示由 DB 填充）</li>
 *   <li>{@link UuidIdStrategy}：返回 32 位无横线 UUID 字符串，且每次唯一</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class IdGenerationStrategyTest {

    @Test
    void snowflake_generates_positive_unique_long() {
        SnowflakeIdStrategy strategy = new SnowflakeIdStrategy();
        Long first = strategy.generate();
        Long second = strategy.generate();

        assertNotNull(first);
        assertNotNull(second);
        assertTrue(first > 0, "雪花 ID 应为正数");
        assertNotEquals(first, second, "连续生成的雪花 ID 应不同");
    }

    @Test
    void auto_increment_returns_null_for_db_fill() {
        AutoIncrementIdStrategy strategy = new AutoIncrementIdStrategy();
        assertNull(strategy.generate(), "自增策略返回 null，由数据库填充");
    }

    @Test
    void uuid_generates_32_char_unique_string() {
        UuidIdStrategy strategy = new UuidIdStrategy();
        String first = strategy.generate();
        String second = strategy.generate();

        assertNotNull(first);
        assertEquals(32, first.length(), "无横线 UUID 应为 32 位");
        assertEquals(-1, first.indexOf("-"), "UUID 不应含横线");
        assertNotEquals(first, second, "连续生成的 UUID 应唯一");
    }

    @Test
    void uuid_is_valid_hex() {
        UuidIdStrategy strategy = new UuidIdStrategy();
        String id = strategy.generate();
        assertTrue(id.matches("[0-9a-f]{32}"), "UUID 应为 32 位十六进制小写");
    }
}
