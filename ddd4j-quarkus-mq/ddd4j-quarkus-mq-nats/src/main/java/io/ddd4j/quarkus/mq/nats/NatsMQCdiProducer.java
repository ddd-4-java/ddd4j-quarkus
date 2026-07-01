package io.ddd4j.quarkus.mq.nats;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.nats.consumer.NatsMQConsumerEndpointRegistrar;
import io.ddd4j.mq.nats.spi.NatsMQBrokerAdapter;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Quarkus NATS MQ CDI producer.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class NatsMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public Connection natsConnection() throws Exception {
        String server = ConfigProvider.getConfig()
                .getOptionalValue("ddd4j.mq.nats.server", String.class)
                .orElse("nats://localhost:4222");
        return Nats.connect(server);
    }

    @Produces
    @Singleton
    @DefaultBean
    public NatsMQBrokerAdapter natsMQBrokerAdapter(
            Connection connection,
            Ddd4jMQProperties mqProperties) {
        return new NatsMQBrokerAdapter(
                connection,
                mqProperties,
                new NatsMQConsumerEndpointRegistrar(connection, mqProperties));
    }

    @Produces
    @Singleton
    @DefaultBean
    public MQEventPublisher natsMQEventPublisher(
            NatsMQBrokerAdapter brokerAdapter,
            Ddd4jMQProperties mqProperties) {
        return brokerAdapter.createPublisher(mqProperties);
    }
}
