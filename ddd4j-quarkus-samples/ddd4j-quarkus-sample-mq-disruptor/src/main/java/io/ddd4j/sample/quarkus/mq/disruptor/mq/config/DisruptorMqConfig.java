package io.ddd4j.sample.quarkus.mq.disruptor.mq.config;

import io.ddd4j.mq.disruptor.DisruptorMQProperties;
import io.ddd4j.mq.disruptor.util.WaitStrategys;
import jakarta.enterprise.inject.Typed;
import io.ddd4j.mq.disruptor.DisruptorMQClient;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Quarkus CDI 配置：为 Disruptor 本地 MQ 提供自定义配置覆盖。
 *
 * <p>DisruptorMQClient 由 ddd4j-quarkus-mq-disruptor 的 CdiProducer 自动暴露。
 * 本配置仅覆盖默认属性（buffer-size / wait-strategy 等）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationScoped
public class DisruptorMqConfig {

    /** 显式注入并在应用退出时释放 broker 客户端，由框架注册器负责启动。 */
    void shutdown(@Observes ShutdownEvent event,
                  DisruptorMQClient client) throws Exception {
        client.close();
    }

    @Produces
    @Typed(DisruptorMQProperties.class)
    @Singleton
    public DisruptorMQProperties disruptorMQProperties() {
        DisruptorMQProperties props = new DisruptorMQProperties();
        props.setEnabled(true);
        props.setBroker("disruptor");
        props.setNamespace("quarkus-disruptor-sample");
        props.setBufferSize(1024);
        props.setWaitStrategy(WaitStrategys.yielding);
        return props;
    }
}
