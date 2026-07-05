package io.ddd4j.sample.quarkus.mq.disruptor.mq.config;

import io.ddd4j.mq.disruptor.config.DisruptorMQProperties;
import io.ddd4j.mq.disruptor.core.DisruptorMQBus;
import io.ddd4j.mq.disruptor.core.DisruptorMQEventDispatcher;
import io.ddd4j.mq.disruptor.publisher.DisruptorMQEventPublisher;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus CDI 配置：为 Disruptor 本地 MQ 提供 CDI Bean。
 *
 * <p>Quarkus CDI 容器自动发现 {@code @Produces} 方法，
 * 将 Disruptor 组件注册为应用级 Bean。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class DisruptorMqConfig {

    /**
     * 提供 Disruptor 配置属性。
     *
     * <p>可通过 application.properties 覆盖：
     * <ul>
     *   <li>{@code ddd4j.mq.disruptor.buffer-size=2048}</li>
     *   <li>{@code ddd4j.mq.disruptor.wait-strategy=yielding}</li>
     * </ul>
     */
    @Produces
    @DefaultBean
    @Singleton
    public DisruptorMQProperties disruptorMQProperties() {
        DisruptorMQProperties props = new DisruptorMQProperties();
        props.setBufferSize(1024);
        props.setWaitStrategy("yielding");
        props.setNamespace("quarkus-disruptor-sample");
        props.setDefaultTopic("DEFAULT");
        return props;
    }

    /**
     * 提供 Disruptor 事件分发器。
     */
    @Produces
    @Singleton
    public DisruptorMQEventDispatcher disruptorMQEventDispatcher() {
        return new DisruptorMQEventDispatcher();
    }

    /**
     * 提供 Disruptor MQ 总线（RingBuffer 生命周期管理）。
     */
    @Produces
    @Singleton
    public DisruptorMQBus disruptorMQBus(DisruptorMQProperties properties,
                                          DisruptorMQEventDispatcher dispatcher) {
        return new DisruptorMQBus(properties, dispatcher);
    }

    /**
     * 提供 Disruptor 事件发布者（注入 MQEventPublisher 接口）。
     */
    @Produces
    @Singleton
    public DisruptorMQEventPublisher disruptorMQEventPublisher(DisruptorMQBus bus,
                                                                DisruptorMQProperties properties) {
        return new DisruptorMQEventPublisher(bus, properties);
    }
}
