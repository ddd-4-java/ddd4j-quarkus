package io.ddd4j.quarkus.mq.rabbit;

import io.ddd4j.mq.rabbitmq.RabbitMQProperties;
import io.ddd4j.mq.rabbitmq.RabbitMQClient;
import io.ddd4j.mq.MQClient;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus RabbitMQ CDI producer.
 *
 * <p>暴露：
 * <ul>
 *   <li>{@link RabbitMQProperties} —— RabbitMQ 特有配置（从 MicroProfile Config 读取）</li>
 *   <li>{@link RabbitMQClient} —— {@link MQClient} 实现，供 QuarkusMQListenerRegistrar 使用</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class RabbitMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public RabbitMQProperties rabbitMQProperties() {
        return new RabbitMQProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public RabbitMQClient rabbitMQClient(RabbitMQProperties properties) {
        return new RabbitMQClient(properties);
    }

    @Produces
    @Singleton
    @DefaultBean
    public MQClient mqClient(RabbitMQClient client) {
        return client;
    }
}