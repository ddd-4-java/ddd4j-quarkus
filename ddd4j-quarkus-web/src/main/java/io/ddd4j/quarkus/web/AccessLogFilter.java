package io.ddd4j.quarkus.web;

import io.ddd4j.web.core.WebRequestContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Set;

/**
 * Quarkus Web 访问日志过滤器：记录写操作的请求摘要。
 *
 * <p>对齐 ddd4j-boot 中由 {@code AccessLogInterceptor} 提供的功能；区别在于用
 * JAX-RS {@link ContainerRequestFilter}/{@link ContainerResponseFilter} 实现。
 *
 * <p>日志主题 {@code access}，可通过 {@code org.apache.log4j2.logger.access.level}
 * 单独调节。
 */
@Provider
public class AccessLogFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger("access");
    private static final Set<String> LOG_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    @Override
    public void filter(ContainerRequestContext request) {
        request.setProperty("access-log-start", System.currentTimeMillis());
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        String method = request.getMethod();
        if (method == null || !LOG_METHODS.contains(method)) {
            return;
        }
        long start = (Long) request.getProperty("access-log-start");
        long duration = System.currentTimeMillis() - start;
        String tenant = request.getHeaderString("X-Tenant-Id");
        String userAgent = request.getHeaderString("User-Agent");
        LOG.infof("[%s] %s %s -> %d (%d ms) tenant=%s ua=%s",
                method,
                request.getUriInfo().getRequestUri().getPath(),
                "",
                response.getStatus(),
                duration,
                tenant == null ? "-" : tenant,
                userAgent == null ? "-" : userAgent);
    }
}