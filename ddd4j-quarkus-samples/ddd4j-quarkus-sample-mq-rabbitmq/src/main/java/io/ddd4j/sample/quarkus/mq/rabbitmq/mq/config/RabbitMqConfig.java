package io.ddd4j.sample.quarkus.mq.rabbitmq.mq.config;

import io.ddd4j.mq.rabbitmq.RabbitMQProperties;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

/**
 * Quarkus CDI 配置：为 RabbitMQ MQ 提供配置 Bean，覆盖 ddd4j-quarkus-mq-rabbitmq
 * 默认的 {@link RabbitMQProperties}（{@code @DefaultBean}）。
 *
 * <h3>装配关系</h3>
 * <p>ddd4j-quarkus-mq-rabbitmq 已通过 {@code RabbitMQCdiProducer} 提供：
 * <ul>
 *   <li>{@link RabbitMQProperties}（{@code @DefaultBean}，默认值）</li>
 *   <li>{@code RabbitMQBrokerAdapter}（{@code @DefaultBean}）</li>
 *   <li>{@code MQEventPublisher}（{@code @DefaultBean}）</li>
 * </ul>
 * 本类通过 {@code @DefaultBean} 重新声明 {@link RabbitMQProperties}，
 * 从 MicroProfile Config 读取 {@code ddd4j.mq.rabbit.*} 前缀配置，
 * 从而覆盖默认值，注入到 Broker Adapter 中。
 *
 * <h3>RabbitMQ 拓扑说明</h3>
 * <ul>
 *   <li><b>Exchange</b>（交换机）：{@code ddd4j.mq.exchange}，类型 Topic，durable=true。
 *       Exchange 由 {@code RabbitMQBrokerAdapter} 在首次连接时自动 declare（{@code auto-declare=true}）。</li>
 *   <li><b>Queue</b>（队列）：由 {@code RabbitMQConsumerEndpointRegistrar} 根据
 *       {@link io.ddd4j.mq.annotation.MQEventListener} 自动生成（命名规则见
 *       {@code io.ddd4j.mq.registry.MQListenerEndpointNaming}），durable=true。</li>
 *   <li><b>Routing Key</b>（路由键）：{@code <namespace>.<topic>.<tag>}，
 *       例如 {@code quarkus-rabbitmq-sample.ORDER.CREATED}。</li>
 *   <li><b>Binding</b>（绑定）：消费者启动时按 routing key 自动 bind 到 Exchange。</li>
 *   <li><b>ACK 模式</b>：{@code manual}，由 {@link io.ddd4j.mq.rabbitmq.RabbitMessageAcknowledgment}
 *       处理 basicAck / basicNack。</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class RabbitMqConfig {

    private static final Logger log = Logger.getLogger(RabbitMqConfig.class);

    /**
     * 从 MicroProfile Config 构建 {@link RabbitMQProperties}，覆盖 ddd4j-quarkus-mq-rabbitmq 默认 Bean。
     *
     * <p>读取的配置项（与 {@code application.properties} 对齐）：
     * <ul>
     *   <li>{@code ddd4j.mq.rabbit.host} —— 默认为 {@code localhost}</li>
     *   <li>{@code ddd4j.mq.rabbit.port} —— 默认为 {@code 5672}</li>
     *   <li>{@code ddd4j.mq.rabbit.username} —— 默认为 {@code guest}</li>
     *   <li>{@code ddd4j.mq.rabbit.password} —— 默认为 {@code guest}</li>
     *   <li>{@code ddd4j.mq.rabbit.virtual-host} —— 默认为 {@code /}</li>
     *   <li>{@code ddd4j.mq.rabbit.exchange} —— 默认为 {@code ddd4j.mq.exchange}</li>
     *   <li>{@code ddd4j.mq.rabbit.durable} —— 默认为 {@code true}</li>
     *   <li>{@code ddd4j.mq.rabbit.auto-declare} —— 默认为 {@code true}</li>
     * </ul>
     *
     * @return 已绑定的 RabbitMQ 配置对象
     */
    @Produces
    @Singleton
    @DefaultBean
    public RabbitMQProperties rabbitMQProperties() {
        Config config = ConfigProvider.getConfig();
        RabbitMQProperties props = new RabbitMQProperties();

        props.setHost(config.getOptionalValue("ddd4j.mq.rabbit.host", String.class).orElse("localhost"));
        props.setPort(config.getOptionalValue("ddd4j.mq.rabbit.port", Integer.class).orElse(5672));
        props.setUsername(config.getOptionalValue("ddd4j.mq.rabbit.username", String.class).orElse("guest"));
        props.setPassword(config.getOptionalValue("ddd4j.mq.rabbit.password", String.class).orElse("guest"));
        props.setVirtualHost(config.getOptionalValue("ddd4j.mq.rabbit.virtual-host", String.class).orElse("/"));
        props.setExchange(config.getOptionalValue("ddd4j.mq.rabbit.exchange", String.class).orElse("ddd4j.mq.exchange"));
        props.setDurable(config.getOptionalValue("ddd4j.mq.rabbit.durable", Boolean.class).orElse(true));
        props.setAutoDeclare(config.getOptionalValue("ddd4j.mq.rabbit.auto-declare", Boolean.class).orElse(true));

        log.infof("RabbitMQ properties: host=%s:%d, vhost=%s, exchange=%s, durable=%s, autoDeclare=%s",
                props.getHost(), props.getPort(), props.getVirtualHost(),
                props.getExchange(), props.isDurable(), props.isAutoDeclare());

        return props;
    }
}