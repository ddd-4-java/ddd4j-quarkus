package io.ddd4j.quarkus.data.panache;

import java.io.Serializable;

/**
 * ID 生成策略 SPI：抽象实体主键生成方式，支持业务按需选择或自定义。
 *
 * <p>与 Hibernate 的 {@link org.hibernate.id.IdentifierGenerator}（在实体注解上编译期绑定）不同，
 * 本接口提供 <b>编程式</b> 的 ID 生成能力，适用于：
 * <ul>
 *   <li>实体主键由业务代码生成后显式 set（而非依赖 ORM 自动生成）的场景</li>
 *   <li>需要在持久化前预先生成 ID 用于消息发送、关联设置等</li>
 *   <li>统一多模块的 ID 生成口径</li>
 * </ul>
 *
 * <p>内置实现：
 * <ul>
 *   <li>{@link SnowflakeIdStrategy} —— 雪花算法（时间戳+节点+序列），与 {@link SnowflakeIdGenerator} 一致</li>
 *   <li>{@link AutoIncrementIdStrategy} —— 数据库自增（生成时返回 null，由 DB 填充）</li>
 *   <li>{@link UuidIdStrategy} —— UUID 字符串</li>
 * </ul>
 *
 * <p>配置项 {@code ddd4j.quarkus.data.id-strategy} 选择默认实现（供 {@link IdGeneratorProducer} 注入）：
 * <pre>
 *   ddd4j.quarkus.data.id-strategy=snowflake   # 默认，雪花
 *   ddd4j.quarkus.data.id-strategy=auto-increment
 *   ddd4j.quarkus.data.id-strategy=uuid
 * </pre>
 *
 * @param <T> 主键类型（如 Long / String）
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @see SnowflakeIdGenerator
 * @since 3.3.x
 */
public interface IdGenerationStrategy<T extends Serializable> {

    /**
     * 生成下一个主键值。
     *
     * <p>对于数据库自增策略，返回 {@code null}（由数据库在 insert 时填充）。
     *
     * @return 主键值，或 {@code null}（表示由持久层填充）
     */
    T generate();
}
