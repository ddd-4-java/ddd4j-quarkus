package io.ddd4j.quarkus.sample.adapter.web;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * {@link OrderResource} Quarkus 集成测试（对齐 ddd4j-boot 的 DemoApplication_Test 冒烟模式）。
 *
 * <p>通过 {@code @QuarkusTest} 启动最小运行时：内存版 {@link OrderRepository}
 * 由 {@link TestOrderRepositoryProducer} 装配（测试 classpath），无需数据库；
 * 测试配置见 {@code src/test/resources/application.properties}。
 * 覆盖创建 → 查询 → 支付 → 分页 全链路，以及订单不存在的 400 映射。</p>
 */
@QuarkusTest
class OrderResourceQuarkusTest {

    private static final String ORDER_NO_PREFIX = "ORD-IT-";

    /**
     * 创建订单并返回回填的订单 ID。
     */
    private static Long createOrder(String orderNo) {
        return given()
                .contentType("application/json")
                .body("{\"orderNo\":\"" + orderNo + "\",\"buyerId\":\"buyer-it-1\",\"buyerName\":\"张三\"}")
                .when().post("/api/orders")
                .then().statusCode(200)
                .body("data.orderNo", equalTo(orderNo))
                .body("data.status", equalTo("CREATED"))
                .extract().jsonPath().getLong("data.id");
    }

    @Test
    void shouldCreateOrder() {
        given()
                .contentType("application/json")
                .body("{\"orderNo\":\"" + ORDER_NO_PREFIX + "CREATE-1\",\"buyerId\":\"buyer-it-1\",\"buyerName\":\"张三\"}")
                .when().post("/api/orders")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .body("data.orderNo", equalTo(ORDER_NO_PREFIX + "CREATE-1"))
                .body("data.buyerName", equalTo("张三"))
                .body("data.status", equalTo("CREATED"));
    }

    @Test
    void shouldGetOrderById() {
        Long id = createOrder(ORDER_NO_PREFIX + "GET-1");

        given()
                .when().get("/api/orders/" + id)
                .then().statusCode(200)
                .body("data.id", equalTo(id.intValue()))
                .body("data.orderNo", equalTo(ORDER_NO_PREFIX + "GET-1"))
                .body("data.status", equalTo("CREATED"));
    }

    @Test
    void shouldPayOrder() {
        Long id = createOrder(ORDER_NO_PREFIX + "PAY-1");

        given()
                .when().post("/api/orders/" + id + "/pay")
                .then().statusCode(200)
                .body("data.id", equalTo(id.intValue()))
                .body("data.status", equalTo("PAID"));
    }

    @Test
    void shouldPageOrders() {
        createOrder(ORDER_NO_PREFIX + "PAGE-1");
        createOrder(ORDER_NO_PREFIX + "PAGE-2");

        // 内存仓储为单例、跨测试方法共享，分页 total 为累积值，断言下限即可
        given()
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when().get("/api/orders")
                .then().statusCode(200)
                .body("data.records", notNullValue())
                .body("data.total", greaterThanOrEqualTo(2));
    }

    @Test
    void shouldMapMissingOrderToBadRequest() {
        given()
                .when().get("/api/orders/999999")
                .then().statusCode(400)
                .body("code", equalTo(400))
                .body("msg", notNullValue());
    }
}
