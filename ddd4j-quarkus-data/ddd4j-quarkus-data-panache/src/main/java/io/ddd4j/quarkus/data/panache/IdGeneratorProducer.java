package io.ddd4j.quarkus.data.panache;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * ID 生成策略 CDI 生产者：按构建期配置项 {@code ddd4j.quarkus.data.id-strategy}
 * 条件装配对应的 {@link IdGenerationStrategy} Bean。
 *
 * <p>配置项取值（{@code @IfBuildProperty} 构建期求值，与仓库其他扩展的条件装配模式一致）：
 * <ul>
 *   <li>{@code snowflake}（默认，未配置时启用）—— {@link SnowflakeIdStrategy}，产出 {@code IdGenerationStrategy<Long>}</li>
 *   <li>{@code auto-increment} —— {@link AutoIncrementIdStrategy}，产出 {@code IdGenerationStrategy<Long>}</li>
 *   <li>{@code uuid} —— {@link UuidIdStrategy}，产出 {@code IdGenerationStrategy<String>}</li>
 * </ul>
 *
 * <p>业务代码按策略的返回类型注入：
 * <pre>
 *   // snowflake / auto-increment
 *   {@code @Inject IdGenerationStrategy<Long> idStrategy;}
 *   Long id = idStrategy.generate();
 *
 *   // uuid
 *   {@code @Inject IdGenerationStrategy<String> idStrategy;}
 *   String id = idStrategy.generate();
 * </pre>
 *
 * <p><b>实现说明</b>：CDI 规范不允许 Bean 类型带通配符（{@code IdGenerationStrategy<?>}
 * 触发 DefinitionException），raw 类型在 ArC 下也不可赋值给参数化注入点——
 * 故按策略拆分为类型化的生产者方法，每个 Bean 类型均合法且注入类型精确。
 *
 * <p><b>注意</b>：业务项目可自行提供 {@code @Alternative} 或 {@code @Priority} 覆盖
 * 的 {@link IdGenerationStrategy} Bean，禁用对应配置项即可让自定义 Bean 接管。
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
     * 雪花算法策略（默认）：时间戳 + 节点 + 序列，生成递增 Long。
     *
     * @return 雪花策略
     */
    @Produces
    @ApplicationScoped
    @IfBuildProperty(name = ID_STRATEGY_CONFIG, stringValue = "snowflake", enableIfMissing = true)
    public IdGenerationStrategy<Long> snowflakeStrategy() {
        return new SnowflakeIdStrategy();
    }

    /**
     * 数据库自增策略：生成时返回 {@code null}，由数据库在 insert 时填充。
     *
     * @return 自增策略
     */
    @Produces
    @ApplicationScoped
    @IfBuildProperty(name = ID_STRATEGY_CONFIG, stringValue = "auto-increment")
    public IdGenerationStrategy<Long> autoIncrementStrategy() {
        return new AutoIncrementIdStrategy();
    }

    /**
     * UUID 策略：生成 32 位无横线十六进制字符串。
     *
     * @return UUID 策略
     */
    @Produces
    @ApplicationScoped
    @IfBuildProperty(name = ID_STRATEGY_CONFIG, stringValue = "uuid")
    public IdGenerationStrategy<String> uuidStrategy() {
        return new UuidIdStrategy();
    }
}
