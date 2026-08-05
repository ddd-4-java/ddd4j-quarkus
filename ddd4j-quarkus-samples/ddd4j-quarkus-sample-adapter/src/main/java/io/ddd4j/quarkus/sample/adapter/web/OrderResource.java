package io.ddd4j.quarkus.sample.adapter.web;

import io.ddd4j.core.api.Page;
import io.ddd4j.quarkus.sample.app.order.command.CreateOrderCommand;
import io.ddd4j.quarkus.sample.app.order.dto.OrderDTO;
import io.ddd4j.quarkus.sample.app.order.query.OrderQuery;
import io.ddd4j.quarkus.sample.app.order.service.OrderApplicationService;
import io.ddd4j.quarkus.sample.client.order.OrderClientDTO;
import io.ddd4j.quarkus.sample.client.order.OrderClientService;
import io.ddd4j.quarkus.sample.domain.order.model.OrderStatus;
import io.ddd4j.web.quarkus.TenantAwareResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Objects;

/**
 * 订单 REST 资源（适配层 Web 入口）。
 *
 * <p>适配层（Adapter Layer）只负责协议转换，不承载业务逻辑：
 * <ol>
 *   <li>接收 HTTP 请求、解析路径/查询参数、反序列化请求体</li>
 *   <li>调用应用层 {@link OrderApplicationService} 完成用例编排</li>
 *   <li>将应用层 DTO 转换为客户端契约 {@link OrderClientDTO} 返回（R 统一包装）</li>
 * </ol>
 *
 * <p>本类同时实现客户端契约 {@link OrderClientService}：对外（前端/网关/其他服务）
 * 面向契约编程，不感知应用层/领域层实现。</p>
 *
 * <p>统一响应：所有端点通过基类 {@link TenantAwareResource} 的 {@code ok()} 返回
 * HTTP 200 + {@code R.ok(data)}；业务异常（{@link IllegalArgumentException}）
 * 由 {@link OrderExceptionMapper} 统一映射为 400。</p>
 *
 * <h3>端点</h3>
 * <ul>
 *   <li>{@code POST /api/orders} - 创建订单</li>
 *   <li>{@code GET /api/orders/{id}} - 查询订单</li>
 *   <li>{@code POST /api/orders/{id}/pay} - 支付订单</li>
 *   <li>{@code POST /api/orders/{id}/ship} - 发货</li>
 *   <li>{@code POST /api/orders/{id}/cancel} - 取消订单</li>
 *   <li>{@code GET /api/orders?page=&size=&orderNo=&buyerId=&status=} - 分页查询</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Tag(name = "订单管理", description = "订单相关的 REST API（分层架构示例）")
@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource extends TenantAwareResource implements OrderClientService {

    private final OrderApplicationService orderApplicationService;

    @Inject
    public OrderResource(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = Objects.requireNonNull(orderApplicationService, "orderApplicationService must not be null");
    }

    /**
     * 创建订单。
     *
     * @param command 创建订单命令（orderNo/buyerId/buyerName）
     * @return HTTP 200 + {@code R<OrderClientDTO>}
     */
    @POST
    @Operation(summary = "创建订单", description = "创建一个新的订单，初始状态为 CREATED")
    public Response create(CreateOrderCommand command) {
        return ok(toClientDTO(orderApplicationService.createOrder(command)));
    }

    /**
     * 按 ID 查询订单。
     *
     * @param id 订单 ID
     * @return HTTP 200 + {@code R<OrderClientDTO>}
     */
    @GET
    @Path("/{id}")
    @Operation(summary = "查询订单", description = "根据订单 ID 查询订单详情")
    public Response getById(@PathParam("id") Long id) {
        return ok(toClientDTO(orderApplicationService.getOrder(id)));
    }

    /**
     * 支付订单（CREATED → PAID）。
     *
     * @param id 订单 ID
     * @return HTTP 200 + {@code R<OrderClientDTO>}
     */
    @POST
    @Path("/{id}/pay")
    @Operation(summary = "支付订单", description = "对已创建的订单执行支付，状态流转 CREATED → PAID")
    public Response pay(@PathParam("id") Long id) {
        return ok(toClientDTO(orderApplicationService.payOrder(id)));
    }

    /**
     * 发货（PAID → SHIPPED）。
     *
     * @param id 订单 ID
     * @return HTTP 200 + {@code R<OrderClientDTO>}
     */
    @POST
    @Path("/{id}/ship")
    @Operation(summary = "订单发货", description = "对已支付的订单执行发货，状态流转 PAID → SHIPPED")
    public Response ship(@PathParam("id") Long id) {
        return ok(toClientDTO(orderApplicationService.shipOrder(id)));
    }

    /**
     * 取消订单（CREATED/PAID → CANCELLED）。
     *
     * @param id 订单 ID
     * @return HTTP 200 + {@code R<OrderClientDTO>}
     */
    @POST
    @Path("/{id}/cancel")
    @Operation(summary = "取消订单", description = "取消指定订单，状态流转 CREATED/PAID → CANCELLED")
    public Response cancel(@PathParam("id") Long id) {
        return ok(toClientDTO(orderApplicationService.cancelOrder(id)));
    }

    /**
     * 分页查询订单。
     *
     * @param page    页码（从 1 开始，默认 1）
     * @param size    每页大小（默认 10）
     * @param orderNo 订单编号（可选，精确匹配）
     * @param buyerId 买家 ID（可选，精确匹配）
     * @param status  订单状态（可选，精确匹配，如 CREATED）
     * @return HTTP 200 + {@code R<Page<OrderClientDTO>>}
     */
    @GET
    @Operation(summary = "分页查询订单", description = "按订单编号/买家/状态组合条件分页查询")
    public Response page(@QueryParam("page") @DefaultValue("1") int page,
                         @QueryParam("size") @DefaultValue("10") int size,
                         @QueryParam("orderNo") String orderNo,
                         @QueryParam("buyerId") String buyerId,
                         @QueryParam("status") String status) {
        OrderStatus orderStatus = parseStatus(status);
        if (status != null && !status.isBlank() && orderStatus == null) {
            return badRequest("非法订单状态: " + status);
        }
        Page<OrderDTO> pageResult = orderApplicationService.pageOrders(
                new OrderQuery(orderNo, buyerId, orderStatus), page, size);
        return ok(transfer(pageResult, this::toClientDTO));
    }

    // ==================== 客户端契约实现（供其他适配器/服务调用） ====================

    @Override
    public OrderClientDTO getOrderById(Long id) {
        return toClientDTO(orderApplicationService.getOrder(id));
    }

    @Override
    public OrderClientDTO createOrder(String orderNo, String buyerId, String buyerName) {
        return toClientDTO(orderApplicationService.createOrder(
                new CreateOrderCommand(orderNo, buyerId, buyerName)));
    }

    @Override
    public OrderClientDTO createOrder(CreateOrderRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return createOrder(request.orderNo(), request.buyerId(), request.buyerName());
    }

    // ==================== 内部转换 ====================

    /**
     * 解析状态查询参数：空值/空串返回 {@code null}（不过滤），非法值返回 {@code null} 由调用方判定。
     */
    private OrderStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 应用层 DTO → 客户端契约 DTO（状态枚举转换为名称字符串）。
     */
    private OrderClientDTO toClientDTO(OrderDTO dto) {
        if (dto == null) {
            return null;
        }
        return new OrderClientDTO(
                dto.getId(),
                dto.getOrderNo(),
                dto.getBuyerId(),
                dto.getBuyerName(),
                dto.getStatus() == null ? null : dto.getStatus().name(),
                dto.getTotalAmount(),
                dto.getCreatedTime());
    }
}
