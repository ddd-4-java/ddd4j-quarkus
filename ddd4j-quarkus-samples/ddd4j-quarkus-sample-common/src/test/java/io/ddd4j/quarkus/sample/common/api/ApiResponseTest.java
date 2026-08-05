package io.ddd4j.quarkus.sample.common.api;

import io.ddd4j.core.api.Page;
import io.ddd4j.core.api.R;
import io.ddd4j.core.api.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ApiResponse} / {@link PageResponse} 单元测试。
 *
 * <p>纯 JUnit 5：覆盖统一响应 ok/fail 静态工厂与核心响应 {@link R} 适配的边界。</p>
 */
class ApiResponseTest {

    @Test
    void okShouldReturnSuccessResponseWithoutData() {
        ApiResponse<String> response = ApiResponse.ok();

        assertThat(response.getCode()).isEqualTo(ResultCode.OK.getCode());
        assertThat(response.getMessage()).isEqualTo(ResultCode.OK.getDesc());
        assertThat(response.getData()).isNull();
    }

    @Test
    void okWithDataShouldCarryPayload() {
        ApiResponse<String> response = ApiResponse.ok("payload");

        assertThat(response.getCode()).isEqualTo(ResultCode.OK.getCode());
        assertThat(response.getData()).isEqualTo("payload");
    }

    @Test
    void failShouldReturnFailureResponseWithExplicitCode() {
        ApiResponse<String> response = ApiResponse.fail(ResultCode.BAD_REQUEST.getCode(), "订单不存在");

        assertThat(response.getCode()).isEqualTo(ResultCode.BAD_REQUEST.getCode());
        assertThat(response.getMessage()).isEqualTo("订单不存在");
        assertThat(response.getData()).isNull();
    }

    @Test
    void failWithMessageOnlyShouldUseDefaultFailCode() {
        ApiResponse<String> response = ApiResponse.fail("服务异常");

        assertThat(response.getCode()).isEqualTo(ResultCode.FAIL.getCode());
        assertThat(response.getMessage()).isEqualTo("服务异常");
        assertThat(response.getData()).isNull();
    }

    @Test
    void ofShouldAdaptCoreResponse() {
        R<String> r = R.ok("data");
        ApiResponse<String> response = ApiResponse.of(r);

        assertThat(response.getCode()).isEqualTo(r.getCode());
        assertThat(response.getMessage()).isEqualTo(r.getMsg());
        assertThat(response.getData()).isEqualTo("data");
    }

    @Test
    void ofShouldRejectNullCoreResponse() {
        assertThatThrownBy(() -> ApiResponse.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void pageResponseOfShouldAdaptCorePage() {
        Page<String> page = Page.succeed(List.of("a", "b"), 2, 1, 10);
        PageResponse<String> response = PageResponse.of(page);

        assertThat(response.getRecords()).containsExactly("a", "b");
        assertThat(response.getTotal()).isEqualTo(2);
        assertThat(response.getCurrent()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(10);
    }

    @Test
    void pageResponseEmptyShouldReturnDefaultEmptyPage() {
        PageResponse<String> response = PageResponse.empty();

        assertThat(response.getRecords()).isEmpty();
        assertThat(response.getTotal()).isZero();
        assertThat(response.getCurrent()).isEqualTo(1L);
        assertThat(response.getSize()).isEqualTo(10L);
    }
}
