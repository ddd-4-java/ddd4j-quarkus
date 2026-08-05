package io.ddd4j.quarkus.sample.client.order;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OrderClientDTO} 单元测试。
 *
 * <p>纯 JUnit 5：验证对外 API 契约 record 的构造、字段访问与值语义
 * （不可变、equals/hashCode、Serializable）。</p>
 */
class OrderClientDTOTest {

    @Test
    void shouldConstructAndExposeFields() {
        LocalDateTime createdTime = LocalDateTime.of(2026, 8, 4, 10, 0);
        OrderClientDTO dto = new OrderClientDTO(
                1L, "ORD-CLIENT-001", "buyer-3001", "王五", "CREATED",
                new BigDecimal("199.00"), createdTime);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.orderNo()).isEqualTo("ORD-CLIENT-001");
        assertThat(dto.buyerId()).isEqualTo("buyer-3001");
        assertThat(dto.buyerName()).isEqualTo("王五");
        assertThat(dto.status()).isEqualTo("CREATED");
        assertThat(dto.totalAmount()).isEqualByComparingTo("199.00");
        assertThat(dto.createdTime()).isEqualTo(createdTime);
    }

    @Test
    void shouldAllowNullStatusAndAmount() {
        OrderClientDTO dto = new OrderClientDTO(null, "ORD-CLIENT-002", "buyer-3002", null, null, null, null);

        assertThat(dto.id()).isNull();
        assertThat(dto.status()).isNull();
        assertThat(dto.totalAmount()).isNull();
    }

    @Test
    void recordShouldImplementValueSemantics() {
        OrderClientDTO a = new OrderClientDTO(1L, "ORD-1", "buyer-1", "张三", "PAID", BigDecimal.TEN, null);
        OrderClientDTO b = new OrderClientDTO(1L, "ORD-1", "buyer-1", "张三", "PAID", BigDecimal.TEN, null);
        OrderClientDTO c = new OrderClientDTO(2L, "ORD-2", "buyer-2", "李四", "CREATED", BigDecimal.ONE, null);

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(c).isNotEqualTo(null);
        assertThat(c).isNotEqualTo("not-a-dto");
        assertThat(a.toString()).contains("ORD-1").contains("PAID");
    }

    @Test
    void shouldImplementSerializable() {
        assertThat(Serializable.class.isAssignableFrom(OrderClientDTO.class)).isTrue();
    }

    @Test
    void createOrderRequestShouldExposeFields() {
        OrderClientService.CreateOrderRequest request =
                new OrderClientService.CreateOrderRequest("ORD-CLIENT-003", "buyer-3003", "赵六");

        assertThat(request.orderNo()).isEqualTo("ORD-CLIENT-003");
        assertThat(request.buyerId()).isEqualTo("buyer-3003");
        assertThat(request.buyerName()).isEqualTo("赵六");
    }
}
