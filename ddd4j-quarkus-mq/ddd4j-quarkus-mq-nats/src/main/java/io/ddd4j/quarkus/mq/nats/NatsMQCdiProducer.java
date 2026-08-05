package io.ddd4j.quarkus.mq.nats;

import io.ddd4j.mq.nats.NatsProperties;
import io.ddd4j.mq.nats.NatsMQClient;
import io.ddd4j.mq.MQClient;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus nats MQ CDI producer.
 *
 * <p>暴露：
 * <ul>
 *   <li>{@link NatsProperties} —— nats 特有配置（从 MicroProfile Config 读取）</li>
 *   <li>{@link NatsMQClient} —— {@link MQClient} 实现，供 {@link io.ddd4j.quarkus.mq.core.QuarkusMQListenerRegistrar} 使用</li>
 * </ul>
 *
 * <p>业务项目可提供 {@code @Alternative} 或 {@code @DefaultBean} Bean 覆盖默认实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class NatsMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public NatsProperties natsProperties() {
        return new NatsProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public NatsMQClient natsMQClient(NatsProperties properties) {
        return new NatsMQClient(properties);
    }

}
