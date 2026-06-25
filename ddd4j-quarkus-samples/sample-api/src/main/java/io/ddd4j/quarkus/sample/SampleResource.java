package io.ddd4j.quarkus.sample;

import io.ddd4j.core.contract.R;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * 示例 REST 资源。
 */
@Path("/hello")
public class SampleResource {

    /**
     * @return 统一响应
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public R<String> hello() {
        return R.ok("ddd4j-quarkus");
    }
}
