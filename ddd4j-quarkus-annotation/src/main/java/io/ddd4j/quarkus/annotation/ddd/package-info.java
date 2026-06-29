/**
 * ddd4j-quarkus DDD 注解包
 * 
 * <p>11 个 DDD 构造型注解的同名复制 + Jakarta CDI 元注解融合实现。
 * 每个注解都是 {@link io.ddd4j.annotation.ddd.DDDAnnotation} 元注解标记，
 * 同时底层融合 Jakarta CDI 的 {@code @ApplicationScoped}，实现"业务代码只写一个注解"。
 *
 * @see io.ddd4j.annotation.ddd.DDDAnnotation
 */
package io.ddd4j.quarkus.annotation.ddd;
