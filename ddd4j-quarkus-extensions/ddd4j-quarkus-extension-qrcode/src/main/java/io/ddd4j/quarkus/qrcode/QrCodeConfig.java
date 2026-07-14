package io.ddd4j.quarkus.qrcode;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/** Quarkus QR code service and HTTP limits. */
@ConfigMapping(prefix = "ddd4j.qrcode")
public interface QrCodeConfig {

    @WithDefault("true")
    boolean enabled();

    @WithDefault("8")
    int concurrency();

    @WithDefault("100")
    int maxBatchSize();

    @WithDefault("10485760")
    int maxUploadBytes();

    Web web();

    interface Web {

        @WithDefault("false")
        boolean enabled();
    }
}
