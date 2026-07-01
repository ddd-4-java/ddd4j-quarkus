package io.ddd4j.quarkus.mq.rabbit;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.rabbit.RabbitMQBrokerAdapter;
import io.ddd4j.mq.rabbit.RabbitMQProperties;
import io.ddd4j.mq.serialization.MQEventSerialization;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus RabbitMQ CDI producer.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
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
    public RabbitMQBrokerAdapter rabbitMQBrokerAdapter(
            RabbitMQProperties rabbitProperties,
            Ddd4jMQProperties mqProperties,
            MQEventSerialization serialization) {
        return new RabbitMQBrokerAdapter(rabbitProperties, mqProperties, serialization, null);
    }

    @Produces
    @Singleton
    @DefaultBean
    public MQEventPublisher rabbitMQEventPublisher(
            RabbitMQBrokerAdapter brokerAdapter,
            Ddd4jMQProperties mqProperties) {
        return brokerAdapter.createPublisher(mqProperties);
    }
}
