package io.ddd4j.quarkus.sample.common.api;

import io.ddd4j.core.api.R;
import io.ddd4j.core.api.ResultCode;

import java.io.Serializable;
import java.util.Objects;

/**
 * 统一接口响应包装。
 *
 * <p>基于核心统一响应 {@link R} 的轻量包装：{@code code + message + data} 三要素，
 * 提供 {@link #ok} / {@link #fail} 静态工厂与 {@link #of(R)} 适配方法。
 * 业务接口可直接使用 {@link R}，本类用于需要自定义出参结构的场景。</p>
 *
 * @param <T> 数据类型
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class ApiResponse<T> {

    /**
     * 编码：0/200 请求成功；500 服务异常；403 未登录；401 无权限
     */
    private Serializable code;

    /**
     * 返回信息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(Serializable code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（无数据）。
     *
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(ResultCode.OK.getCode(), ResultCode.OK.getDesc(), null);
    }

    /**
     * 成功响应（带数据）。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ResultCode.OK.getCode(), ResultCode.OK.getDesc(), data);
    }

    /**
     * 失败响应。
     *
     * @param code    错误编码
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> fail(Serializable code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 失败响应（默认编码）。
     *
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> fail(String message) {
        return fail(ResultCode.FAIL.getCode(), message);
    }

    /**
     * 从核心统一响应 {@link R} 适配为轻量包装。
     *
     * @param r 核心统一响应
     * @param <T> 数据类型
     * @return 轻量包装响应
     */
    public static <T> ApiResponse<T> of(R<T> r) {
        Objects.requireNonNull(r, "r must not be null");
        return new ApiResponse<>(r.getCode(), r.getMsg(), r.getData());
    }

    public Serializable getCode() {
        return code;
    }

    public void setCode(Serializable code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
