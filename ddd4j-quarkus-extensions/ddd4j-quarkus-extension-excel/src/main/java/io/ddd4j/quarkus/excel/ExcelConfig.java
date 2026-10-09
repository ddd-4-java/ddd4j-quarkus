package io.ddd4j.quarkus.excel;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * ddd4j-quarkus-excel 配置（绑定 {@code ddd4j.excel.*}）。
 *
 * <p>对应 ddd4j-boot-extension-excel 的 {@code ExcelProperties}（{@code @ConfigurationProperties}
 * 前缀 {@code ddd4j.excel}），以 Quarkus {@link ConfigMapping} 方式承载等价配置模型。
 *
 * <p>配置示例（application.properties）：
 * <pre>{@code
 * ddd4j.excel.enabled=true
 * ddd4j.excel.batch-size=1000
 * ddd4j.excel.max-upload-mb=50
 * ddd4j.excel.charset=UTF-8
 * ddd4j.excel.style.default-border=true
 * ddd4j.excel.style.auto-size-column=true
 * ddd4j.excel.style.header-row-height=600
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ConfigMapping(prefix = "ddd4j.excel")
public interface ExcelConfig {

    /**
     * 是否启用 ddd4j-quarkus-excel 自动装配（总开关，默认 true）。
     * @return 是否启用
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * 监听器批量入库阈值，默认 1000 行。
     * @return 批次大小
     */
    @WithDefault("1000")
    int batchSize();

    /**
     * Web 上传单文件大小上限（MB），默认 50。
     * @return 最大上传MB
     */
    @WithDefault("50")
    int maxUploadMB();

    /**
     * 字符编码，默认 UTF-8（影响 CSV 与文件名编码）。
     * @return 方法返回结果
     */
    @WithDefault("UTF-8")
    String charset();

    /**
     * 样式相关配置。
     * @return 方法返回结果
     */
    Style style();

    /**
     * 导出样式配置（对齐 boot {@code ExcelProperties.Style}）。
     */
    interface Style {

        /**
         * 是否为单元格启用默认细边框。
         * @return 是否默认Border
         */
        @WithDefault("true")
        boolean defaultBorder();

        /**
         * 是否自动适配列宽（基于内容长度）。
         * @return 是否自动大小列
         */
        @WithDefault("true")
        boolean autoSizeColumn();

        /**
         * 表头行高（单位：1/20 磅，即 short）。例如 600 表示 30 磅；默认 600。
         * @return 报文头行Height
         */
        @WithDefault("600")
        short headerRowHeight();
    }
}
