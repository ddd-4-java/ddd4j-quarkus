package io.ddd4j.quarkus.qrcode;

import io.github.hiwepy.zxing.exception.QrCodeErrorCode;
import io.github.hiwepy.zxing.exception.QrCodeException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;

/** Maps QR engine errors to the same HTTP contract used by the other adapters. */
@Provider
public class QrCodeExceptionMapper implements ExceptionMapper<QrCodeException> {

    @Override
    public Response toResponse(QrCodeException exception) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", exception.getErrorCode().name());
        body.put("message", exception.getMessage());
        return Response.status(status(exception.getErrorCode())).entity(body).build();
    }

    private int status(QrCodeErrorCode errorCode) {
        if (errorCode == QrCodeErrorCode.QRCODE_IMAGE_TOO_LARGE) {
            return 413;
        }
        if (errorCode == QrCodeErrorCode.QRCODE_UNSUPPORTED_FORMAT) {
            return 415;
        }
        if (errorCode == QrCodeErrorCode.QRCODE_DECODE_NOT_FOUND
                || errorCode == QrCodeErrorCode.QRCODE_CAPACITY_EXCEEDED
                || errorCode == QrCodeErrorCode.QRCODE_SELF_CHECK_FAILED) {
            return 422;
        }
        return errorCode == QrCodeErrorCode.QRCODE_INVALID_ARGUMENT ? 400 : 500;
    }
}
