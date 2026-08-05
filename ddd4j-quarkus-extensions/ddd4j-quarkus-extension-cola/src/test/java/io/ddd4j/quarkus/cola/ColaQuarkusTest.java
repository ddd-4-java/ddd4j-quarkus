package io.ddd4j.quarkus.cola;

import com.alibaba.cola.dto.Response;
import io.ddd4j.quarkus.cola.exception.ColaExceptionHandler;
import io.ddd4j.quarkus.cola.exception.ColaSysExceptionHandler;
import io.ddd4j.quarkus.cola.handler.Ddd4jResponseHandler;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link ColaCdiProducer} Quarkus 集成测试：验证 COLA 响应处理器与
 * 异常映射器的 CDI 装配。
 */
@QuarkusTest
class ColaQuarkusTest {

    @Inject
    Ddd4jResponseHandler responseHandler;

    @Inject
    ColaExceptionHandler colaExceptionHandler;

    @Inject
    ColaSysExceptionHandler colaSysExceptionHandler;

    @Test
    void handlersShouldBeInjectable() {
        assertNotNull(responseHandler);
        assertNotNull(colaExceptionHandler);
        assertNotNull(colaSysExceptionHandler);
    }

    @Test
    void responseHandlerShouldBuildErrorResponse() {
        // 返回类型为 COLA Response 时构造失败响应（success=false + errCode/errMsg）
        Object result = responseHandler.handle(Response.class, "COL-001", "业务校验失败");
        assertNotNull(result);
        Response response = (Response) result;
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrCode());
    }
}
