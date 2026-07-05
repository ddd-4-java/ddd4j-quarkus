package io.ddd4j.sample.quarkus.mq.disruptor.web;

import io.ddd4j.sample.quarkus.mq.disruptor.order.application.OrderApplicationService;
import io.ddd4j.sample.quarkus.mq.disruptor.order.domain.Order;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Objects;

/**
 * 订单 JAX-RS 资源：演示 ddd4j + Quarkus + Disruptor MQ 集成。
 *
 * <h3>端点</h3>
 * <ul>
 *   <li>{@code POST /orders} - 创建订单（触发 OrderCreatedEvent → Disruptor MQ → @MQEventListener）</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    private final OrderApplicationService applicationService;

    @Inject
    public OrderResource(OrderApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService must not be null");
    }

    /**
     * 创建订单。
     *
     * <p>创建后会自动发布 OrderCreatedEvent 到 Disruptor RingBuffer，
     * 由 @MQEventListener 消费。
     */
    @POST
    public Response create(CreateOrderRequest request) {
        Order order = applicationService.createOrder(request.orderNo(), request.buyerId(), request.buyerName());
        return Response.ok(order).build();
    }

    /**
     * 创建订单请求 DTO。
     */
    public record CreateOrderRequest(String orderNo, String buyerId, String buyerName) {
    }
}
