package io.ddd4j.quarkus.data.panache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * ID 生成策略 CDI 生产者：根据配置项 {@code ddd4j.quarkus.data.id-strategy}
 * 选择并注入对应的 {@link IdGenerationStrategy} Bean。
 *
 * <p>配置项取值：
 * <ul>
 *   <li>{@code snowflake}（默认）—— {@link SnowflakeIdStrategy}</li>
 *   <li>{@code auto-increment} —— {@link AutoIncrementIdStrategy}</li>
 *   <li>{@code uuid} —— {@link UuidIdStrategy}</li>
 * </ul>
 *
 * <p>业务代码注入使用：
 * <pre>
 *   {@code @Inject IdGenerationStrategy<Long> idStrategy;}
 *   Long id = idStrategy.generate();
 * </pre>
 *
 * <p><b>注意</b>：业务项目可自行提供 {@code @Alternative} 的 {@link IdGenerationStrategy} Bean
 * 覆盖此默认生产者，实现完全自定义的 ID 生成逻辑。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class IdGeneratorProducer {

    /**
     * ID 策略配置项名。
     */
    public static final String ID_STRATEGY_CONFIG = "ddd4j.quarkus.data.id-strategy";

    /**
     * 根据配置产出 {@link IdGenerationStrategy} Bean。
     *
     * <p>由于不同策略返回类型不同（Long / String），此处产出泛型擦除后的 Bean。
     * 业务方注入时按实际类型使用（默认 snowflake/auto-increment 为 Long，uuid 为 String）。
     *
     * @return ID 生成策略实现
     */
    @Produces
    @ApplicationScoped
    public IdGenerationStrategy<?> idGenerationStrategy() {
        String strategy = ConfigProvider.getConfig()
                .getOptionalValue(ID_STRATEGY_CONFIG, String.class)
                .orElse("snowflake");
        return switch (strategy.toLowerCase()) {
            case "auto-increment", "auto_increment", "autoincrement" -> new AutoIncrementIdStrategy();
            case "uuid" -> new UuidIdStrategy();
            default -> new SnowflakeIdStrategy();
        };
    }
}
