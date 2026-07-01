package io.ddd4j.quarkus.mq.sqs;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.serialization.MQEventSerialization;
import io.ddd4j.mq.sqs.spi.SqsBrokerAdapter;
import io.ddd4j.mq.sqs.spi.SqsMQProperties;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus SQS MQ CDI producer.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class SqsMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public SqsMQProperties sqsMQProperties() {
        return new SqsMQProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public SqsBrokerAdapter sqsBrokerAdapter(
            SqsMQProperties sqsProperties,
            Ddd4jMQProperties mqProperties,
            MQEventSerialization serialization) {
        return new SqsBrokerAdapter(sqsProperties, mqProperties, serialization);
    }

    @Produces
    @Singleton
    @DefaultBean
    public MQEventPublisher sqsMQEventPublisher(
            SqsBrokerAdapter brokerAdapter,
            Ddd4jMQProperties mqProperties) {
        return brokerAdapter.createPublisher(mqProperties);
    }
}
