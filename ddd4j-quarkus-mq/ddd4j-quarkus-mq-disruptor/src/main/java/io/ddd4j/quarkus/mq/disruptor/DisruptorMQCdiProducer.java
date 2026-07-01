package io.ddd4j.quarkus.mq.disruptor;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.disruptor.config.DisruptorMQProperties;
import io.ddd4j.mq.disruptor.consumer.DisruptorMQConsumerEndpointRegistrar;
import io.ddd4j.mq.disruptor.core.DisruptorMQBus;
import io.ddd4j.mq.disruptor.core.DisruptorMQEventDispatcher;
import io.ddd4j.mq.disruptor.spi.DisruptorMQBrokerAdapter;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus Disruptor MQ CDI producer.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class DisruptorMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public DisruptorMQProperties disruptorMQProperties() {
        return new DisruptorMQProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public DisruptorMQEventDispatcher disruptorMQEventDispatcher() {
        return new DisruptorMQEventDispatcher();
    }

    @Produces
    @Singleton
    @DefaultBean
    public DisruptorMQBus disruptorMQBus(
            DisruptorMQProperties disruptorProperties,
            DisruptorMQEventDispatcher dispatcher) {
        return new DisruptorMQBus(disruptorProperties, dispatcher);
    }

    @Produces
    @Singleton
    @DefaultBean
    public DisruptorMQBrokerAdapter disruptorMQBrokerAdapter(
            DisruptorMQBus bus,
            Ddd4jMQProperties mqProperties) {
        return new DisruptorMQBrokerAdapter(
                bus,
                mqProperties,
                new DisruptorMQConsumerEndpointRegistrar(bus));
    }

    @Produces
    @Singleton
    @DefaultBean
    public MQEventPublisher disruptorMQEventPublisher(
            DisruptorMQBrokerAdapter brokerAdapter,
            Ddd4jMQProperties mqProperties) {
        return brokerAdapter.createPublisher(mqProperties);
    }
}
