package io.ddd4j.quarkus.sample.adapter.web;

import io.ddd4j.core.api.R;
import io.ddd4j.core.api.ResultCode;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * 业务参数/状态异常统一映射器（适配层）。
 *
 * <p>应用层/领域层以 {@link IllegalArgumentException} 表达「请求不合法」
 * （如订单不存在、状态流转不允许），本映射器将其统一转换为
 * HTTP 400 + {@code R.fail(400, message)}，与资源端点的 {@code R} 响应契约保持一致。</p>
 *
 * <p>说明：{@code ddd4j-web-quarkus} 的 {@code DefaultExceptionHandler}
 * 已能将 {@code IllegalArgumentException} 翻译为 400，本映射器作为更精确的
 * 适配层专属处理，显式返回统一的 {@code R} 结构，便于调用方消费。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Provider
public class OrderExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    private static final Logger log = Logger.getLogger(OrderExceptionMapper.class);

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        String message = exception.getMessage() == null
                ? ResultCode.BAD_REQUEST.getDesc() : exception.getMessage();
        log.debugf("Bad request mapped to 400: %s", message);
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(R.fail(ResultCode.BAD_REQUEST.getCode(), message))
                .build();
    }
}
