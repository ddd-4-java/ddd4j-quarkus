package io.ddd4j.quarkus.mq.mqttmica;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.mqttmica.spi.MicaMqttMQBrokerAdapter;
import io.ddd4j.mq.mqttmica.spi.MicaMqttProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.serialization.MQEventSerialization;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus mica-mqtt MQ CDI producer.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class MicaMqttMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public MicaMqttProperties micaMqttProperties() {
        return new MicaMqttProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public MicaMqttMQBrokerAdapter micaMqttMQBrokerAdapter(
            MicaMqttProperties micaMqttProperties,
            Ddd4jMQProperties mqProperties,
            MQEventSerialization serialization) {
        return new MicaMqttMQBrokerAdapter(micaMqttProperties, mqProperties, serialization);
    }

    @Produces
    @Singleton
    @DefaultBean
    public MQEventPublisher micaMqttMQEventPublisher(
            MicaMqttMQBrokerAdapter brokerAdapter,
            Ddd4jMQProperties mqProperties) {
        return brokerAdapter.createPublisher(mqProperties);
    }
}
