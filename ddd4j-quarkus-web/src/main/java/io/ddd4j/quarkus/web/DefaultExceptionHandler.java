package io.ddd4j.quarkus.web;

import io.ddd4j.core.contract.R;
import io.quarkus.arc.log.LoggerName;
import org.jboss.logging.Logger;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Quarkus 全局异常映射：将未捕获异常转为统一 JSON 错误响应。
 * <p>
 * 对标 ddd4j-web 的 {@code GlobalRestExceptionAdvice}（Spring @RestControllerAdvice），
 * Quarkus 轨道采用 JAX-RS {@link ExceptionMapper} 方案。
 * </p>
 *
 * <h2>映射规则</h2>
 * <ul>
 *   <li>{@link WebApplicationException}：保留原始 HTTP 状态，404/401 转友好文案</li>
 *   <li>{@link IllegalArgumentException}：400 参数错误</li>
 *   <li>{@link io.ddd4j.core.exception.ServiceException}：业务异常，保留消息</li>
 *   <li>其余异常：500 服务器错误，记录 warn 日志</li>
 * </ul>
 */
@Provider
public class DefaultExceptionHandler implements ExceptionMapper<Exception> {

    @LoggerName("error")
    Logger logger;

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            String message = exception.getMessage();
            if (status == 404) message = "地址不存在";
            if (status == 401) message = "没有权限";
            return Response.ok(R.fail(status, message)).build();
        }
        if (exception instanceof IllegalArgumentException) {
            logger.warnf(exception, "Failed to process request");
            return Response.ok(R.fail(400, "参数不正确")).build();
        }
        // 业务异常（ddd4j-core 已有）
        if (exception instanceof io.ddd4j.core.contract.exception.ServiceException se) {
            return Response.ok(R.fail(se.getCode(), se.getMessage())).build();
        }
        logger.warnf(exception, "Failed to process request");
        return Response.ok(R.fail(500, exception.getMessage())).build();
    }
}
