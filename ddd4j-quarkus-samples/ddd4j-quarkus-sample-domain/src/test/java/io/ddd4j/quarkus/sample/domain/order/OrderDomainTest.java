package io.ddd4j.quarkus.sample.domain.order;

import io.ddd4j.core.ddd.event.DomainEvent;
import io.ddd4j.quarkus.sample.domain.order.event.OrderCancelledEvent;
import io.ddd4j.quarkus.sample.domain.order.event.OrderCreatedEvent;
import io.ddd4j.quarkus.sample.domain.order.event.OrderPaidEvent;
import io.ddd4j.quarkus.sample.domain.order.event.OrderShippedEvent;
import io.ddd4j.quarkus.sample.domain.order.model.OrderStatus;
import io.ddd4j.quarkus.sample.domain.order.model.aggregate.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link Order} 聚合根（充血模型）单元测试。
 *
 * <p>纯 JUnit 5 测试，不启动 Quarkus：覆盖创建初始状态、状态机流转
 * （CREATED → PAID → SHIPPED）、非法流转与领域事件注册。</p>
 */
class OrderDomainTest {

    private static Order createdOrder() {
        return Order.create("ORD-20260804-001", "buyer-1001", "张三");
    }

    @Test
    void createShouldInitCreatedStatusAndZeroAmount() {
        Order order = createdOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getTotalAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(order.getOrderNo()).isEqualTo("ORD-20260804-001");
        assertThat(order.getBuyerId()).isEqualTo("buyer-1001");
        assertThat(order.getBuyerName()).isEqualTo("张三");
        assertThat(order.getCreatedTime()).isNotNull();
        // 持久化前 ID 为 null，由仓储 save() 回填
        assertThat(order.getId()).isNull();
    }

    @Test
    void createShouldRejectBlankArguments() {
        assertThatThrownBy(() -> Order.create("", "buyer-1", "张三"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Order.create("ORD-1", null, "张三"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Order.create("ORD-1", "buyer-1", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createShouldRegisterCreatedEvent() {
        Order order = createdOrder();

        List<DomainEvent<?>> events = order.domainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(OrderCreatedEvent.class);

        OrderCreatedEvent event = (OrderCreatedEvent) events.get(0);
        // 订单未持久化，事件中 orderId 为 null（由基础设施层持久化后重建）
        assertThat(event.getOrderId()).isNull();
        assertThat(event.getOrderNo()).isEqualTo("ORD-20260804-001");
        assertThat(event.getBuyerId()).isEqualTo("buyer-1001");
        assertThat(event.getTotalAmount()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void payShouldTransitionToPaidAndRegisterEvent() {
        Order order = createdOrder();

        order.pay();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        // domainEvents() 返回不可变视图（含创建事件 + 支付事件）
        assertThat(order.domainEvents())
                .anyMatch(event -> event instanceof OrderPaidEvent);
        // pullDomainEvents() 返回并清空未提交事件
        assertThat(order.pullDomainEvents())
                .anyMatch(event -> event instanceof OrderPaidEvent);
        assertThat(order.hasDomainEvents()).isFalse();
    }

    @Test
    void payShouldRejectNonCreatedOrder() {
        Order order = createdOrder();
        order.pay();

        assertThatThrownBy(order::pay)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only created order can be paid");
    }

    @Test
    void shipShouldRejectUnpaidOrder() {
        // 未支付（CREATED）直接发货 → 非法流转
        Order order = createdOrder();

        assertThatThrownBy(order::ship)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only paid order can be shipped");
    }

    @Test
    void shipAfterPayShouldTransitionToShipped() {
        Order order = createdOrder();
        order.pay();

        order.ship();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.domainEvents())
                .anyMatch(event -> event instanceof OrderShippedEvent);
    }

    @Test
    void cancelShouldTransitionCreatedToCancelled() {
        Order order = createdOrder();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.domainEvents())
                .anyMatch(event -> event instanceof OrderCancelledEvent);
    }

    @Test
    void cancelShouldRejectShippedOrder() {
        Order order = createdOrder();
        order.pay();
        order.ship();

        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("shipped order cannot be cancelled");
    }

    @Test
    void cancelShouldRejectCompletedOrder() {
        Order order = createdOrder();
        // 聚合根无 complete() 方法，直接 setStatus 构造终态（测试取消校验）
        order.setStatus(OrderStatus.COMPLETED);

        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("completed order cannot be cancelled");
    }

    @Test
    void cancelShouldRejectAlreadyCancelledOrder() {
        Order order = createdOrder();
        order.cancel();

        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("order already cancelled");
    }

    @Test
    void statusMachineShouldDefineLegalTransitions() {
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.PAID)).isTrue();
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.SHIPPED)).isTrue();
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        // 非法流转
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.PAID)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CREATED)).isFalse();
    }
}
