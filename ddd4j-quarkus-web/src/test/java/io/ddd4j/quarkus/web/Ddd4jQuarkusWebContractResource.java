package io.ddd4j.quarkus.web;

import io.ddd4j.core.constant.ContextConstants;
import io.ddd4j.core.context.ThreadContext;
import io.ddd4j.web.core.context.WebContextScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import java.util.Objects;

/**
 * 仅供 Web 消费者集成测试使用的请求上下文资源和响应期清理探针。
 */
@Path("/ddd4j/contract")
@ApplicationScoped
public class Ddd4jQuarkusWebContractResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ContractResponse contract() {
        return new ContractResponse(
                ThreadContext.get(ContextConstants.TENANT_ID),
                ThreadContext.get(WebContextScope.REQUEST_ID),
                ThreadContext.get(ContextConstants.AUTHORIZATION),
                Thread.currentThread().getName());
    }

    /**
     * 以低于 ddd4j 用户优先级的响应优先级执行，验证会话已在同一服务线程中关闭。
     */
    @ServerResponseFilter(priority = Priorities.AUTHENTICATION)
    public void capturePostResponseContext(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().putSingle("X-Ddd4j-Test-Post-Response-Service-Thread", Thread.currentThread().getName());
        response.getHeaders().putSingle("X-Ddd4j-Test-Post-Response-Tenant-Cleared",
                Boolean.toString(Objects.isNull(ThreadContext.get(ContextConstants.TENANT_ID))));
        response.getHeaders().putSingle("X-Ddd4j-Test-Post-Response-Request-Id-Cleared",
                Boolean.toString(Objects.isNull(ThreadContext.get(WebContextScope.REQUEST_ID))));
        response.getHeaders().putSingle("X-Ddd4j-Test-Post-Response-Authorization-Cleared",
                Boolean.toString(Objects.isNull(ThreadContext.get(ContextConstants.AUTHORIZATION))));
    }

    /**
     * 暴露资源处理期间的线程与请求上下文，证明清理前各状态已实际绑定。
     */
    public record ContractResponse(String tenantId, String requestId, String authorization, String serviceThread) {
    }
}
