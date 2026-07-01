package io.ddd4j.quarkus.mq.pulsar;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.pulsar.spi.PulsarMQBrokerAdapter;
import io.ddd4j.mq.pulsar.spi.PulsarMQProperties;
import io.ddd4j.mq.serialization.MQEventSerialization;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus Pulsar MQ CDI producer.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class PulsarMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public PulsarMQProperties pulsarMQProperties() {
        return new PulsarMQProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public PulsarMQBrokerAdapter pulsarMQBrokerAdapter(
            PulsarMQProperties pulsarProperties,
            Ddd4jMQProperties mqProperties,
            MQEventSerialization serialization) {
        return new PulsarMQBrokerAdapter(pulsarProperties, mqProperties, serialization);
    }

    @Produces
    @Singleton
    @DefaultBean
    public MQEventPublisher pulsarMQEventPublisher(
            PulsarMQBrokerAdapter brokerAdapter,
            Ddd4jMQProperties mqProperties) {
        return brokerAdapter.createPublisher(mqProperties);
    }
}
