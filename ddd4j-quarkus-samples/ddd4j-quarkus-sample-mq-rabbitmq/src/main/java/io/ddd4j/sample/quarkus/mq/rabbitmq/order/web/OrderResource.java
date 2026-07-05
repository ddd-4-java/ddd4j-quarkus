package io.ddd4j.sample.quarkus.mq.rabbitmq.order.web;

import io.ddd4j.sample.quarkus.mq.rabbitmq.order.application.OrderApplicationService;
import io.ddd4j.sample.quarkus.mq.rabbitmq.order.domain.Order;
import io.ddd4j.sample.quarkus.mq.rabbitmq.order.domain.OrderStatus;
import io.ddd4j.sample.quarkus.mq.rabbitmq.order.domain.event.OrderCreatedEvent;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 订单 JAX-RS 资源：演示 ddd4j + Quarkus + RabbitMQ MQ 集成。
 *
 * <h3>端点</h3>
 * <ul>
 *   <li>{@code POST /orders} - 创建订单（触发 OrderCreatedEvent → RabbitMQ Topic Exchange → @MQEventListener）</li>
 * </ul>
 *
 * <p>本类是 Web 入口层（adapter / 适配层），仅负责：
 * <ol>
 *   <li>接收 HTTP 请求并校验输入</li>
 *   <li>调用 {@link OrderApplicationService} 应用服务完成用例编排</li>
 *   <li>将聚合根序列化为 JSON 返回</li>
 * </ol>
 * 不参与业务逻辑，业务逻辑全部下沉到 {@link Order} 聚合根（充血模型）与 {@code OrderApplicationService}。
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
     * <p>完整调用链：
     * <pre>
     *   POST /orders
     *     ↓ OrderResource.create
     *     ↓ OrderApplicationService.createOrder
     *     ├─→ Order.create(orderNo, buyerId, buyerName)         [充血聚合根]
     *     ├─→ InMemoryOrderRepository.save(order)              [仓储持久化]
     *     └─→ MQEventPublisher.publish(OrderCreatedEvent)       [投递到 MQ]
     *            ↓ ddd4j-mq publish(BOTH/MQ)
     *            ↓ RabbitMQEventPublisher.publish
     *            ↓ Topic Exchange + routing key = "ORDER.CREATED"
     *     ↓ @MQEventListener(topic="ORDER", tags="CREATED")
     *     ↓ OrderCreatedMqListener.onOrderCreated               [消费处理]
     * </pre>
     *
     * @param request 创建订单请求体
     * @return HTTP 200 + Order JSON 响应
     */
    @POST
    public Response create(CreateOrderRequest request) {
        if (Objects.isNull(request) || Objects.isNull(request.orderNo())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("orderNo must not be null"))
                    .build();
        }
        Order order = applicationService.createOrder(
                request.orderNo(),
                request.buyerId(),
                request.buyerName());
        return Response.ok(toResponse(order)).build();
    }

    /**
     * 转换为对外响应 DTO（避免直接暴露聚合内部结构）。
     */
    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getBuyerId(),
                order.getBuyerName(),
                order.getStatus().name(),
                order.getTotalAmount());
    }

    /**
     * 创建订单请求 DTO。
     *
     * @param orderNo   订单编号（必填）
     * @param buyerId   买家 ID
     * @param buyerName 买家名称
     */
    public record CreateOrderRequest(String orderNo, String buyerId, String buyerName) {
    }

    /**
     * 订单响应 DTO（与 {@link Order} 解耦，避免外部依赖聚合根结构）。
     *
     * @param id          订单 ID
     * @param orderNo     订单编号
     * @param buyerId     买家 ID
     * @param buyerName   买家名称
     * @param status      订单状态（{@link OrderStatus} 名称）
     * @param totalAmount 订单总金额
     */
    public record OrderResponse(
            String id,
            String orderNo,
            String buyerId,
            String buyerName,
            String status,
            BigDecimal totalAmount) {
    }

    /**
     * 错误响应 DTO。
     */
    public record ErrorResponse(String message) {
    }

    /**
     * 仅供文档/测试引用：MQ Topic 与 Tag 常量。
     */
    @SuppressWarnings("unused")
    public static final class MqConstants {
        public static final String TOPIC = OrderCreatedEvent.TOPIC;
        public static final String TAG = OrderCreatedEvent.TAG;
    }
}