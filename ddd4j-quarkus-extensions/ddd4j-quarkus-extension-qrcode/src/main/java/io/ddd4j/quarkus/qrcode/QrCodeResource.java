package io.ddd4j.quarkus.qrcode;

import io.ddd4j.extension.qrcode.QrCodeService;
import io.ddd4j.extension.qrcode.command.DecodeQrCodeCommand;
import io.ddd4j.extension.qrcode.command.GenerateQrCodeCommand;
import io.ddd4j.extension.qrcode.model.QrCodeDecodeRequest;
import io.ddd4j.extension.qrcode.model.QrCodeOutput;
import io.ddd4j.extension.qrcode.model.QrCodeRequest;
import io.ddd4j.extension.qrcode.result.QrCodeArtifact;
import io.ddd4j.extension.qrcode.result.QrCodeScanResult;
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

/**
 * Opt-in Quarkus REST delivery（对齐主仓 ddd4j-extension-qrcode 当前 API）。
 *
 * <p>主仓 2.0.x 将模型收敛到 {@code io.ddd4j.extension.qrcode.model.*}（不再依赖
 * zxing-extension 的 {@code com.google.zxing.model.*}）：{@link QrCodeRequest}
 * 仅保留 content/width/height，格式/容错由实现默认处理。远程 URL 解码有意不支持。</p>
 */
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
        QrCodeRequest request = QrCodeRequest.builder()
                .content(input.getContent())
                .width(input.getWidth())
                .height(input.getHeight())
                .build();
        QrCodeArtifact artifact = service.generate(GenerateQrCodeCommand.builder()
                .correlationId(input.getCorrelationId())
                .request(request)
                .build());
        QrCodeOutput output = artifact.getOutput();
        return Response.ok(output.getBytes(), MediaType.APPLICATION_OCTET_STREAM).build();
    }

    @POST
    @Path("/decode")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public QrCodeScanResult decode(@RestForm("file") FileUpload file) throws IOException {
        if (file.size() > config.maxUploadBytes()) {
            throw new IllegalArgumentException(
                    "QR code image exceeds configured upload limit");
        }
        byte[] bytes = Files.readAllBytes(file.uploadedFile());
        return service.decode(DecodeQrCodeCommand.builder()
                .request(QrCodeDecodeRequest.from(bytes))
                .build());
    }

    @Data
    public static class RenderRequest {

        private String correlationId;
        private String content;
        private int width = 256;
        private int height = 256;
    }
}
