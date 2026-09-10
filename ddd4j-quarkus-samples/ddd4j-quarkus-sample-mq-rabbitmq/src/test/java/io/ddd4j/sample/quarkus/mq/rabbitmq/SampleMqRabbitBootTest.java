package io.ddd4j.sample.quarkus.mq.rabbitmq;

import io.ddd4j.sample.quarkus.mq.rabbitmq.mq.ConsumedOrderProjection;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.ddd4j.quarkus.mq.testcontainers.RabbitMqQuarkusTestResource;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** 验证 HTTP 创建订单经真实 MQ 消费后产生相同订单的投影。 */
@QuarkusTest
@QuarkusTestResource(SampleMqRabbitBootTest.BrokerResource.class)
class SampleMqRabbitBootTest {
    /** 仅适配共享 fixture 的生命周期与 broker 大小写，不复制容器启动。 */
    public static class BrokerResource implements QuarkusTestResourceLifecycleManager {
        private final RabbitMqQuarkusTestResource fixture = new RabbitMqQuarkusTestResource();

        @Override
        public Map<String, String> start() {
            Map<String, String> config = new HashMap<>(fixture.start());
            config.put("ddd4j.mq.broker", "rabbit");
            return config;
        }

        @Override
        public void stop() {
            fixture.stop();
        }
    }

    @Inject
    ConsumedOrderProjection projection;

    @BeforeEach
    void resetProjection() {
        projection.clear();
    }

    @Test
    void httpOrderReachesConsumerWithSamePayload() {
        String orderNo = "MQ-" + UUID.randomUUID();
        var response = given().contentType("application/json")
                .body(Map.of("orderNo", orderNo, "buyerId", "buyer-17", "buyerName", "MQ 买家"))
                .when().post("/orders").then().statusCode(200).extract().jsonPath();
        String orderId = response.getString("id");
        assertThat(orderId).isNotBlank();
        assertThat(response.getString("orderNo")).isEqualTo(orderNo);
        assertThat(response.getString("buyerName")).isEqualTo("MQ 买家");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var consumed = projection.find(orderId);
            assertThat(consumed).as("listener must project the HTTP-created order").isPresent();
            var event = consumed.orElseThrow();
            assertThat(event.getOrderId()).isEqualTo(orderId);
            assertThat(event.getOrderNo()).isEqualTo(orderNo);
            assertThat(event.getBuyerName()).isEqualTo("MQ 买家");
            assertThat(event.getTopic()).isEqualTo("ORDER");
            assertThat(event.getTag()).isEqualTo("created");
        });
    }
}
