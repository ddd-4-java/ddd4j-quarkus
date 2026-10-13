package io.ddd4j.quarkus.qrcode;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Quarkus QR code service and HTTP limits.
 */
@ConfigMapping(prefix = "ddd4j.qrcode")
public interface QrCodeConfig {

    /**
     * 是否启用。
     * @return 是否启用
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * 执行 concurrency 操作。
     * @return 方法返回结果
     */
    @WithDefault("8")
    int concurrency();

    /**
     * 最大批次大小。
     * @return 最大批次大小
     */
    @WithDefault("100")
    int maxBatchSize();

    /**
     * 最大上传Bytes。
     * @return 最大上传Bytes
     */
    @WithDefault("10485760")
    int maxUploadBytes();

    /**
     * 执行 web 操作。
     * @return 方法返回结果
     */
    Web web();

    /**
     * Web 接口。
     */
    interface Web {

        /**
         * 是否启用。
         * @return 是否启用
         */
        @WithDefault("false")
        boolean enabled();
    }
}
