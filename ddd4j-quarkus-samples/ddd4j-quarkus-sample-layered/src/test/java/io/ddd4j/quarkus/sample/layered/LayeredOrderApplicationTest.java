package io.ddd4j.quarkus.sample.layered;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 分层架构示例全链路 Quarkus 集成测试（对齐 ddd4j-boot 的 DemoApplication_Test 冒烟模式）。
 *
 * <p>通过 {@code @QuarkusTest} 启动分层示例应用：{@code LayeredSampleConfig}
 * 装配内存版 {@code OrderRepository}（开箱即跑，无需数据库），
 * 使用 RestAssured 走 {@code /api/orders} 全链路：
 * 创建（CREATED）→ 查询 → 支付（PAID）→ 分页。</p>
 */
@QuarkusTest
class LayeredOrderApplicationTest {

    @Test
    void shouldRunOrderFullFlow() {
        // 1. 创建订单 → CREATED
        Long id = given()
                .contentType("application/json")
                .body("{\"orderNo\":\"ORD-LAYERED-001\",\"buyerId\":\"buyer-layered-1\",\"buyerName\":\"张三\"}")
                .when().post("/api/orders")
                .then().statusCode(200)
                .body("data.orderNo", equalTo("ORD-LAYERED-001"))
                .body("data.status", equalTo("CREATED"))
                .extract().jsonPath().getLong("data.id");

        // 2. 按 ID 查询 → 仍为 CREATED
        given()
                .when().get("/api/orders/" + id)
                .then().statusCode(200)
                .body("data.id", equalTo(id.intValue()))
                .body("data.orderNo", equalTo("ORD-LAYERED-001"))
                .body("data.status", equalTo("CREATED"));

        // 3. 支付 → PAID
        given()
                .when().post("/api/orders/" + id + "/pay")
                .then().statusCode(200)
                .body("data.id", equalTo(id.intValue()))
                .body("data.status", equalTo("PAID"));

        // 4. 分页查询 → 至少包含刚创建的订单
        given()
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when().get("/api/orders")
                .then().statusCode(200)
                .body("data.records", notNullValue())
                .body("data.total", greaterThanOrEqualTo(1));
    }

    /**
     * 完整状态机贯通（含正向主链路 CREATED → PAID → SHIPPED + 分支 CREATED → CANCELLED）：
     * <p>本测试验证两条独立业务流：
     * <ul>
     *   <li>正向流：CREATED → PAID → SHIPPED（终态）</li>
     *   <li>分支流：另一独立订单 CREATED → CANCELLED</li>
     * </ul>
     * <p>说明：领域层 {@code Order.cancel} 仅允许 CREATED/PAID → CANCELLED，
     * SHIPPED → CANCELLED 抛出 IllegalStateException（业务约束：已发货不可取消），
     * 因此 COMPLETED/CANCELLED 终止 SHIPPED 链。
     */
    @Test
    void shouldRunCompleteOrderLifecycleAcrossAllStates() {
        // 主链路：CREATED → PAID → SHIPPED
        Long id = given()
                .contentType("application/json")
                .body("{\"orderNo\":\"ORD-LIFECYCLE-001\",\"buyerId\":\"buyer-lc-1\",\"buyerName\":\"李四\"}")
                .when().post("/api/orders")
                .then().statusCode(200)
                .body("data.status", equalTo("CREATED"))
                .extract().jsonPath().getLong("data.id");

        given()
                .when().post("/api/orders/" + id + "/pay")
                .then().statusCode(200)
                .body("data.status", equalTo("PAID"));

        given()
                .when().post("/api/orders/" + id + "/ship")
                .then().statusCode(200)
                .body("data.status", equalTo("SHIPPED"));

        given()
                .when().get("/api/orders/" + id)
                .then().statusCode(200)
                .body("data.status", equalTo("SHIPPED"));

        // 分支链路：独立订单 CREATED → CANCELLED（验证 cancel 业务约束）
        Long id2 = given()
                .contentType("application/json")
                .body("{\"orderNo\":\"ORD-LIFECYCLE-002\",\"buyerId\":\"buyer-lc-2\",\"buyerName\":\"王五\"}")
                .when().post("/api/orders")
                .then().statusCode(200)
                .extract().jsonPath().getLong("data.id");

        given()
                .when().post("/api/orders/" + id2 + "/cancel")
                .then().statusCode(200)
                .body("data.status", equalTo("CANCELLED"));

        // 业务约束：SHIPPED 订单不可取消（领域层 Order.cancel 抛 IllegalStateException → 409）
        given()
                .when().post("/api/orders/" + id + "/cancel")
                .then().statusCode(409);
    }
}
