package io.ddd4j.quarkus.web;

import io.ddd4j.web.core.BearerSubjectAuthenticator;
import io.ddd4j.web.core.CacheIdempotencyGuard;
import io.ddd4j.web.core.ClientIpResolver;
import io.ddd4j.web.core.RequestIdGenerator;
import io.ddd4j.web.core.SynchronousWebRequestSession;
import io.ddd4j.web.core.WebHeaders;
import io.ddd4j.web.core.WebIdempotencyLifecycle;
import io.ddd4j.web.core.WebOtelSupport;
import io.ddd4j.web.core.WebRequestContext;
import io.ddd4j.web.core.WebRequestContextFactory;
import io.ddd4j.web.core.WebRequestData;
import io.ddd4j.web.core.WebRequestLifecycle;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Quarkus REST 同步请求上下文、Bearer Subject 与幂等过滤器。
 *
 * <p>对齐 ddd4j-boot 的 {@code Ddd4jWebMvcAutoConfiguration}/{@code Ddd4jWebFluxAutoConfiguration}
 * 中的过滤器行为。区别在于底层使用 RESTEasy Reactive 的 {@link ServerRequestFilter}/
 * {@link ServerResponseFilter} 而非 Spring MVC/WebFlux 的拦截器。
 *
 * <p>同时集成 OTel 分布式追踪：通过 {@link WebOtelSupport} 反射调用
 * WebOtelIntegration（OTel 集成可选，无依赖时不生效）。
 */
public class Ddd4jQuarkusWebFilter {

    static final String SESSION_PROPERTY = Ddd4jQuarkusWebFilter.class.getName() + ".session";
    static final String CONTEXT_PROPERTY = Ddd4jQuarkusWebFilter.class.getName() + ".context";
    static final String OTEL_SPAN_PROPERTY = Ddd4jQuarkusWebFilter.class.getName() + ".otelSpan";
    static final String OTEL_SCOPE_PROPERTY = Ddd4jQuarkusWebFilter.class.getName() + ".otelScope";

    private final WebRequestContextFactory contextFactory;
    private final WebRequestLifecycle requestLifecycle;
    private final WebIdempotencyLifecycle idempotencyLifecycle;

    @Inject
    RoutingContext routingContext;

    public Ddd4jQuarkusWebFilter() {
        this(Ddd4jQuarkusWebConfiguration.load());
    }

    public Ddd4jQuarkusWebFilter(Ddd4jQuarkusWebConfiguration configuration) {
        Ddd4jQuarkusWebConfiguration config = Objects.requireNonNull(configuration,
                "configuration must not be null");
        ClientIpResolver clientIpResolver = config.isTrustForwardedHeaders()
                ? ClientIpResolver.trustedProxy() : ClientIpResolver.remoteAddressOnly();
        this.contextFactory = new WebRequestContextFactory(RequestIdGenerator.uuid(), clientIpResolver);
        this.requestLifecycle = new WebRequestLifecycle(new BearerSubjectAuthenticator(), config.accessPolicy());
        this.idempotencyLifecycle = config.isIdempotencyEnabled()
                ? new WebIdempotencyLifecycle(new CacheIdempotencyGuard(config.getIdempotencyCacheName()),
                        config.getIdempotencyTtl()) : null;
    }

    Ddd4jQuarkusWebFilter(WebRequestContextFactory contextFactory,
                          WebRequestLifecycle requestLifecycle,
                          WebIdempotencyLifecycle idempotencyLifecycle) {
        this.contextFactory = Objects.requireNonNull(contextFactory, "contextFactory must not be null");
        this.requestLifecycle = Objects.requireNonNull(requestLifecycle, "requestLifecycle must not be null");
        this.idempotencyLifecycle = idempotencyLifecycle;
    }

    @ServerRequestFilter(priority = Priorities.AUTHENTICATION)
    public void request(ContainerRequestContext request) {
        // OTel: 启动 SERVER span
        Object span = WebOtelSupport.startServerSpan(
                request.getMethod(),
                request.getUriInfo().getRequestUri().getPath(),
                extractRequestHeaders(request));
        AutoCloseable scope = WebOtelSupport.activate(span);
        request.setProperty(OTEL_SPAN_PROPERTY, span);
        request.setProperty(OTEL_SCOPE_PROPERTY, scope);

        try {
            WebRequestContext context = createContext(request);
            SynchronousWebRequestSession session = SynchronousWebRequestSession.open(context, requestLifecycle,
                    idempotencyLifecycle, request.getHeaderString(WebHeaders.IDEMPOTENCY_KEY));
            request.setProperty(CONTEXT_PROPERTY, context);
            request.setProperty(SESSION_PROPERTY, session);
        } catch (RuntimeException exception) {
            WebOtelSupport.recordError(span, exception);
            throw exception;
        }
    }

    @ServerResponseFilter(priority = Priorities.USER)
    public void response(ContainerRequestContext request, ContainerResponseContext response) {
        Object contextValue = request.getProperty(CONTEXT_PROPERTY);
        if (contextValue instanceof WebRequestContext context) {
            response.getHeaders().putSingle(WebHeaders.REQUEST_ID, context.requestId());
            response.getHeaders().putSingle(WebHeaders.TRACE_ID, context.traceId());
        }
        Object sessionValue = request.getProperty(SESSION_PROPERTY);
        boolean successful = response.getStatus() < 400;
        if (sessionValue instanceof SynchronousWebRequestSession session) {
            session.complete(successful);
        }
        // OTel: 结束 span
        Object span = request.getProperty(OTEL_SPAN_PROPERTY);
        if (span != null) {
            WebOtelSupport.endServerSpan(span, response.getStatus());
            request.removeProperty(OTEL_SPAN_PROPERTY);
        }
        Object scope = request.getProperty(OTEL_SCOPE_PROPERTY);
        if (scope instanceof AutoCloseable) {
            try {
                ((AutoCloseable) scope).close();
            } catch (Throwable ignored) {
            }
            request.removeProperty(OTEL_SCOPE_PROPERTY);
        }
        request.removeProperty(CONTEXT_PROPERTY);
        request.removeProperty(SESSION_PROPERTY);
    }

    private static Map<String, String> extractRequestHeaders(ContainerRequestContext request) {
        Map<String, String> headers = new HashMap<>();
        for (var entry : request.getHeaders().entrySet()) {
            String value = request.getHeaderString(entry.getKey());
            if (value != null) {
                headers.put(entry.getKey(), value);
            }
        }
        return headers;
    }

    private WebRequestContext createContext(ContainerRequestContext request) {
        return contextFactory.create(new WebRequestData(
                request.getHeaderString(WebHeaders.REQUEST_ID),
                request.getHeaderString(WebHeaders.TRACE_ID),
                request.getHeaderString(WebHeaders.TENANT_ID),
                request.getHeaderString(WebHeaders.AUTHORIZATION),
                Optional.ofNullable(request.getLanguage()).orElse(Locale.getDefault()),
                request.getHeaderString(WebHeaders.FORWARDED_FOR),
                request.getHeaderString("X-Real-IP"),
                remoteAddress(),
                request.getMethod(),
                request.getUriInfo().getRequestUri().getPath()));
    }

    private String remoteAddress() {
        if (Objects.isNull(routingContext) || Objects.isNull(routingContext.request().remoteAddress())) {
            return null;
        }
        return routingContext.request().remoteAddress().hostAddress();
    }
}