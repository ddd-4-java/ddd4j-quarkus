package io.ddd4j.quarkus.web;

import io.ddd4j.core.contract.R;
import io.quarkus.arc.log.LoggerName;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Quarkus 全局异常映射：将未捕获异常转为统一 JSON 错误响应。
 *
 * <p>对标 ddd4j-web 的 {@code GlobalRestExceptionAdvice}（Spring @RestControllerAdvice），
 * Quarkus 轨道采用 JAX-RS {@link ExceptionMapper} 方案。
 *
 * <h2>HTTP 状态码语义（可配置）</h2>
 * 通过配置项 {@code ddd4j.quarkus.web.exception-handler} 切换 HTTP 状态码策略：
 * <ul>
 *   <li>{@code unified200}（默认，前端友好模式）：HTTP 层统一返回 200，
 *       错误码仅在 JSON body 的 {@code R.code} 中。适合前端只认 200 的场景。</li>
 *   <li>{@code standard}（标准 REST 语义）：HTTP 状态码真实反映错误类型
 *       （404/401/400/500），便于网关、监控、标准客户端处理。</li>
 * </ul>
 *
 * <p>无论哪种模式，响应 body 始终使用 {@link R} 统一格式，保证前端解析逻辑一致。
 *
 * <h2>映射规则（错误码到状态码）</h2>
 * <ul>
 *   <li>{@link WebApplicationException}：保留原始 HTTP 状态，404/401 转友好文案</li>
 *   <li>{@link IllegalArgumentException}：400 参数错误</li>
 *   <li>{@link io.ddd4j.core.contract.exception.ServiceException}：业务异常，默认 400</li>
 *   <li>其余异常：500 服务器错误，记录 warn 日志</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Provider
public class DefaultExceptionHandler implements ExceptionMapper<Exception> {

    /**
     * 异常处理模式：{@code unified200}（默认）或 {@code standard}。
     */
    @ConfigProperty(name = "ddd4j.quarkus.web.exception-handler", defaultValue = "unified200")
    String exceptionHandlerMode;

    @LoggerName("error")
    Logger logger;

    @Override
    public Response toResponse(Exception exception) {
        boolean standard = "standard".equalsIgnoreCase(exceptionHandlerMode);

        if (exception instanceof WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            String message = exception.getMessage();
            if (status == 404) {
                message = "地址不存在";
            } else if (status == 401) {
                message = "没有权限";
            }
            return build(status, message, standard);
        }
        if (exception instanceof IllegalArgumentException) {
            logger.warnf(exception, "Failed to process request");
            return build(400, "参数不正确", standard);
        }
        // 业务异常（ddd4j-core 已有）
        if (exception instanceof io.ddd4j.core.contract.exception.ServiceException se) {
            return build(se.getCode(), se.getMessage(), standard ? 400 : 0);
        }
        logger.warnf(exception, "Failed to process request");
        return build(500, exception.getMessage(), standard);
    }

    /**
     * unified200 模式：统一 200，错误码进 body。
     */
    private Response build(int code, String message, boolean standard) {
        return build(code, message, standard ? code : 0);
    }

    /**
     * 构建响应。httpStatus 为 0 时表示 unified200 模式（用 200）。
     */
    private Response build(int code, String message, int httpStatus) {
        if (httpStatus > 0) {
            return Response.status(httpStatus).entity(R.fail(code, message)).build();
        }
        return Response.ok(R.fail(code, message)).build();
    }
}
