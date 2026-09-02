package io.ddd4j.quarkus.mq.sqs;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.sqs.SqsProperties;
import io.ddd4j.quarkus.mq.testcontainers.AbstractMqQuarkusIntegrationTest;
import io.ddd4j.quarkus.mq.testcontainers.JunitJupiterQuarkusTestContainers;
import io.ddd4j.quarkus.mq.testcontainers.SqsQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * sqs MQ 集成测试。
 *
 * <p>验证：
 * <ul>
 *   <li>{@link MQClient} Bean 被 CDI 正确解析，且 impl() = "sqs"</li>
 *   <li>{@link MQProperties} Bean 存在且 broker = "SQS"</li>
 *   <li>{@link MQEventSerialization} Bean 存在且可注入</li>
 *   <li>端到端：{@code OrderCreatedEvent.publish()} → LocalStack SQS →
 *       {@code @MQEventListener} 监听器收到事件（继承 {@link AbstractMqQuarkusIntegrationTest}
 *       round-trip 骨架）</li>
 * </ul>
 *
 * <p><b>SQS 差异</b>：SQS 没有 topic/tag 概念，{@code MQListener.topic} 与事件 topic
 * 都必须是 queue URL——{@link #adaptListener} 先在 LocalStack 建队列并改写监听器 topic，
 * {@link #newOrderCreatedEvent} 用同一 queue URL 发布（对齐 javalin Ddd4jSqsMqIT 先例）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@QuarkusTest
@QuarkusTestResource(SqsQuarkusIntegrationTest.SqsTestResource.class)
@JunitJupiterQuarkusTestContainers
class SqsQuarkusIntegrationTest extends AbstractMqQuarkusIntegrationTest<SqsProperties> {

    @Inject
    SqsProperties sqsProperties;

    /** adaptListener 阶段建好的 LocalStack 队列 URL，发布侧复用。 */
    private volatile String queueUrl;

    @Override
    protected SqsProperties mqPropertiesExtension() {
        return sqsProperties;
    }

    @Override
    protected void applyContainerProperties(SqsProperties properties) {
        properties.setRegion("us-east-1");
        properties.setAccessKey("test");
        properties.setSecretKey("test");
        properties.setEndpointOverride(config("ddd4j.mq.sqs.endpoint"));
        // 加快 IT 轮询节奏
        properties.setWaitTimeSeconds(1);
        properties.setPollIntervalMs(200);
    }

    @Override
    protected void adaptListener(MQListener listener) {
        try (SqsClient sqs = SqsClient.builder()
                .endpointOverride(URI.create(sqsProperties.getEndpointOverride()))
                .region(Region.of(sqsProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(sqsProperties.getAccessKey(), sqsProperties.getSecretKey())))
                .build()) {
            // createQueue 幂等：同名队列已存在时返回既有 URL
            this.queueUrl = sqs.createQueue(CreateQueueRequest.builder()
                    .queueName(TOPIC).build()).queueUrl();
        }
        listener.setTopic(queueUrl);
    }

    @Override
    protected OrderCreatedEvent newOrderCreatedEvent() {
        // SQS producer 以 event.topic 作为 queueUrl 发送
        OrderCreatedEvent event = super.newOrderCreatedEvent();
        event.setTopic(queueUrl);
        return event;
    }

    @Test
    void shouldInjectMQClient() {
        Assertions.assertThat(mqClient).isNotNull();
        Assertions.assertThat(mqClient.impl()).isEqualTo("sqs");
    }

    @Test
    void shouldInjectMQProperties() {
        Assertions.assertThat(mqProperties).isNotNull();
        Assertions.assertThat(mqProperties.isEnabled()).isTrue();
        Assertions.assertThat(mqProperties.getBroker()).isEqualTo("SQS");
    }

    @Test
    void shouldInjectSerialization() {
        Assertions.assertThat(serialization).isNotNull();
        // 验证序列化 round-trip
        String json = serialization.serialize(Map.of("key", "value"));
        Assertions.assertThat(json).contains("key");
    }

    /**
     * 端到端：OrderCreatedEvent 发布 → LocalStack SQS（queueUrl 直发）→ 监听器消费。
     */
    @Test
    void shouldPublishAndConsumeOrderCreatedEventEndToEnd() throws Exception {
        runOrderCreatedRoundTrip();
    }

    /**
     * sqs testcontainers resource for Quarkus：委托共享 fixture {@link SqsQuarkusTestResource}。
     */
    public static class SqsTestResource implements QuarkusTestResourceLifecycleManager {

        private final SqsQuarkusTestResource fixture = new SqsQuarkusTestResource();

        @Override
        public Map<String, String> start() {
            Map<String, String> props = new HashMap<>(fixture.start());
            props.put("ddd4j.mq.enabled", "true");
            return props;
        }

        @Override
        public void stop() {
            fixture.stop();
        }
    }
}
