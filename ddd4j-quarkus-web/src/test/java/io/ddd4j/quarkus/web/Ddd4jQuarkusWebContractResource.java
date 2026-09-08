package io.ddd4j.quarkus.web;

import io.ddd4j.core.constant.ContextConstants;
import io.ddd4j.core.context.ThreadContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Collections;
import java.util.Map;

@Path("/ddd4j/contract")
@Produces(MediaType.APPLICATION_JSON)
public class Ddd4jQuarkusWebContractResource {

    @GET
    public Map<String, String> context() {
        return Collections.singletonMap("tenantId", ThreadContext.get(ContextConstants.TENANT_ID));
    }
}
