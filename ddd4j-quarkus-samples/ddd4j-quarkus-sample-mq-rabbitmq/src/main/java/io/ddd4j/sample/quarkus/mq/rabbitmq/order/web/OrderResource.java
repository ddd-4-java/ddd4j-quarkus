package io.ddd4j.sample.quarkus.mq.rabbitmq.order.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

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
 * <h2>端点</h2>
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

    /**
     * 构造 OrderResource 实例。
     * @param applicationService  应用服务
     */
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
     * 组件 orderNo：订单编号（必填）
     * 组件 buyerId：买家 ID
     * 组件 buyerName：买家名称
     */
    public final static class CreateOrderRequest {

        private static final long serialVersionUID = 0L;

        private final String orderNo;

        private final String buyerId;

        private final String buyerName;

        @JsonCreator()
        public CreateOrderRequest(@JsonProperty("orderNo") String orderNo, @JsonProperty("buyerId") String buyerId, @JsonProperty("buyerName") String buyerName) {
            this.orderNo = orderNo;
            this.buyerId = buyerId;
            this.buyerName = buyerName;
        }

        @JsonProperty("orderNo")
        public String orderNo() {
            return orderNo;
        }

        @JsonProperty("buyerId")
        public String buyerId() {
            return buyerId;
        }

        @JsonProperty("buyerName")
        public String buyerName() {
            return buyerName;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (Objects.isNull(obj) || getClass() != obj.getClass()) {
                return false;
            }
            CreateOrderRequest other = (CreateOrderRequest) obj;
            return Objects.equals(this.orderNo, other.orderNo) && Objects.equals(this.buyerId, other.buyerId) && Objects.equals(this.buyerName, other.buyerName);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(orderNo);
            result = 31 * result + Objects.hashCode(buyerId);
            result = 31 * result + Objects.hashCode(buyerName);
            return result;
        }

        @Override
        public String toString() {
            return "CreateOrderRequest[orderNo=" + orderNo + ", buyerId=" + buyerId + ", buyerName=" + buyerName + "]";
        }
    }

    /**
     * 订单响应 DTO（与 {@link Order} 解耦，避免外部依赖聚合根结构）。
     *
     * 组件 id：订单 ID
     * 组件 orderNo：订单编号
     * 组件 buyerId：买家 ID
     * 组件 buyerName：买家名称
     * 组件 status：订单状态（{@link OrderStatus} 名称）
     * 组件 totalAmount：订单总金额
     */
    public final static class OrderResponse {

        private static final long serialVersionUID = 0L;

        private final String id;

        private final String orderNo;

        private final String buyerId;

        private final String buyerName;

        private final String status;

        private final BigDecimal totalAmount;

        @JsonCreator()
        public OrderResponse(@JsonProperty("id") String id, @JsonProperty("orderNo") String orderNo, @JsonProperty("buyerId") String buyerId, @JsonProperty("buyerName") String buyerName, @JsonProperty("status") String status, @JsonProperty("totalAmount") BigDecimal totalAmount) {
            this.id = id;
            this.orderNo = orderNo;
            this.buyerId = buyerId;
            this.buyerName = buyerName;
            this.status = status;
            this.totalAmount = totalAmount;
        }

        @JsonProperty("id")
        public String id() {
            return id;
        }

        @JsonProperty("orderNo")
        public String orderNo() {
            return orderNo;
        }

        @JsonProperty("buyerId")
        public String buyerId() {
            return buyerId;
        }

        @JsonProperty("buyerName")
        public String buyerName() {
            return buyerName;
        }

        @JsonProperty("status")
        public String status() {
            return status;
        }

        @JsonProperty("totalAmount")
        public BigDecimal totalAmount() {
            return totalAmount;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (Objects.isNull(obj) || getClass() != obj.getClass()) {
                return false;
            }
            OrderResponse other = (OrderResponse) obj;
            return Objects.equals(this.id, other.id) && Objects.equals(this.orderNo, other.orderNo) && Objects.equals(this.buyerId, other.buyerId) && Objects.equals(this.buyerName, other.buyerName) && Objects.equals(this.status, other.status) && Objects.equals(this.totalAmount, other.totalAmount);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(id);
            result = 31 * result + Objects.hashCode(orderNo);
            result = 31 * result + Objects.hashCode(buyerId);
            result = 31 * result + Objects.hashCode(buyerName);
            result = 31 * result + Objects.hashCode(status);
            result = 31 * result + Objects.hashCode(totalAmount);
            return result;
        }

        @Override
        public String toString() {
            return "OrderResponse[id=" + id + ", orderNo=" + orderNo + ", buyerId=" + buyerId + ", buyerName=" + buyerName + ", status=" + status + ", totalAmount=" + totalAmount + "]";
        }
    }

    /**
     * 错误响应 DTO。
     */
    public final static class ErrorResponse {

        private static final long serialVersionUID = 0L;

        private final String message;

        @JsonCreator()
        public ErrorResponse(@JsonProperty("message") String message) {
            this.message = message;
        }

        @JsonProperty("message")
        public String message() {
            return message;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (Objects.isNull(obj) || getClass() != obj.getClass()) {
                return false;
            }
            ErrorResponse other = (ErrorResponse) obj;
            return Objects.equals(this.message, other.message);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(message);
            return result;
        }

        @Override
        public String toString() {
            return "ErrorResponse[message=" + message + "]";
        }
    }

    /**
     * 仅供文档/测试引用：MQ Topic 与 Tag 常量。
     */
    @SuppressWarnings("unused")
    public static final class MqConstants {
        /**
         * 主题。
         */
        public static final String TOPIC = OrderCreatedEvent.TOPIC;
        /**
         * 标签。
         */
        public static final String TAG = OrderCreatedEvent.TAG;
    }
}
