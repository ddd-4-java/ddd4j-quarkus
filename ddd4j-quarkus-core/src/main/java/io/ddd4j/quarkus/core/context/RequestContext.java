package io.ddd4j.quarkus.core.context;

import jakarta.enterprise.context.RequestScoped;
import lombok.Getter;
import lombok.Setter;

/**
 * 请求级上下文（对标 ddd4j-boot-web 的 ThreadContext）。
 */
@RequestScoped
@Getter
@Setter
public class RequestContext {

    private String tenantId;
    private String userId;
}
