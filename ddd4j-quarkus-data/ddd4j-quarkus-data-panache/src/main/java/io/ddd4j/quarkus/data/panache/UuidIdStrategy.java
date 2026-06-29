package io.ddd4j.quarkus.data.panache;

import io.ddd4j.kit.lang.IdKit;

import java.io.Serializable;

/**
 * UUID 字符串 ID 策略：生成 32 位无横线 UUID。
 *
 * <p>委托 ddd4j 标准工具 {@link IdKit#simpleUUID()}（Hutool 实现，比 JDK UUID 高性能）。
 * 适用于需要全局唯一、不依赖数据库自增、无安全顺序泄露要求的场景。
 * 使用此策略时，实体主键应为 {@code String} 类型：
 * <pre>
 *   @Id
 *   @GeneratedValue(generator = "ddd4j-uuid")
 *   @GenericGenerator(name = "ddd4j-uuid", strategy = "org.hibernate.id.UUIDGenerator")
 *   public String id;
 * </pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @see IdKit
 * @since 3.3.x
 */
public class UuidIdStrategy implements IdGenerationStrategy<String> {

    @Override
    public String generate() {
        return IdKit.simpleUUID();
    }
}
