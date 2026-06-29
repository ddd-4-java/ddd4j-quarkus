package io.ddd4j.quarkus.annotation.ddd;

import io.ddd4j.annotation.ddd.DDDAnnotation;
import jakarta.enterprise.context.ApplicationScoped;
import java.lang.annotation.*;

/**
 * Quarkus 应用服务
 */
@DDDAnnotation
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ApplicationScoped
@Inherited
public @interface ApplicationService {
}
