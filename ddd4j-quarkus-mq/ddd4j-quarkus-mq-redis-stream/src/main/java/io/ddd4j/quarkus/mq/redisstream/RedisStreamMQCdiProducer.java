package io.ddd4j.quarkus.mq.redisstream;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.redisstream.RedisStreamMQBrokerAdapter;
import io.ddd4j.mq.redisstream.RedisStreamMQProperties;
import io.ddd4j.mq.serialization.MQEventSerialization;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus Redis Stream MQ CDI producer.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class RedisStreamMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public RedisStreamMQProperties redisStreamMQProperties() {
        return new RedisStreamMQProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public RedisStreamMQBrokerAdapter redisStreamMQBrokerAdapter(
            RedisStreamMQProperties redisProperties,
            Ddd4jMQProperties mqProperties,
            MQEventSerialization serialization) {
        return new RedisStreamMQBrokerAdapter(
                redisProperties,
                mqProperties,
                serialization,
                redisProperties.newOperations());
    }

    @Produces
    @Singleton
    @DefaultBean
    public MQEventPublisher redisStreamMQEventPublisher(
            RedisStreamMQBrokerAdapter brokerAdapter,
            Ddd4jMQProperties mqProperties) {
        return brokerAdapter.createPublisher(mqProperties);
    }
}
