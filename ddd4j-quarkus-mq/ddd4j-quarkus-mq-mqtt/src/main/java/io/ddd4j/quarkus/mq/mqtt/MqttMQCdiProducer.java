package io.ddd4j.quarkus.mq.mqtt;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.mqtt.spi.MqttMQBrokerAdapter;
import io.ddd4j.mq.mqtt.spi.MqttMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.serialization.MQEventSerialization;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus MQTT MQ CDI producer.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class MqttMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public MqttMQProperties mqttMQProperties() {
        return new MqttMQProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public MqttMQBrokerAdapter mqttMQBrokerAdapter(
            MqttMQProperties mqttProperties,
            Ddd4jMQProperties mqProperties,
            MQEventSerialization serialization) {
        return new MqttMQBrokerAdapter(mqttProperties, mqProperties, serialization);
    }

    @Produces
    @Singleton
    @DefaultBean
    public MQEventPublisher mqttMQEventPublisher(
            MqttMQBrokerAdapter brokerAdapter,
            Ddd4jMQProperties mqProperties) {
        return brokerAdapter.createPublisher(mqProperties);
    }
}
