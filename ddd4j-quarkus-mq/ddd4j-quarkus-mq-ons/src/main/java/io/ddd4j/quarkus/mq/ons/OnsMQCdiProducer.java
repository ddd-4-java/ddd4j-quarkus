package io.ddd4j.quarkus.mq.ons;

import io.ddd4j.mq.ons.OnsProperties;
import io.ddd4j.mq.ons.OnsMQClient;
import io.ddd4j.mq.MQClient;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus ons MQ CDI producer.
 *
 * <p>暴露：
 * <ul>
 *   <li>{@link OnsProperties} —— ons 特有配置（从 MicroProfile Config 读取）</li>
 *   <li>{@link OnsMQClient} —— {@link MQClient} 实现，供 {@link io.ddd4j.quarkus.mq.core.QuarkusMQListenerRegistrar} 使用</li>
 * </ul>
 *
 * <p>业务项目可提供 {@code @Alternative} 或 {@code @DefaultBean} Bean 覆盖默认实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class OnsMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public OnsProperties onsProperties() {
        return new OnsProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public OnsMQClient onsMQClient(OnsProperties properties) {
        return new OnsMQClient(properties);
    }

}
