package io.ddd4j.quarkus.cola;

import io.ddd4j.quarkus.cola.exception.ColaExceptionHandler;
import io.ddd4j.quarkus.cola.exception.ColaSysExceptionHandler;
import io.ddd4j.quarkus.cola.handler.Ddd4jResponseHandler;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * COLA 架构组件 Quarkus CDI Producer（替代 boot 版的 {@code ColaAutoConfiguration}）。
 *
 * <p>当 classpath 存在 COLA 核心类时自动激活（构建期条件），提供：
 * <ul>
 *   <li>{@link Ddd4jResponseHandler} — 让 catchlog 的 ResponseHandler 输出 ddd4j 的
 *       {@code ApiRestResponse} 格式（对应 boot 版注册方式）</li>
 *   <li>{@link ColaExceptionHandler} — COLA {@code BizException} 到 {@code ApiRestResponse}
 *       的 JAX-RS {@code ExceptionMapper} 转换</li>
 *   <li>{@link ColaSysExceptionHandler} — COLA {@code SysException} 到 {@code ApiRestResponse}
 *       的 JAX-RS {@code ExceptionMapper} 转换（HTTP 500）</li>
 * </ul>
 *
 * <p>配置项（{@code application.properties} / {@code application.yml}）：
 * <pre>
 * ddd4j:
 *   cola:
 *     enabled: true  # 默认开启，设为 false 可关闭 COLA 集成
 * </pre>
 *
 * <p>与 boot 版差异：boot 依赖 COLA starter 的 Spring Boot AutoConfiguration +
 * {@code ComponentScan} 激活 COLA 组件（Extension/CatchLog/StateMachine/DomainFactory）；
 * Quarkus 版不加载 Spring 自动配置，本模块只装配 ddd4j 风格的响应与异常处理 Bean，
 * COLA 核心组件由业务项目按 Quarkus 方式（CDI/手动装配）启用。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
@IfBuildProperty(name = "ddd4j.cola.enabled", stringValue = "true", enableIfMissing = true)
public class ColaCdiProducer {

    /**
     * 注册 ddd4j 风格的 COLA ResponseHandler。
     *
     * <p>让 COLA catchlog 的异常处理输出 ddd4j 的 {@code ApiRestResponse} 格式，
     * 而非 COLA 默认的 {@code Response} 格式。
     *
     * @return Ddd4jResponseHandler 实例
     */
    @Produces
    @Singleton
    public Ddd4jResponseHandler ddd4jResponseHandler() {
        return new Ddd4jResponseHandler();
    }

    /**
     * 注册 COLA 业务异常处理器（JAX-RS ExceptionMapper，BizException → HTTP 200 fail）。
     *
     * @return ColaExceptionHandler 实例
     */
    @Produces
    @Singleton
    public ColaExceptionHandler colaExceptionHandler() {
        return new ColaExceptionHandler();
    }

    /**
     * 注册 COLA 系统异常处理器（JAX-RS ExceptionMapper，SysException → HTTP 500）。
     *
     * @return ColaSysExceptionHandler 实例
     */
    @Produces
    @Singleton
    public ColaSysExceptionHandler colaSysExceptionHandler() {
        return new ColaSysExceptionHandler();
    }
}
