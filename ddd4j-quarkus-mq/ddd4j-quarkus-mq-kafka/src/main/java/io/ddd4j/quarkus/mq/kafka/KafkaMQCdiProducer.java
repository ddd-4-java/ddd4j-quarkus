package io.ddd4j.quarkus.mq.kafka;

import io.ddd4j.mq.kafka.KafkaMQProperties;
import io.ddd4j.mq.kafka.KafkaMQClient;
import io.ddd4j.mq.MQClient;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus kafka MQ CDI producer.
 *
 * <p>暴露：
 * <ul>
 *   <li>{@link KafkaMQProperties} —— kafka 特有配置（从 MicroProfile Config 读取）</li>
 *   <li>{@link KafkaMQClient} —— {@link MQClient} 实现，供 {@link io.ddd4j.quarkus.mq.core.QuarkusMQListenerRegistrar} 使用</li>
 * </ul>
 *
 * <p>业务项目可提供 {@code @Alternative} 或 {@code @DefaultBean} Bean 覆盖默认实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class KafkaMQCdiProducer {

    @Produces
    @Singleton
    @DefaultBean
    public KafkaMQProperties kafkaMQProperties() {
        return new KafkaMQProperties();
    }

    @Produces
    @Singleton
    @DefaultBean
    public KafkaMQClient kafkaMQClient(KafkaMQProperties properties) {
        // KafkaMQClient 构造器要求 Callback（Kafka producer send 回调），
        // 默认 null 表示不处理发送回调（业务项目可注入自定义 Callback）。
        return new KafkaMQClient(properties, null);
    }

    /**
     * 以 {@link MQClient} 接口暴露，供 QuarkusMQListenerRegistrar 查找活跃 broker。
     */
    @Produces
    @Singleton
    @DefaultBean
    public MQClient mqClient(KafkaMQClient client) {
        return client;
    }
}
