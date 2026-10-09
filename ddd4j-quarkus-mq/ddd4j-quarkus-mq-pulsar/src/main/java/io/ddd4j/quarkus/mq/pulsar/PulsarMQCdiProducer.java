package io.ddd4j.quarkus.mq.pulsar;

import io.ddd4j.mq.pulsar.PulsarProperties;
import io.ddd4j.mq.pulsar.PulsarMQClient;
import io.ddd4j.mq.MQClient;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus pulsar MQ CDI producer.
 *
 * <p>暴露：
 * <ul>
 *   <li>{@link PulsarProperties} —— pulsar 特有配置（从 MicroProfile Config 读取）</li>
 *   <li>{@link PulsarMQClient} —— {@link MQClient} 实现，供 {@link io.ddd4j.quarkus.mq.core.QuarkusMQListenerRegistrar} 使用</li>
 * </ul>
 *
 * <p>业务项目可提供 {@code @Alternative} 或 {@code @DefaultBean} Bean 覆盖默认实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class PulsarMQCdiProducer {

    /**
     * 无参构造函数，供 CDI 容器实例化本生产者 Bean。
     */
    public PulsarMQCdiProducer() {
    }

    /**
     * 产出 {@link PulsarProperties} 默认 Bean。
     *
     * @return {@link PulsarProperties} 实例
     */
    @Produces
    @Singleton
    @DefaultBean
    public PulsarProperties pulsarProperties() {
        return new PulsarProperties();
    }

    /**
     * 依据 {@code PulsarProperties} 构建并产出 {@link PulsarMQClient} 默认 Bean。
     *
     * @param properties MQ 客户端配置
     * @return {@link PulsarMQClient} 实例
     */
    @Produces
    @Singleton
    @DefaultBean
    public PulsarMQClient pulsarMQClient(PulsarProperties properties) {
        return new PulsarMQClient(properties);
    }

}
