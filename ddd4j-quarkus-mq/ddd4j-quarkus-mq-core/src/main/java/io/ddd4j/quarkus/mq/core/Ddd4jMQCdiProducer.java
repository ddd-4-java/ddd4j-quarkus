package io.ddd4j.quarkus.mq.core;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.serialization.JsonMQMessageSerialization;
import io.ddd4j.mq.serialization.MQEventSerialization;
import io.ddd4j.mq.serialization.MQMessageSerialization;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import jakarta.inject.Singleton;

/**
 * ddd4j MQ 核心 CDI 生产者：为 Quarkus 容器提供 mq-core 契约的默认 Bean。
 *
 * <p>对标 ddd4j-mq-spring 的 {@code Ddd4jMQPropertiesConfiguration}，产出：
 * <ul>
 *   <li>{@link Ddd4jMQProperties} —— 从 MicroProfile Config（{@code ddd4j.mq.*}）构建配置对象</li>
 *   <li>{@link MQMessageSerialization} / {@link MQEventSerialization} —— JSON 序列化默认实现</li>
 * </ul>
 *
 * <p>业务项目可提供 {@code @Alternative} 或同类型的 {@code @ApplicationScoped} Bean 覆盖默认实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class Ddd4jMQCdiProducer {

    /**
     * 从 MicroProfile Config 构建 MQ 配置。
     *
     * <p>读取的关键配置项（与 ddd4j-mq-spring 对齐）：
     * <ul>
     *   <li>{@code ddd4j.mq.enabled} —— 是否启用 MQ（默认 true）</li>
     *   <li>{@code ddd4j.mq.namespace} —— 默认命名空间</li>
     *   <li>{@code ddd4j.mq.broker-type} —— broker 类型</li>
     * </ul>
     *
     * @return MQ 配置对象
     */
    @Produces
    @Singleton
    public Ddd4jMQProperties ddd4jMQProperties() {
        Config config = ConfigProvider.getConfig();
        Ddd4jMQProperties props = new Ddd4jMQProperties();
        props.setEnabled(config.getOptionalValue("ddd4j.mq.enabled", Boolean.class).orElse(false));
        props.setNamespace(config.getOptionalValue("ddd4j.mq.namespace", String.class).orElse(""));
        props.setBroker(config.getOptionalValue("ddd4j.mq.broker", String.class).orElse("none"));
        props.setDefaultTopic(config.getOptionalValue("ddd4j.mq.default-topic", String.class).orElse("DEFAULT"));
        // 消费端确认模式
        String ackMode = config.getOptionalValue("ddd4j.mq.consumer.ack-mode", String.class).orElse("manual");
        props.getConsumer().setAckMode(ackMode);
        return props;
    }

    /**
     * 默认 JSON 消息序列化实现（同时作为 {@link MQEventSerialization}）。
     *
     * @return JSON 序列化器
     */
    @Produces
    @Singleton
    public JsonMQMessageSerialization jsonMQMessageSerialization() {
        return new JsonMQMessageSerialization();
    }
}
