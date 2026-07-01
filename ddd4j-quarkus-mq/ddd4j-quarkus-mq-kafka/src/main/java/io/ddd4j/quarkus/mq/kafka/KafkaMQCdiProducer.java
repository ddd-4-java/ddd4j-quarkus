package io.ddd4j.quarkus.mq.kafka;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.kafka.KafkaMQBrokerAdapter;
import io.ddd4j.mq.kafka.KafkaMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.serialization.MQEventSerialization;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus Kafka MQ CDI producer.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class KafkaMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public KafkaMQProperties kafkaMQProperties() {
        return new KafkaMQProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public KafkaMQBrokerAdapter kafkaMQBrokerAdapter(
            KafkaMQProperties kafkaProperties,
            Ddd4jMQProperties mqProperties,
            MQEventSerialization serialization) {
        return new KafkaMQBrokerAdapter(kafkaProperties, mqProperties, serialization);
    }

    @Produces
    @Singleton
    @DefaultBean
    public MQEventPublisher kafkaMQEventPublisher(
            KafkaMQBrokerAdapter brokerAdapter,
            Ddd4jMQProperties mqProperties) {
        return brokerAdapter.createPublisher(mqProperties);
    }
}
