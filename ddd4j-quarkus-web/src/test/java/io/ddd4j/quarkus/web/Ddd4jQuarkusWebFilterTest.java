package io.ddd4j.quarkus.web;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.Priorities;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;

/**
 * {@link Ddd4jQuarkusWebFilter} 集成测试：启动最小 Quarkus 运行时，
 * 请求 {@code /hello} 测试资源，断言请求头 {@code request-id} / {@code trace-id} 被回填。
 *
 * <ul>
 *   <li>未携带 {@code X-Request-Id} 时，服务端生成 UUID 回填到响应头</li>
 *   <li>携带 {@code X-Request-Id} 时，原样回显</li>
 *   <li>携带 {@code X-Trace-Id} 时，透传到响应头</li>
 * </ul>
 *
 * <p>说明：{@link Ddd4jQuarkusWebFilter} 本身不是 CDI Bean（由上层应用按需注册），
 * 测试通过 {@link Ddd4jQuarkusWebFilterBean}（{@code @ApplicationScoped} 子类）将其
 * 注册进容器，使 RESTEasy Reactive 发现 {@code @ServerRequestFilter}/{@code @ServerResponseFilter} 方法。
 */
@QuarkusTest
class Ddd4jQuarkusWebFilterTest {

    @Test
    void backfillsRequestIdWhenClientDoesNotSendOne() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .header("X-Request-Id", not(emptyOrNullString()));
    }

    @Test
    void echoesClientSuppliedRequestId() {
        given()
                .header("X-Request-Id", "client-req-42")
                .when().get("/hello")
                .then()
                .statusCode(200)
                .header("X-Request-Id", equalTo("client-req-42"));
    }

    @Test
    void passesThroughTraceId() {
        given()
                .header("X-Trace-Id", "trace-abc-123")
                .when().get("/hello")
                .then()
                .statusCode(200)
                .header("X-Trace-Id", equalTo("trace-abc-123"));
    }
}

/**
 * 测试用 JAX-RS 资源：暴露 {@code GET /hello} 供过滤器断言。
 */
@Path("/hello")
@ApplicationScoped
class HelloTestResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }
}

/**
 * 测试用过滤器 Bean：将 {@link Ddd4jQuarkusWebFilter} 以 CDI Bean 形式注册，
 * 显式重声明过滤器方法并委托给父类（保证 RESTEasy Reactive 发现到 filter 方法）。
 */
@ApplicationScoped
class Ddd4jQuarkusWebFilterBean extends Ddd4jQuarkusWebFilter {

    @ServerRequestFilter(priority = Priorities.AUTHENTICATION)
    void doRequest(ContainerRequestContext request) {
        super.request(request);
    }

    @ServerResponseFilter(priority = Priorities.USER)
    void doResponse(ContainerRequestContext request, ContainerResponseContext response) {
        super.response(request, response);
    }
}
