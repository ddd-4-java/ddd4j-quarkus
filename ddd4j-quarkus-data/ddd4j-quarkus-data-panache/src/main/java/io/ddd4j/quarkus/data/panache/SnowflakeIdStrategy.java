package io.ddd4j.quarkus.data.panache;

import io.ddd4j.kit.lang.IdKit;

/**
 * 雪花 ID 生成策略（编程式入口）。
 *
 * <p>委托 ddd4j 标准工具 {@link IdKit}（基于 Hutool Snowflake 单例）生成分布式 ID，
 * workerId 由本机 IP 末字节自动派生（{@link IdKit#getLastIPAddress()}），
 * 保证同一 JVM 内全局唯一、按时间递增。
 *
 * <p>与实体注解 {@code @GenericGenerator(strategy = "...SnowflakeIdGenerator")}（ORM 自动生成）
 * 的 ID 均来自 Snowflake 算法，结果形态一致；若需两者完全同源，
 * 可让 {@link SnowflakeIdGenerator} 也改为委托 {@link IdKit}。
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   // 编程式生成
 *   Long id = new SnowflakeIdStrategy().generate();
 *
 *   // 或通过 CDI 注入（配合 IdGeneratorProducer + 配置 ddd4j.quarkus.data.id-strategy=snowflake）
 *   @Inject IdGenerationStrategy<Long> strategy;
 *   Long id = strategy.generate();
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @see IdKit#getSnowflake(long)
 * @see SnowflakeIdGenerator
 * @since 3.3.x
 */
public class SnowflakeIdStrategy implements IdGenerationStrategy<Long> {

    @Override
    public Long generate() {
        // workerId 由本机 IP 末字节派生，Hutool 内部以单例持有 Snowflake，避免多实例 ID 冲突
        return IdKit.getSnowflake(IdKit.getLastIPAddress()).nextId();
    }
}
