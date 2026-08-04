package io.ddd4j.quarkus.cola.exception;

import com.alibaba.cola.exception.BizException;
import io.ddd4j.core.ApiRestResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * COLA 业务异常处理器（Quarkus 版，JAX-RS {@link ExceptionMapper}）。
 *
 * <p>将 COLA 的 {@link BizException}（业务异常）转换为统一的
 * {@link ApiRestResponse}（{@code ApiCode.SC_FAIL}，HTTP 200 但 success=false）。
 *
 * <p>与 boot 版差异：boot 版基于 Spring MVC（{@code @RestControllerAdvice + @ExceptionHandler}，
 * 继承 {@code BaseExceptionHandler}，一个类承载多个异常处理方法）；JAX-RS 规范要求一个
 * ExceptionMapper 类只映射一种异常类型，故系统异常拆分到
 * {@link ColaSysExceptionHandler}，两个类均由 {@code ColaCdiProducer} 装配
 * （{@link Provider} 标注便于 REST 运行时发现）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Provider
public class ColaExceptionHandler implements ExceptionMapper<BizException> {

    private static final Logger log = LoggerFactory.getLogger(ColaExceptionHandler.class);

    /**
     * 处理 COLA 业务异常。
     *
     * @param ex COLA 业务异常
     * @return 统一响应（success=false，HTTP 200）
     */
    @Override
    public Response toResponse(BizException ex) {
        log.warn("COLA 业务异常: errCode={}, message={}", ex.getErrCode(), ex.getMessage());
        String message = ex.getMessage() != null ? ex.getMessage() : "COLA 业务异常";
        return Response.ok(ApiRestResponse.fail(message))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
}
