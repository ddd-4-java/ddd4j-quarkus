package io.ddd4j.quarkus.cola.handler;

import com.alibaba.cola.catchlog.ResponseHandlerI;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.exception.BaseException;
import io.ddd4j.core.ApiCode;
import io.ddd4j.core.ApiRestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * COLA catchlog 的 ResponseHandler 扩展（Quarkus 版）。
 *
 * <p>实现 COLA 的 {@link ResponseHandlerI} 接口，在 catchlog 捕获异常后，
 * 将 COLA 的 {@code Response} 格式转换为 ddd4j 的 {@link ApiRestResponse} 格式。
 *
 * <p>这样 COLA 的 {@code @CatchAndLog} AOP 产出的异常响应与 ddd4j 的其他接口风格一致，
 * 前端只需处理一种响应格式（{@code ApiRestResponse}）。
 *
 * <p>与 boot 版差异：boot 版标注 {@code @Component @Order(0)}（Spring），Quarkus 版为纯
 * Java 类，由 {@code ColaCdiProducer} 装配为 CDI Bean，无 Spring 注解。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class Ddd4jResponseHandler implements ResponseHandlerI {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jResponseHandler.class);

    @Override
    public Object handle(Class returnType, String errCode, String errMsg) {
        log.debug("COLA catchlog 捕获异常: errCode={}, errMsg={}", errCode, errMsg);

        // 如果返回类型是 ddd4j 的 ApiRestResponse，直接构造
        if (ApiRestResponse.class.isAssignableFrom(returnType)) {
            return ApiRestResponse.fail(errMsg);
        }

        // 如果返回类型是 COLA 的 Response，构造 COLA 格式（保持兼容）
        if (Response.class.isAssignableFrom(returnType)) {
            try {
                Response response = (Response) returnType.getDeclaredConstructor().newInstance();
                response.setSuccess(false);
                response.setErrCode(errCode);
                response.setErrMessage(errMsg);
                return response;
            } catch (Exception e) {
                log.warn("无法构造 COLA Response: {}", returnType.getName(), e);
            }
        }

        // 兜底：返回 null（catchlog 会记录）
        return null;
    }

    /**
     * 处理 COLA BaseException 的重载方法。
     *
     * <p>从异常中提取 errCode 和 errMsg，委托给主方法。
     *
     * @param returnType 返回类型
     * @param exception  COLA 异常
     * @return 构造的响应对象
     */
    public Object handle(Class returnType, BaseException exception) {
        String errCode = exception.getErrCode() != null ? exception.getErrCode() : ApiCode.SC_FAIL.name();
        String errMsg = exception.getMessage() != null ? exception.getMessage() : "COLA 业务异常";
        return handle(returnType, errCode, errMsg);
    }
}
