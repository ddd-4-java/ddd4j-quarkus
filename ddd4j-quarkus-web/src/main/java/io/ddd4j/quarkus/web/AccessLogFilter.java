package io.ddd4j.quarkus.web;

import io.quarkus.arc.log.LoggerName;
import io.vertx.core.http.HttpServerRequest;
import org.jboss.logging.Logger;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * 全局请求日志过滤器：记录客户端地址、HTTP 方法、路径与租户 ID，写入名为 {@code access} 的日志。
 * <p>
 * 对标 ddd4j-web 的 {@code LogWebInterceptor}（Spring HandlerInterceptor），
 * Quarkus 轨道采用 JAX-RS {@link ContainerRequestFilter} 方案。
 * </p>
 */
@Provider
public class AccessLogFilter implements ContainerRequestFilter {

    @LoggerName("access")
    Logger accessLog;

    @Context
    HttpServerRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();
        String tenantId = WebUtils.getTenantId(request);
        String address = request.remoteAddress().hostAddress();

        if ("POST".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method) || "GET".equalsIgnoreCase(method)) {
            accessLog.infof("%s:%s %s tenantId=%s", address, method, path, tenantId);
        }
    }
}
