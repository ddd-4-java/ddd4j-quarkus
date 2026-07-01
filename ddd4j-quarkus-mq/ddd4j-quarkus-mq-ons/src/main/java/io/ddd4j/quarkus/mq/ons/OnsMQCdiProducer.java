package io.ddd4j.quarkus.mq.ons;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.ons.spi.OnsMQBrokerAdapter;
import io.ddd4j.mq.ons.spi.OnsMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.serialization.MQEventSerialization;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus ONS MQ CDI producer.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class OnsMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public OnsMQProperties onsMQProperties() {
        return new OnsMQProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public OnsMQBrokerAdapter onsMQBrokerAdapter(
            OnsMQProperties onsProperties,
            Ddd4jMQProperties mqProperties,
            MQEventSerialization serialization) {
        return new OnsMQBrokerAdapter(onsProperties, mqProperties, serialization);
    }

    @Produces
    @Singleton
    @DefaultBean
    public MQEventPublisher onsMQEventPublisher(
            OnsMQBrokerAdapter brokerAdapter,
            Ddd4jMQProperties mqProperties) {
        return brokerAdapter.createPublisher(mqProperties);
    }
}
