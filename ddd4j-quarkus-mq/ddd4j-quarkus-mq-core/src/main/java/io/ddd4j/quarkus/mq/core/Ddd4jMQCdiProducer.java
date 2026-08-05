package io.ddd4j.quarkus.mq.core;

import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * ddd4j MQ 核心 CDI 生产者：为 Quarkus 容器提供 mq-core 契约的默认 Bean。
 *
 * <p>对标 ddd4j-mq-spring 的 {@code Ddd4jMQPropertiesConfiguration}，产出：
 * <ul>
 *   <li>{@link MQProperties} —— 从 MicroProfile Config（{@code ddd4j.mq.*}）构建配置对象</li>
 *   <li>{@link JsonMQEventSerialization} / {@link MQEventSerialization} —— JSON 序列化默认实现</li>
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
     * <p>读取的关键配置项（与 ddd4j-mq-core 的 {@link MQProperties} 字段对齐）：
     * <ul>
     *   <li>{@code ddd4j.mq.enabled} —— 是否启用 MQ（默认 false）</li>
     *   <li>{@code ddd4j.mq.namespace} —— 默认命名空间</li>
     *   <li>{@code ddd4j.mq.broker} —— 当前 Broker 实现标识（如 kafka/rabbit/rocket/redisStream）</li>
     *   <li>{@code ddd4j.mq.default-topic} —— 默认主题</li>
     *   <li>{@code ddd4j.mq.server} —— 服务地址</li>
     *   <li>{@code ddd4j.mq.auto-ack} —— 是否自动确认</li>
     * </ul>
     *
     * @return MQ 配置对象
     */
    @Produces
    @Singleton
    public MQProperties mqProperties() {
        Config config = ConfigProvider.getConfig();
        MQProperties props = new MQProperties();
        props.setEnabled(config.getOptionalValue("ddd4j.mq.enabled", Boolean.class).orElse(false));
        props.setBroker(config.getOptionalValue("ddd4j.mq.broker", String.class).orElse("none"));
        props.setNamespace(config.getOptionalValue("ddd4j.mq.namespace", String.class).orElse(""));
        props.setServer(config.getOptionalValue("ddd4j.mq.server", String.class).orElse(""));
        props.setAutoAck(config.getOptionalValue("ddd4j.mq.auto-ack", Boolean.class).orElse(false));
        return props;
    }

    /**
     * 默认 JSON 消息序列化实现（同时作为 {@link MQEventSerialization}）。
     *
     * <p>返回类型 {@link JsonMQEventSerialization} 实现 {@link MQEventSerialization} 接口，
     * CDI 类型安全解析会同时满足 {@code @Inject MQEventSerialization} 与
     * {@code @Inject JsonMQEventSerialization}，无需再显式暴露接口绑定
     * （否则会产生两个候选 Bean，触发 Ambiguous dependencies 部署错误）。</p>
     *
     * @return JSON 序列化器
     */
    @Produces
    @Singleton
    public JsonMQEventSerialization jsonMQEventSerialization() {
        return new JsonMQEventSerialization();
    }
}