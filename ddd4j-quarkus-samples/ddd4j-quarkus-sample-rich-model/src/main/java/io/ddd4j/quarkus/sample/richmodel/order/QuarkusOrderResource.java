package io.ddd4j.quarkus.sample.richmodel.order;

import io.ddd4j.sample.richmodel.order.application.AddOrderLineCommand;
import io.ddd4j.sample.richmodel.order.application.CreateOrderCommand;
import io.ddd4j.sample.richmodel.order.application.OrderApplicationService;
import io.ddd4j.sample.richmodel.order.domain.model.Order;
import io.ddd4j.sample.richmodel.order.domain.repository.OrderRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * JAX-RS adapter for the rich-model sample.
 */
@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class QuarkusOrderResource {

    private final OrderApplicationService applicationService;
    private final OrderRepository repository;

    @Inject
    public QuarkusOrderResource(OrderApplicationService applicationService, OrderRepository repository) {
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @POST
    public OrderResponse create(CreateOrderRequest request) {
        Order order = applicationService.createDraft(new CreateOrderCommand(
                request.orderNo(),
                request.buyerId(),
                request.buyerName()
        ));
        return OrderResponse.from(order);
    }

    @POST
    @Path("/{orderId}/lines")
    public OrderResponse addLine(@PathParam("orderId") String orderId, AddLineRequest request) {
        Order order = applicationService.addLine(new AddOrderLineCommand(
                orderId,
                request.productId(),
                request.productName(),
                request.quantity(),
                request.unitPrice()
        ));
        return OrderResponse.from(order);
    }

    @POST
    @Path("/{orderId}/pay")
    public OrderResponse pay(@PathParam("orderId") String orderId) {
        return OrderResponse.from(applicationService.pay(orderId));
    }

    @GET
    @Path("/by-no/{orderNo}")
    public OrderResponse findByOrderNo(@PathParam("orderNo") String orderNo) {
        return repository.findByOrderNo(orderNo)
                .map(OrderResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderNo));
    }

    public record CreateOrderRequest(String orderNo, String buyerId, String buyerName) {
    }

    public record AddLineRequest(String productId, String productName, int quantity, BigDecimal unitPrice) {
    }

    public record OrderResponse(
            String id,
            String orderNo,
            String buyerId,
            String buyerName,
            String status,
            BigDecimal totalAmount,
            List<OrderLineResponse> lines
    ) {

        static OrderResponse from(Order order) {
            return new OrderResponse(
                    order.id(),
                    order.orderNo(),
                    order.buyerId(),
                    order.buyerName(),
                    order.status().name(),
                    order.totalAmount().amount(),
                    order.lines().stream().map(OrderLineResponse::from).toList()
            );
        }
    }

    public record OrderLineResponse(String id, String productId, String productName, int quantity, BigDecimal subtotal) {

        static OrderLineResponse from(io.ddd4j.sample.richmodel.order.domain.model.OrderLine line) {
            return new OrderLineResponse(
                    line.id(),
                    line.productId(),
                    line.productName(),
                    line.quantity(),
                    line.subtotal().amount()
            );
        }
    }
}
