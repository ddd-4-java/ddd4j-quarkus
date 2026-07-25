package io.ddd4j.quarkus.mq.activemq;

import io.ddd4j.mq.activemq.ActiveMQProperties;
import io.ddd4j.mq.activemq.ActiveMQClient;
import io.ddd4j.mq.MQClient;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus activemq MQ CDI producer.
 *
 * <p>暴露：
 * <ul>
 *   <li>{@link ActiveMQProperties} —— activemq 特有配置（从 MicroProfile Config 读取）</li>
 *   <li>{@link ActiveMQClient} —— {@link MQClient} 实现，供 {@link io.ddd4j.quarkus.mq.core.QuarkusMQListenerRegistrar} 使用</li>
 * </ul>
 *
 * <p>业务项目可提供 {@code @Alternative} 或 {@code @DefaultBean} Bean 覆盖默认实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class ActiveMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public ActiveMQProperties activeMQProperties() {
        return new ActiveMQProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public ActiveMQClient activeMQClient(ActiveMQProperties properties) {
        return new ActiveMQClient(properties);
    }

    /**
     * 以 {@link MQClient} 接口暴露，供 QuarkusMQListenerRegistrar 查找活跃 broker。
     */
    @Produces
    @Singleton
    @DefaultBean
    public MQClient mqClient(ActiveMQClient client) {
        return client;
    }
}
