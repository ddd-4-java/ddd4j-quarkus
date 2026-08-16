package io.ddd4j.quarkus.web;

import io.ddd4j.core.api.R;
import io.ddd4j.web.core.error.DefaultWebExceptionTranslator;
import io.ddd4j.web.core.error.WebError;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Quarkus Web 默认异常映射器：将 {@link RuntimeException} 转译为统一的 {@link R} JSON 响应。
 *
 * <p>对齐 ddd4j-boot 中的 {@code BaseExceptionHandler}/{@code MybatisExceptionHandler}
 * 模式，但用 JAX-RS {@link ExceptionMapper} 而非 Spring {@code @ControllerAdvice} 实现。
 *
 * <p>翻译逻辑复用 ddd4j-web-core 中的 {@link DefaultWebExceptionTranslator}，保证多框架
 * （Quarkus / Spring / Javalin / Vert.x）下的错误响应体一致。
 */
@Provider
public class DefaultExceptionHandler implements ExceptionMapper<RuntimeException> {

    private static final Logger LOG = Logger.getLogger(DefaultExceptionHandler.class);

    private final DefaultWebExceptionTranslator translator = new DefaultWebExceptionTranslator();

    @Override
    public Response toResponse(RuntimeException exception) {
        WebError error = translator.translate(exception);
        // 仅记录 5xx 错误；4xx 由业务预期，正常返回即可
        if (error.status() >= 500) {
            LOG.errorf(exception, "Unhandled exception: %s", error.message());
        }
        R<Object> body = R.fail(Optional.ofNullable(error.code()).orElse(error.status()), error.message());
        return Response.status(error.status()).entity(body).build();
    }
}