package io.ddd4j.quarkus.cola.exception;

import com.alibaba.cola.exception.SysException;
import io.ddd4j.core.ApiCode;
import io.ddd4j.core.ApiRestResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * COLA 系统异常处理器（Quarkus 版，JAX-RS {@link ExceptionMapper}）。
 *
 * <p>将 COLA 的 {@link SysException}（系统异常）转换为统一的
 * {@link ApiRestResponse}（{@code ApiCode.SC_INTERNAL_SERVER_ERROR}，HTTP 500）。
 *
 * <p>与 boot 版差异：boot 版在 {@code ColaExceptionHandler} 中以 Spring MVC
 * {@code @ExceptionHandler} 多方法承载；JAX-RS 规范要求一个 ExceptionMapper 类只映射一种
 * 异常类型，故拆分本类，由 {@code ColaCdiProducer} 装配（{@link Provider} 标注便于 REST
 * 运行时发现）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Provider
public class ColaSysExceptionHandler implements ExceptionMapper<SysException> {

    private static final Logger log = LoggerFactory.getLogger(ColaSysExceptionHandler.class);

    /**
     * 处理 COLA 系统异常。
     *
     * @param ex COLA 系统异常
     * @return 统一响应（HTTP 500）
     */
    @Override
    public Response toResponse(SysException ex) {
        log.error("COLA 系统异常: errCode={}, message={}", ex.getErrCode(), ex.getMessage(), ex);
        ApiRestResponse<String> body = ApiCode.SC_INTERNAL_SERVER_ERROR.toResponse(
                ex.getMessage() != null ? "COLA: " + ex.getMessage() : "COLA: 系统异常");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(body)
                .build();
    }
}
