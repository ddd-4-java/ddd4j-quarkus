package io.ddd4j.quarkus.core.extension;

import io.ddd4j.quarkus.annotation.ddd.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.*;
import org.jboss.logging.Logger;

/**
 * CDI 扩展：扫描 DDD 构造型注解并自动注册为 CDI Bean
 * <p>
 * 扫描以下注解（完整 10 个 DDD 注解）：
 * <ul>
 *   <li>{@link ApplicationService} -> @ApplicationScoped</li>
 *   <li>{@link DomainService} -> @ApplicationScoped</li>
 *   <li>{@link DomainRepository} -> @ApplicationScoped</li>
 *   <li>{@link DomainAssembler} -> @ApplicationScoped</li>
 *   <li>{@link DomainConverter} -> @ApplicationScoped</li>
 *   <li>{@link DomainEntity} -> @ApplicationScoped</li>
 *   <li>{@link DomainValueObject} -> @ApplicationScoped</li>
 *   <li>{@link DomainGateway} -> @ApplicationScoped</li>
 *   <li>{@link QueryService} -> @ApplicationScoped</li>
 *   <li>{@link CommandExecutor} -> @ApplicationScoped</li>
 * </ul>
 *
 * @author Loong Wan
 */
public class DddCdiExtension implements Extension {

    private static final Logger logger = Logger.getLogger(DddCdiExtension.class);

    /**
     * 扫描带有 DDD 构造型注解的类，添加 @ApplicationScoped 注解
     */
    <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> event) {
        AnnotatedType<T> type = event.getAnnotatedType();

        // 检查是否带有 DDD 构造型注解
        boolean hasDddAnnotation = false;
        String annotationName = null;

        if (type.isAnnotationPresent(ApplicationService.class)) {
            hasDddAnnotation = true;
            annotationName = "@ApplicationService";
        } else if (type.isAnnotationPresent(DomainService.class)) {
            hasDddAnnotation = true;
            annotationName = "@DomainService";
        } else if (type.isAnnotationPresent(DomainRepository.class)) {
            hasDddAnnotation = true;
            annotationName = "@DomainRepository";
        } else if (type.isAnnotationPresent(DomainAssembler.class)) {
            hasDddAnnotation = true;
            annotationName = "@DomainAssembler";
        } else if (type.isAnnotationPresent(DomainConverter.class)) {
            hasDddAnnotation = true;
            annotationName = "@DomainConverter";
        } else if (type.isAnnotationPresent(DomainEntity.class)) {
            hasDddAnnotation = true;
            annotationName = "@DomainEntity";
        } else if (type.isAnnotationPresent(DomainValueObject.class)) {
            hasDddAnnotation = true;
            annotationName = "@DomainValueObject";
        } else if (type.isAnnotationPresent(DomainGateway.class)) {
            hasDddAnnotation = true;
            annotationName = "@DomainGateway";
        } else if (type.isAnnotationPresent(QueryService.class)) {
            hasDddAnnotation = true;
            annotationName = "@QueryService";
        } else if (type.isAnnotationPresent(CommandExecutor.class)) {
            hasDddAnnotation = true;
            annotationName = "@CommandExecutor";
        }

        if (hasDddAnnotation) {
            // 如果没有 @ApplicationScoped，则添加
            if (!type.isAnnotationPresent(ApplicationScoped.class)) {
                logger.debugf("Adding @ApplicationScoped to %s %s", annotationName, type.getJavaClass().getName());
                event.configureAnnotatedType().add(new ApplicationScopedLiteral());
            }
        }
    }

    /**
     * ApplicationScoped 注解字面量
     */
    private static class ApplicationScopedLiteral extends jakarta.enterprise.util.AnnotationLiteral<ApplicationScoped>
            implements ApplicationScoped {
        private static final long serialVersionUID = 1L;
    }
}
