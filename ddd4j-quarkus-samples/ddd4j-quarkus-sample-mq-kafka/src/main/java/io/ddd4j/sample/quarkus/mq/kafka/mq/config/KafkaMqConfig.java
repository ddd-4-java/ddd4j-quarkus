package io.ddd4j.sample.quarkus.mq.kafka.mq.config;

import io.ddd4j.mq.kafka.KafkaMQProperties;
import io.ddd4j.mq.kafka.KafkaMQClient;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.Config;

/** 将示例的 Kafka 连接配置绑定到实际客户端，支持测试容器动态端口。 */
@ApplicationScoped
public class KafkaMqConfig {

    /** 显式注入并在应用退出时释放 broker 客户端，由框架注册器负责启动。 */
    void shutdown(@Observes ShutdownEvent event,
                  KafkaMQClient client) throws Exception {
        client.close();
    }
    /** 创建 broker 专用属性；MQ 通用属性继续由框架提供。 */
    @Produces
    @Singleton
    @Typed(KafkaMQProperties.class)
    public KafkaMQProperties kafkaMQProperties(Config config) {
        KafkaMQProperties properties = new KafkaMQProperties();
        properties.setBootstrapServers(config.getValue("ddd4j.mq.kafka.bootstrap-servers", String.class));
        properties.setGroupIdPrefix(config.getValue("ddd4j.mq.kafka.consumer-group", String.class));
        return properties;
    }
}
