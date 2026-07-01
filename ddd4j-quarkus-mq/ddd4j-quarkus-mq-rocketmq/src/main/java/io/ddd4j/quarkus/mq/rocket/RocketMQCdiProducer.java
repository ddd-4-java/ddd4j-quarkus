package io.ddd4j.quarkus.mq.rocket;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.rocketmq.RocketMQBrokerAdapter;
import io.ddd4j.mq.rocketmq.RocketMQProperties;
import io.ddd4j.mq.serialization.MQEventSerialization;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus RocketMQ CDI producer.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class RocketMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public RocketMQProperties rocketMQProperties() {
        return new RocketMQProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public RocketMQBrokerAdapter rocketMQBrokerAdapter(
            RocketMQProperties rocketProperties,
            Ddd4jMQProperties mqProperties,
            MQEventSerialization serialization) {
        return new RocketMQBrokerAdapter(rocketProperties, mqProperties, serialization);
    }

    @Produces
    @Singleton
    @DefaultBean
    public MQEventPublisher rocketMQEventPublisher(
            RocketMQBrokerAdapter brokerAdapter,
            Ddd4jMQProperties mqProperties) {
        return brokerAdapter.createPublisher(mqProperties);
    }
}
