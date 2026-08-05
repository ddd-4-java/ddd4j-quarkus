package io.ddd4j.quarkus.sample.infrastructure.order.persistence.entity;

import io.ddd4j.quarkus.sample.domain.order.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link OrderEntity} 单元测试。
 *
 * <p>纯 JUnit 5（不启动 Quarkus / 不访问数据库）：验证持久化实体字段映射
 * （与领域聚合根字段一一对应）以及 {@link OrderStatus} 编码值与枚举的 round-trip。</p>
 */
class OrderEntityTest {

    @Test
    void shouldMapAllFields() {
        LocalDateTime createdTime = LocalDateTime.of(2026, 8, 4, 10, 30);
        OrderEntity entity = new OrderEntity();
        entity.id = 42L;
        entity.orderNo = "ORD-ENT-001";
        entity.buyerId = "buyer-2001";
        entity.buyerName = "李四";
        entity.status = OrderStatus.PAID.getCode();
        entity.totalAmount = new BigDecimal("199.00");
        entity.createdTime = createdTime;

        // 字段映射：聚合根 ↔ 实体一一对应
        assertThat(entity.id).isEqualTo(42L);
        assertThat(entity.orderNo).isEqualTo("ORD-ENT-001");
        assertThat(entity.buyerId).isEqualTo("buyer-2001");
        assertThat(entity.buyerName).isEqualTo("李四");
        assertThat(entity.status).isEqualTo("PAID");
        assertThat(entity.totalAmount).isEqualByComparingTo("199.00");
        assertThat(entity.createdTime).isEqualTo(createdTime);

        // 实体 status 以 OrderStatus.getCode() 编码值存储，可无损还原
        assertThat(OrderStatus.fromCode(entity.status)).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void statusCodeShouldRoundTripForAllValues() {
        for (OrderStatus status : OrderStatus.values()) {
            assertThat(OrderStatus.fromCode(status.getCode())).isEqualTo(status);
            assertThat(status.getDescription()).isNotBlank();
        }
    }

    @Test
    void fromCodeShouldRejectUnknownCode() {
        assertThatThrownBy(() -> OrderStatus.fromCode("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown order status code");
    }

    @Test
    void fromCodeShouldRejectNull() {
        assertThatThrownBy(() -> OrderStatus.fromCode(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void newEntityShouldHaveNullFields() {
        OrderEntity entity = new OrderEntity();

        assertThat(entity.id).isNull();
        assertThat(entity.orderNo).isNull();
        assertThat(entity.status).isNull();
        assertThat(entity.totalAmount).isNull();
        assertThat(entity.createdTime).isNull();
    }
}
