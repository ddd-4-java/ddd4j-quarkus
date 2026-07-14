package io.ddd4j.quarkus.qrcode;

import io.ddd4j.extension.qrcode.DefaultQrCodeService;
import io.ddd4j.extension.qrcode.QrCodeService;
import io.ddd4j.extension.qrcode.template.InMemoryQrCodeTemplateRegistry;
import io.ddd4j.extension.qrcode.template.QrCodeTemplateBinder;
import io.ddd4j.extension.qrcode.template.QrCodeTemplateRegistry;
import io.github.hiwepy.zxing.QrCodes;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/** CDI producer for the framework-neutral QR code service. */
@ApplicationScoped
@IfBuildProperty(name = "ddd4j.qrcode.enabled", stringValue = "true", enableIfMissing = true)
public class QrCodeProducer {

    @Produces
    @Singleton
    public QrCodeService qrCodeService(QrCodeConfig config) {
        return new DefaultQrCodeService(QrCodes.encoder(), QrCodes.decoder(),
                config.concurrency(), config.maxBatchSize());
    }

    @Produces
    @Singleton
    public QrCodeTemplateRegistry qrCodeTemplateRegistry() {
        return new InMemoryQrCodeTemplateRegistry();
    }

    @Produces
    @Singleton
    public QrCodeTemplateBinder qrCodeTemplateBinder() {
        return new QrCodeTemplateBinder();
    }

    public void close(@Disposes QrCodeService service) throws Exception {
        if (service instanceof AutoCloseable) {
            ((AutoCloseable) service).close();
        }
    }
}
