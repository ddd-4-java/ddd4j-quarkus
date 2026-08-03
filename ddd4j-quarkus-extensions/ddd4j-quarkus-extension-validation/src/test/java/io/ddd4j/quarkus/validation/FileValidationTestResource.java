package io.ddd4j.quarkus.validation;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/**
 * 文件约束的测试 REST 资源。
 */
@Path("/validation/files")
public class FileValidationTestResource {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(@RestForm("file") @ValidFileUpload FileUpload fileUpload) {
        return Response.ok(fileUpload.fileName()).build();
    }
}
