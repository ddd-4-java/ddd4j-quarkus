package io.ddd4j.quarkus.qrcode;

import io.ddd4j.extension.qrcode.QrCodeService;
import io.ddd4j.extension.qrcode.command.GenerateQrCodeCommand;
import io.github.hiwepy.zxing.model.QrCodeRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class QrCodeQuarkusTest {

    @Inject
    QrCodeService service;

    @Test
    void shouldProvideCdiService() {
        assertNotNull(service.generate(GenerateQrCodeCommand.builder()
                .request(QrCodeRequest.builder("quarkus-cdi").build())
                .build()));
    }

    @Test
    void shouldRenderAndDecodeThroughRest() {
        byte[] png = given()
                .contentType("application/json")
                .body("{\"content\":\"quarkus-rest\"}")
                .when().post("/qrcodes/render")
                .then().statusCode(200).contentType("image/png")
                .extract().asByteArray();

        given()
                .multiPart("file", "code.png", png, "image/png")
                .when().post("/qrcodes/decode")
                .then().statusCode(200).body(containsString("quarkus-rest"));
    }
}
