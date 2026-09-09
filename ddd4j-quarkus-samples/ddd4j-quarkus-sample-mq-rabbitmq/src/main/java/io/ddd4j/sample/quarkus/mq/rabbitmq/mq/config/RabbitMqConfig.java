package io.ddd4j.sample.quarkus.mq.rabbitmq.mq.config;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.rabbitmq.RabbitMQClient;
import io.ddd4j.mq.rabbitmq.RabbitMQProperties;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.Config;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ 示例装配：声明具名 topic exchange，并让通用路由与连接使用同一套属性。
 *
 * <p>客户端负责队列、绑定、发布和消费；示例负责交换机及原生连接生命周期。
 */
@Slf4j
@ApplicationScoped
public class RabbitMqConfig {

    /** 显式保留客户端供启动注册器发现；原生连接由 CDI disposer 回收。 */
    void shutdown(@Observes ShutdownEvent event, RabbitMQClient client) throws Exception {
        client.close();
    }

    /** 将连接、交换机和通用路由配置绑定到同一个属性对象。 */
    @Produces
    @Singleton
    @Typed(RabbitMQProperties.class)
    public RabbitMQProperties rabbitMQProperties(Config config) {
        RabbitMQProperties props = new RabbitMQProperties();
        props.setEnabled(config.getValue("ddd4j.mq.enabled", Boolean.class));
        props.setBroker(config.getValue("ddd4j.mq.broker", String.class));
        props.setNamespace(config.getValue("ddd4j.mq.namespace", String.class));
        props.setAutoAck(config.getOptionalValue("ddd4j.mq.auto-ack", Boolean.class).orElse(false));
        props.setHost(config.getValue("ddd4j.mq.rabbitmq.host", String.class));
        props.setPort(config.getValue("ddd4j.mq.rabbitmq.port", Integer.class));
        props.setUsername(config.getValue("ddd4j.mq.rabbitmq.username", String.class));
        props.setPassword(config.getValue("ddd4j.mq.rabbitmq.password", String.class));
        props.setVirtualHost(config.getValue("ddd4j.mq.rabbitmq.virtual-host", String.class));
        props.setExchange(config.getValue("ddd4j.mq.rabbitmq.exchange", String.class));
        props.setDurable(config.getValue("ddd4j.mq.rabbitmq.durable", Boolean.class));
        if (StrKit.isBlank(props.getExchange())) {
            throw new IllegalArgumentException("RabbitMQ sample requires a named exchange");
        }
        return props;
    }

    /**
     * 框架通用属性生产者未绑定 exchange，使用同一 RabbitMQProperties 替代以保持路由一致。
     */
    @Produces
    @Alternative
    @Priority(1)
    @Singleton
    public MQProperties routingProperties(RabbitMQProperties properties) {
        return properties;
    }

    /** 在注册消费者之前声明交换机；失败时立即关闭已建立的连接。 */
    @Produces
    @Singleton
    public Connection rabbitConnection(RabbitMQProperties properties) throws IOException, TimeoutException {
        Connection connection = properties.connectionFactory().newConnection();
        try {
            try (Channel channel = connection.createChannel()) {
                channel.exchangeDeclare(properties.getExchange(), "topic", properties.isDurable());
            }
            log.info("RabbitMQ exchange ready: host={}:{}, exchange={}",
                    properties.getHost(), properties.getPort(), properties.getExchange());
            return connection;
        } catch (IOException | TimeoutException | RuntimeException failure) {
            try {
                connection.close();
            } catch (IOException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw failure;
        }
    }

    /** 客户端复用已声明交换机的连接，仍由框架注册真实 listener。 */
    @Produces
    @Singleton
    public RabbitMQClient rabbitMQClient(Connection connection) {
        return new RabbitMQClient(connection);
    }

    /** CDI 销毁应用时关闭生产者和消费者共用的原生连接及其 channels。 */
    public void closeConnection(@Disposes Connection connection) throws IOException {
        connection.close();
        log.info("RabbitMQ connection closed: {}", !connection.isOpen());
    }
}
