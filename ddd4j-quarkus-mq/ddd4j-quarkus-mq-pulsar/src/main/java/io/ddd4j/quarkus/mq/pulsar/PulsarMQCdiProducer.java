package io.ddd4j.quarkus.mq.pulsar;

import io.ddd4j.mq.pulsar.PulsarProperties;
import io.ddd4j.mq.pulsar.PulsarMQClient;
import io.ddd4j.mq.MQClient;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus pulsar MQ CDI producer.
 *
 * <p>暴露：
 * <ul>
 *   <li>{@link PulsarProperties} —— pulsar 特有配置（从 MicroProfile Config 读取）</li>
 *   <li>{@link PulsarMQClient} —— {@link MQClient} 实现，供 {@link io.ddd4j.quarkus.mq.core.QuarkusMQListenerRegistrar} 使用</li>
 * </ul>
 *
 * <p>业务项目可提供 {@code @Alternative} 或 {@code @DefaultBean} Bean 覆盖默认实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class PulsarMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public PulsarProperties pulsarProperties() {
        return new PulsarProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public PulsarMQClient pulsarMQClient(PulsarProperties properties) {
        return new PulsarMQClient(properties);
    }

    /**
     * 以 {@link MQClient} 接口暴露，供 QuarkusMQListenerRegistrar 查找活跃 broker。
     */
    @Produces
    @Singleton
    @DefaultBean
    public MQClient mqClient(PulsarMQClient client) {
        return client;
    }
}
