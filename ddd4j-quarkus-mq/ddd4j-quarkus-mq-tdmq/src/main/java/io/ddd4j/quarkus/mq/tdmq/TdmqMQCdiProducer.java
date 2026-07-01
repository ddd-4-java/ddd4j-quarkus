package io.ddd4j.quarkus.mq.tdmq;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.serialization.MQEventSerialization;
import io.ddd4j.mq.tdmq.client.TdmqClient;
import io.ddd4j.mq.tdmq.client.TdmqClientPlaceholder;
import io.ddd4j.mq.tdmq.spi.TdmqMQBrokerAdapter;
import io.ddd4j.mq.tdmq.spi.TdmqMQProperties;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Quarkus TDMQ CDI 生产者。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class TdmqMQCdiProducer {

    @Produces
    @Singleton
    public TdmqMQProperties tdmqMQProperties() {
        Config config = ConfigProvider.getConfig();
        TdmqMQProperties properties = new TdmqMQProperties();
        properties.setServiceUrl(config.getOptionalValue("ddd4j.mq.tdmq.service-url", String.class).orElse(null));
        properties.setTenant(config.getOptionalValue("ddd4j.mq.tdmq.tenant", String.class).orElse(null));
        properties.setNamespace(config.getOptionalValue("ddd4j.mq.tdmq.namespace", String.class).orElse(null));
        properties.setAccessKey(config.getOptionalValue("ddd4j.mq.tdmq.access-key", String.class).orElse(null));
        properties.setSecretKey(config.getOptionalValue("ddd4j.mq.tdmq.secret-key", String.class).orElse(null));
        properties.setDefaultGroup(config.getOptionalValue("ddd4j.mq.tdmq.default-group", String.class).orElse("ddd4j-tdmq"));
        properties.setAutoStartConsumers(config.getOptionalValue("ddd4j.mq.tdmq.auto-start-consumers", Boolean.class).orElse(true));
        properties.setRequeueOnError(config.getOptionalValue("ddd4j.mq.tdmq.requeue-on-error", Boolean.class).orElse(true));
        return properties;
    }

    @Produces
    @Singleton
    public TdmqClient tdmqClient() {
        return new TdmqClientPlaceholder();
    }

    @Produces
    @Singleton
    public TdmqMQBrokerAdapter tdmqMQBrokerAdapter(
            TdmqClient tdmqClient,
            TdmqMQProperties tdmqProperties,
            Ddd4jMQProperties mqProperties,
            MQEventSerialization serialization) {
        return new TdmqMQBrokerAdapter(tdmqClient, tdmqProperties, mqProperties, serialization);
    }

    @Produces
    @Singleton
    public MQEventPublisher tdmqMQEventPublisher(
            TdmqMQBrokerAdapter tdmqMQBrokerAdapter,
            Ddd4jMQProperties mqProperties) {
        return tdmqMQBrokerAdapter.createPublisher(mqProperties);
    }
}
