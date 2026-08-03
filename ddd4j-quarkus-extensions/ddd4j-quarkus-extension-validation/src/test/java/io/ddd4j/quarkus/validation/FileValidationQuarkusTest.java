package io.ddd4j.quarkus.validation;

import io.ddd4j.extension.validation.FileValidationService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Quarkus 文件上传约束集成测试。
 */
@QuarkusTest
class FileValidationQuarkusTest {

    @Inject
    FileValidationService validationService;

    @Test
    void shouldProvideCoreValidationServiceThroughCdi() {
        assertNotNull(validationService);
    }

    @Test
    void shouldAcceptPdfWithMatchingHeader() {
        given()
                .multiPart("file", "report.pdf", pdfBytes(), "application/pdf")
                .when().post("/validation/files")
                .then().statusCode(200);
    }

    @Test
    void shouldRejectExecutableRenamedAsPdf() {
        byte[] executable = "MZ\u0090\u0000malicious".getBytes(StandardCharsets.ISO_8859_1);
        given()
                .multiPart("file", "report.pdf", executable, "application/pdf")
                .when().post("/validation/files")
                .then().statusCode(400);
    }

    private byte[] pdfBytes() {
        return "%PDF-1.7\n1 0 obj\n<<>>\nendobj\n%%EOF".getBytes(StandardCharsets.US_ASCII);
    }
}
