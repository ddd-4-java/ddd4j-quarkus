package io.ddd4j.sample.quarkus.mq.kafka.web;

import io.ddd4j.sample.quarkus.mq.kafka.order.application.OrderApplicationService;
import io.ddd4j.sample.quarkus.mq.kafka.order.domain.Order;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Objects;

/**
 * 订单 JAX-RS 资源：演示 ddd4j + Quarkus + Kafka MQ 集成。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    private final OrderApplicationService applicationService;

    /**
     * 构造 OrderResource 实例。
     * @param applicationService  应用服务
     */
    @Inject
    public OrderResource(OrderApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService must not be null");
    }

    /**
     * 创建订单（触发 OrderCreatedEvent → Kafka → @MQEventListener）。
     * @param request  请求
     * @return 创建
     */
    @POST
    public Response create(CreateOrderRequest request) {
        Order order = applicationService.createOrder(request.orderNo(), request.buyerId(), request.buyerName());
        return Response.ok(order).build();
    }

    /**
     * CreateOrderRequest 记录类。
     * @param buyerId  买家标识
     * @param buyerName  买家名称
     * @param orderNo  订单编号
     */
    public record CreateOrderRequest(String orderNo, String buyerId, String buyerName) {
    }
}
