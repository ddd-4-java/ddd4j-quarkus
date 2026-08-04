/**
 * COLA 架构组件集成（Quarkus 版）。
 *
 * <p>提供 ddd4j-quarkus 与 Alibaba COLA 组件的集成：
 * <ul>
 *   <li>{@link io.ddd4j.quarkus.cola.ColaCdiProducer} — COLA CDI Producer
 *       （响应 Handler + 异常处理器 Bean，{@code ddd4j.cola.enabled} 构建期条件）</li>
 *   <li>{@link io.ddd4j.quarkus.cola.exception.ColaExceptionHandler} — COLA 业务异常到
 *       {@code ApiRestResponse} 的 JAX-RS {@code ExceptionMapper} 转换</li>
 *   <li>{@link io.ddd4j.quarkus.cola.exception.ColaSysExceptionHandler} — COLA 系统异常到
 *       {@code ApiRestResponse} 的 JAX-RS {@code ExceptionMapper} 转换（HTTP 500）</li>
 *   <li>{@link io.ddd4j.quarkus.cola.handler.Ddd4jResponseHandler} — catchlog 的
 *       ResponseHandler 扩展</li>
 * </ul>
 *
 * <p>boot 版依赖 COLA starter 的 Spring Boot AutoConfiguration，本模块改为 Quarkus
 * CDI 装配（统一响应、异常处理），COLA 核心组件由业务项目按 Quarkus 方式启用。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
package io.ddd4j.quarkus.cola;
