package io.ddd4j.quarkus.qrcode;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.ddd4j.extension.qrcode.QrCodeService;
import io.ddd4j.extension.qrcode.command.DecodeQrCodeCommand;
import io.ddd4j.extension.qrcode.command.GenerateQrCodeCommand;
import io.ddd4j.extension.qrcode.result.QrCodeArtifact;
import io.ddd4j.extension.qrcode.result.QrCodeScanResult;
import io.github.hiwepy.zxing.exception.QrCodeErrorCode;
import io.github.hiwepy.zxing.exception.QrCodeException;
import io.github.hiwepy.zxing.model.QrCodeDecodeRequest;
import io.github.hiwepy.zxing.model.QrCodeImageFormat;
import io.github.hiwepy.zxing.model.QrCodeRequest;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Data;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;

/** Opt-in Quarkus REST delivery. Remote URL decoding is intentionally unsupported. */
@Path("/qrcodes")
@IfBuildProperty(name = "ddd4j.qrcode.web.enabled", stringValue = "true")
public class QrCodeResource {

    @Inject
    QrCodeService service;

    @Inject
    QrCodeConfig config;

    @POST
    @Path("/render")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response render(RenderRequest input) {
        QrCodeImageFormat format = QrCodeImageFormat.valueOf(input.getFormat().toUpperCase());
        QrCodeRequest request = QrCodeRequest.builder(input.getContent())
                .size(input.getWidth(), input.getHeight())
                .margin(input.getMargin())
                .errorCorrectionLevel(ErrorCorrectionLevel.valueOf(input.getErrorCorrectionLevel().toUpperCase()))
                .format(format)
                .selfCheck(input.isSelfCheck())
                .build();
        QrCodeArtifact artifact = service.generate(GenerateQrCodeCommand.builder()
                .correlationId(input.getCorrelationId())
                .request(request)
                .build());
        return Response.ok(artifact.getOutput().getBytes(), artifact.getOutput().getFormat().getMimeType()).build();
    }

    @POST
    @Path("/decode")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public QrCodeScanResult decode(@RestForm("file") FileUpload file) throws IOException {
        if (file.size() > config.maxUploadBytes()) {
            throw new QrCodeException(QrCodeErrorCode.QRCODE_IMAGE_TOO_LARGE,
                    "QR code image exceeds configured upload limit");
        }
        byte[] bytes = Files.readAllBytes(file.uploadedFile());
        return service.decode(DecodeQrCodeCommand.builder()
                .request(QrCodeDecodeRequest.from(bytes)
                        .multiple(true)
                        .maxInputBytes(config.maxUploadBytes())
                        .build())
                .build());
    }

    @Data
    public static class RenderRequest {

        private String correlationId;
        private String content;
        private int width = 256;
        private int height = 256;
        private int margin = 2;
        private String format = "PNG";
        private String errorCorrectionLevel = "M";
        private boolean selfCheck;
    }
}
