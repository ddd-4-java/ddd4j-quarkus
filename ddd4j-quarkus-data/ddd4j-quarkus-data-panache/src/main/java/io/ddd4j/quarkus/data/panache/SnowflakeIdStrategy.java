package io.ddd4j.quarkus.data.panache;

import java.io.Serializable;

/**
 * 雪花 ID 生成策略（编程式入口）。
 *
 * <p>委托 {@link SnowflakeIdGenerator#nextId()}，与实体注解
 * {@code @GenericGenerator(strategy = "...SnowflakeIdGenerator")} 生成的 ID 同源，
 * 保证编程式生成与 ORM 自动生成结果一致。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @see SnowflakeIdGenerator
 * @since 3.3.x
 */
public class SnowflakeIdStrategy implements IdGenerationStrategy<Long> {

    @Override
    public Long generate() {
        return SnowflakeIdGenerator.nextId();
    }
}
