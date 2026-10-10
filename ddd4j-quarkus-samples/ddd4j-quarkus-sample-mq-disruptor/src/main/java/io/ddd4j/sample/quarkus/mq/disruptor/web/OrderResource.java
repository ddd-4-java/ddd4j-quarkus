package io.ddd4j.sample.quarkus.mq.disruptor.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

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
 * <h2>端点</h2>
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
}
