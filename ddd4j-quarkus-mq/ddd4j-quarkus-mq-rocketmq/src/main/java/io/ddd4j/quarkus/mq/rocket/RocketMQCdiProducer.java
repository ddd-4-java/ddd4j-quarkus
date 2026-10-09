package io.ddd4j.quarkus.mq.rocket;

import io.ddd4j.mq.rocketmq.RocketMQProperties;
import io.ddd4j.mq.rocketmq.RocketMQClient;
import io.ddd4j.mq.MQClient;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus RocketMQ CDI producer.
 *
 * <p>暴露：
 * <ul>
 *   <li>{@link RocketMQProperties} —— RocketMQ 特有配置（从 MicroProfile Config 读取）</li>
 *   <li>{@link RocketMQClient} —— {@link MQClient} 实现，供 QuarkusMQListenerRegistrar 使用</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class RocketMQCdiProducer {

    /**
     * 无参构造函数，供 CDI 容器实例化本生产者 Bean。
     */
    public RocketMQCdiProducer() {
    }

    /**
     * 产出 {@link RocketMQProperties} 默认 Bean。
     *
     * @return {@link RocketMQProperties} 实例
     */
    @Produces
    @Singleton
    @DefaultBean
    public RocketMQProperties rocketMQProperties() {
        return new RocketMQProperties();
    }

    /**
     * 依据 {@code RocketMQProperties} 构建并产出 {@link RocketMQClient} 默认 Bean。
     *
     * @param properties MQ 客户端配置
     * @return {@link RocketMQClient} 实例
     */
    @Produces
    @Singleton
    @DefaultBean
    public RocketMQClient rocketMQClient(RocketMQProperties properties) {
        return new RocketMQClient(properties);
    }

}