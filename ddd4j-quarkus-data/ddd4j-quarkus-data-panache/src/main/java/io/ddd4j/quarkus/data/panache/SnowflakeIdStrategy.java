package io.ddd4j.quarkus.data.panache;

import io.ddd4j.kit.lang.IdKit;

/**
 * 雪花 ID 生成策略（编程式入口）。
 *
 * <p>委托 ddd4j 标准工具 {@link IdKit}（基于 Hutool Snowflake 单例）生成分布式 ID，
 * 默认 workerId 由本机 IP 末字节归一化至 0..31（{@link IdKit#getLastIPAddress()}），
 * 也可显式指定。相同 workerId 复用 JVM 内的 Snowflake 实例。
 * 多节点部署必须分配互不重复的 workerId；IP 归一化不保证跨节点唯一。
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

    private final long workerId;

    /**
     * 根据本机 IP 末字节确定节点，兼容有符号 byte 的负值。
     */
    public SnowflakeIdStrategy() {
        this(Math.floorMod(IdKit.getLastIPAddress(), 32));
    }

    /**
     * 创建使用显式节点编号的策略；非法配置立即失败，不进行归一化。
     *
     * @param workerId 节点编号，范围为 0..31
     * @throws IllegalArgumentException 节点编号超出合法范围
     */
    public SnowflakeIdStrategy(long workerId) {
        if (workerId < 0 || workerId > 31) {
            throw new IllegalArgumentException(
                    "ddd4j.quarkus.data.snowflake.worker-id must be between 0 and 31: " + workerId);
        }
        this.workerId = workerId;
    }

    @Override
    public Long generate() {
        // IdKit 按节点复用 Snowflake，保持同一 JVM 内多个策略实例的序列连续。
        return IdKit.getSnowflake(workerId).nextId();
    }
}
