package io.ddd4j.quarkus.web;

import io.ddd4j.core.constant.ContextConstants;
import io.ddd4j.core.context.ThreadContext;
import io.ddd4j.web.core.context.WebContextScope;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Path("/ddd4j/contract")
@Produces(MediaType.APPLICATION_JSON)
public class Ddd4jQuarkusWebContractResource {

    private static final String POST_RESPONSE_THREAD_HEADER = "X-Ddd4j-Test-Post-Response-Thread";
    private static final String POST_RESPONSE_TENANT_CLEARED_HEADER = "X-Ddd4j-Test-Post-Response-Tenant-Cleared";
    private static final String POST_RESPONSE_REQUEST_ID_CLEARED_HEADER = "X-Ddd4j-Test-Post-Response-Request-Id-Cleared";
    private static final String POST_RESPONSE_AUTHORIZATION_CLEARED_HEADER = "X-Ddd4j-Test-Post-Response-Authorization-Cleared";

    @GET
    public Map<String, String> context() {
        Map<String, String> context = new HashMap<>();
        context.put("tenantId", ThreadContext.get(ContextConstants.TENANT_ID));
        context.put("serviceThread", Thread.currentThread().getName());
        return context;
    }

    /**
     * 测试专用的响应生命周期探针：AUTHENTICATION 优先级会在上游 USER 响应过滤器之后执行。
     */
    @ServerResponseFilter(priority = Priorities.AUTHENTICATION)
    public void recordPostResponseThreadContext(ContainerResponseContext response) {
        response.getHeaders().putSingle(POST_RESPONSE_THREAD_HEADER, Thread.currentThread().getName());
        response.getHeaders().putSingle(POST_RESPONSE_TENANT_CLEARED_HEADER,
                Boolean.toString(Objects.isNull(ThreadContext.get(ContextConstants.TENANT_ID))));
        response.getHeaders().putSingle(POST_RESPONSE_REQUEST_ID_CLEARED_HEADER,
                Boolean.toString(Objects.isNull(ThreadContext.get(WebContextScope.REQUEST_ID))));
        response.getHeaders().putSingle(POST_RESPONSE_AUTHORIZATION_CLEARED_HEADER,
                Boolean.toString(Objects.isNull(ThreadContext.get(ContextConstants.AUTHORIZATION))));
    }
}
