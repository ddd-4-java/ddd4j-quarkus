package io.ddd4j.quarkus.annotation.ddd;

import io.ddd4j.annotation.ddd.DDDAnnotation;
import jakarta.enterprise.context.ApplicationScoped;
import java.lang.annotation.*;

/**
 * Quarkus 业务服务 Bean（领域服务）
 * 
 * <p><b>核心目标</b>：业务代码只写一个 @DomainService，同时获得：
 * <ul>
 *   <li>DDD 语义（被 ArchUnit 规则识别）</li>
 *   <li>Quarkus CDI 自动注册为 Bean（@ApplicationScoped 元注解）</li>
 * </ul>
 */
@DDDAnnotation
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ApplicationScoped
@Inherited
public @interface DomainService {
}
