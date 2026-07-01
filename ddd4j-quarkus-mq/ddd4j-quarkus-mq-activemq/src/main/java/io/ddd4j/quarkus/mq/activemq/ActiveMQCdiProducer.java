package io.ddd4j.quarkus.mq.activemq;

import io.ddd4j.mq.activemq.config.ActiveMQProperties;
import io.ddd4j.mq.activemq.spi.ActiveMQBrokerAdapter;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.serialization.MQEventSerialization;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus ActiveMQ CDI producer.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
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
    public ActiveMQBrokerAdapter activeMQBrokerAdapter(
            ActiveMQProperties activeMQProperties,
            Ddd4jMQProperties mqProperties,
            MQEventSerialization serialization) {
        return new ActiveMQBrokerAdapter(activeMQProperties, mqProperties, serialization);
    }

    @Produces
    @Singleton
    @DefaultBean
    public MQEventPublisher activeMQEventPublisher(
            ActiveMQBrokerAdapter brokerAdapter,
            Ddd4jMQProperties mqProperties) {
        return brokerAdapter.createPublisher(mqProperties);
    }
}
