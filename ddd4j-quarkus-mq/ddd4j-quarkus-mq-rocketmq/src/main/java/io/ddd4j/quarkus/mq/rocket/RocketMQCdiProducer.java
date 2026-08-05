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

    @Produces
    @Singleton
    @DefaultBean
    public RocketMQProperties rocketMQProperties() {
        return new RocketMQProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public RocketMQClient rocketMQClient(RocketMQProperties properties) {
        return new RocketMQClient(properties);
    }

}