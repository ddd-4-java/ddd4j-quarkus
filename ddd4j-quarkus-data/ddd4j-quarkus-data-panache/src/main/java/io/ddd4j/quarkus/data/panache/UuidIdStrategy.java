package io.ddd4j.quarkus.data.panache;

import java.io.Serializable;
import java.util.UUID;

/**
 * UUID 字符串 ID 策略：生成 32 位无横线 UUID。
 *
 * <p>适用于需要全局唯一、不依赖数据库自增、无安全顺序泄露要求的场景。
 * 使用此策略时，实体主键应为 {@code String} 类型：
 * <pre>
 *   @Id
 *   @GeneratedValue(generator = "ddd4j-uuid")
 *   @GenericGenerator(name = "ddd4j-uuid", strategy = "org.hibernate.id.UUIDGenerator")
 *   public String id;
 * </pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class UuidIdStrategy implements IdGenerationStrategy<String> {

    @Override
    public String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
