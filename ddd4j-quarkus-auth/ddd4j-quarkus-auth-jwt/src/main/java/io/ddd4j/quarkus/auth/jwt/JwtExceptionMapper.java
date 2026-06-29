package io.ddd4j.quarkus.auth.jwt;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * SmallRye JWT 验证异常映射器：将 JWT 解析/验证异常转为统一 401 响应。
 *
 * <p>对标 satoken/security/shiro 三鉴权的 ExceptionMapper，覆盖 JWT 模式的 401 场景。
 * 响应体格式：{@code {"code":401,"msg":"..."}}，与 ddd4j-quarkus-web 的 {@code R.fail} 风格一致。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Provider
public class JwtExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        // SmallRye JWT 抛出的异常类型较多（ParseException/JsonException/JWTDecodeException 等），
        // 这里统一映射为 401，msg 为根因消息。
        JsonObject body = Json.createObjectBuilder()
                .add("code", 401)
                .add("msg", "无效的访问凭证：" + rootMessage(exception))
                .build();
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(body)
                .build();
    }

    /**
     * 取根因消息（避免只看到包装异常的 "..."）。
     */
    private String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return msg == null ? t.getClass().getSimpleName() : msg;
    }
}
