package io.ddd4j.quarkus.mq.redisstream;

import io.ddd4j.mq.redisstream.RedisStreamMQProperties;
import io.ddd4j.mq.redisstream.RedisStreamMQClient;
import io.ddd4j.mq.MQClient;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus redis-stream MQ CDI producer.
 *
 * <p>暴露：
 * <ul>
 *   <li>{@link RedisStreamMQProperties} —— redis-stream 特有配置（从 MicroProfile Config 读取）</li>
 *   <li>{@link RedisStreamMQClient} —— {@link MQClient} 实现，供 {@link io.ddd4j.quarkus.mq.core.QuarkusMQListenerRegistrar} 使用</li>
 * </ul>
 *
 * <p>业务项目可提供 {@code @Alternative} 或 {@code @DefaultBean} Bean 覆盖默认实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class RedisStreamMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public RedisStreamMQProperties redisStreamMQProperties() {
        return new RedisStreamMQProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public RedisStreamMQClient redisStreamMQClient(RedisStreamMQProperties properties) {
        return new RedisStreamMQClient(properties);
    }

}
