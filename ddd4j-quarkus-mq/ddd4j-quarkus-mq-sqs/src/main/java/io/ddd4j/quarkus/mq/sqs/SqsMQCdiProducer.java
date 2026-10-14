package io.ddd4j.quarkus.mq.sqs;

import io.ddd4j.mq.sqs.SqsProperties;
import io.ddd4j.mq.sqs.SqsMQClient;
import io.ddd4j.mq.MQClient;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus sqs MQ CDI producer.
 *
 * <p>暴露：
 * <ul>
 *   <li>{@link SqsProperties} —— sqs 特有配置（从 MicroProfile Config 读取）</li>
 *   <li>{@link SqsMQClient} —— {@link MQClient} 实现，供 {@link io.ddd4j.quarkus.mq.core.QuarkusMQListenerRegistrar} 使用</li>
 * </ul>
 *
 * <p>业务项目可提供 {@code @Alternative} 或 {@code @DefaultBean} Bean 覆盖默认实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class SqsMQCdiProducer {

    /**
     * 无参构造函数，供 CDI 容器实例化本生产者 Bean。
     */
    public SqsMQCdiProducer() {
    }

    /**
     * 产出 {@link SqsProperties} 默认 Bean。
     *
     * @return {@link SqsProperties} 实例
     */
    @Produces
    @Singleton
    @DefaultBean
    public SqsProperties sqsProperties() {
        return new SqsProperties();
    }

    /**
     * 依据 {@code SqsProperties} 构建并产出 {@link SqsMQClient} 默认 Bean。
     *
     * @param properties MQ 客户端配置
     * @return {@link SqsMQClient} 实例
     */
    @Produces
    @Singleton
    @DefaultBean
    public SqsMQClient sqsMQClient(SqsProperties properties) {
        return new SqsMQClient(properties);
    }

}
