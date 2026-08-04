package io.ddd4j.quarkus.excel;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * ddd4j-quarkus Excel CDI 装配（对应 boot 的 {@code Ddd4jExcelBootAutoConfiguration}）。
 *
 * <p>boot 版通过 {@code @AutoConfiguration + @ConditionalOnProperty(ddd4j.excel.enabled)}
 * 装配 {@code ExcelHttpKit} Bean；Quarkus 版等价物为：
 * <ul>
 *   <li>{@link ExcelConfig}：SmallRye {@code @ConfigMapping} 绑定 {@code ddd4j.excel.*} 配置
 *       （由 Quarkus 构建期自动注册为 CDI Bean，可注入）</li>
 *   <li>本 Producer：{@code @IfBuildProperty} 条件开关（{@code ddd4j.excel.enabled=false} 时
 *       整个装配禁用），生产 {@link ExcelHttpKit} 单例</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
@IfBuildProperty(name = "ddd4j.excel.enabled", stringValue = "true", enableIfMissing = true)
public class Ddd4jExcelCdiProducer {

    /**
     * Web Excel 工具单例（业务侧直接注入使用）。
     *
     * @param config Excel 配置（上传校验等参数）
     * @return {@link ExcelHttpKit}
     */
    @Produces
    @Singleton
    public ExcelHttpKit excelHttpKit(ExcelConfig config) {
        return new ExcelHttpKit(config);
    }
}
