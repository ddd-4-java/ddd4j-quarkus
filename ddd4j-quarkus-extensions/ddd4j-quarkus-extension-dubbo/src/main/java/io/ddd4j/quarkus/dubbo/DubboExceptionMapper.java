package io.ddd4j.quarkus.dubbo;

import io.ddd4j.core.ApiCode;
import io.ddd4j.core.ApiRestResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dubbo RPC 异常处理器（JAX-RS 版，对应 boot 的 {@code DubboExceptionHandler}）。
 *
 * <p>boot 版通过 {@code @RestControllerAdvice + @ExceptionHandler(RpcException.class)}
 * 将 {@link RpcException} 及其子类转换为统一的 {@link ApiRestResponse}，避免 Dubbo 的框架异常
 * 直接暴露给客户端；Quarkus 等价物为 {@link ExceptionMapper}（{@code @Provider} 自动注册）。
 *
 * <p>异常映射规则（与 boot 完全一致）：
 * <ul>
 *   <li>{@code isTimeout()} / {@code LIMIT_EXCEEDED_EXCEPTION} / {@code TIMEOUT_TERMINATE}
 *       → 504 Gateway Timeout</li>
 *   <li>{@code NETWORK_EXCEPTION} / {@code FORBIDDEN_EXCEPTION} → 503 Service Unavailable</li>
 *   <li>{@code METHOD_NOT_FOUND} / {@code NO_INVOKER_AVAILABLE_AFTER_FILTER} → 404 Not Found</li>
 *   <li>{@code SERIALIZATION_EXCEPTION} / {@code VALIDATION_EXCEPTION} → 400 Bad Request</li>
 *   <li>{@code isBiz()} / 其他 → 500 Internal Server Error（业务异常）</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Provider
public class DubboExceptionMapper implements ExceptionMapper<RpcException> {

    private static final Logger log = LoggerFactory.getLogger(DubboExceptionMapper.class);

    @Override
    public Response toResponse(RpcException ex) {
        log.error("Dubbo RPC 异常: code={}, message={}", ex.getCode(), ex.getMessage(), ex);

        ApiCode apiCode = mapRpcExceptionToApiCode(ex);
        ApiRestResponse<String> body = apiCode.toResponse(getRpcMessage(ex));
        return Response.status(apiCode.getCode()).entity(body).build();
    }

    /**
     * 将 Dubbo RpcException 的 code 映射到 HTTP 状态码（逻辑与 boot 对齐）。
     *
     * @param ex RPC 异常
     * @return 对应的 ApiCode
     */
    private ApiCode mapRpcExceptionToApiCode(RpcException ex) {
        if (ex.isTimeout()) {
            return ApiCode.SC_GATEWAY_TIMEOUT;
        }
        if (ex.isBiz()) {
            return ApiCode.SC_INTERNAL_SERVER_ERROR;
        }
        switch (ex.getCode()) {
            case RpcException.FORBIDDEN_EXCEPTION:
                return ApiCode.SC_SERVICE_UNAVAILABLE;
            case RpcException.NETWORK_EXCEPTION:
                return ApiCode.SC_SERVICE_UNAVAILABLE;
            case RpcException.METHOD_NOT_FOUND:
                return ApiCode.SC_NOT_FOUND;
            case RpcException.NO_INVOKER_AVAILABLE_AFTER_FILTER:
                return ApiCode.SC_NOT_FOUND;
            case RpcException.LIMIT_EXCEEDED_EXCEPTION:
                return ApiCode.SC_GATEWAY_TIMEOUT;
            case RpcException.TIMEOUT_TERMINATE:
                return ApiCode.SC_GATEWAY_TIMEOUT;
            case RpcException.SERIALIZATION_EXCEPTION:
                return ApiCode.SC_BAD_REQUEST;
            case RpcException.VALIDATION_EXCEPTION:
                return ApiCode.SC_BAD_REQUEST;
            default:
                return ApiCode.SC_INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * 提取友好的异常消息（RpcException 的 message 可能包含堆栈信息，只取第一行）。
     *
     * @param ex RPC 异常
     * @return 消息字符串
     */
    private String getRpcMessage(RpcException ex) {
        String message = ex.getMessage();
        if (message != null) {
            int newline = message.indexOf('\n');
            if (newline > 0) {
                message = message.substring(0, newline);
            }
        }
        return message != null ? "Dubbo: " + message : "Dubbo: RPC 调用异常";
    }
}
