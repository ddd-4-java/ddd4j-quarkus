package io.ddd4j.quarkus.sample.client.order;

import java.util.Objects;

/**
 * 订单客户端调用接口（对外 API 契约）。
 *
 * <p>客户端模块（Client Module）是分层架构中的接口契约层：
 * 本接口定义了订单服务对外提供的服务能力，由适配层（Adapter）实现，
 * 供前端、网关或其他服务调用。调用方只依赖 {@link OrderClientDTO} 等
 * 客户端契约，不感知应用层、领域层的任何内部实现。</p>
 *
 * <p>分层调用链：{@code Client 契约（本接口）→ Adapter 实现 → ApplicationService → Domain}。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 适配层注入实现
 * @Inject
 * OrderClientService orderClientService;
 *
 * // 按 ID 查询订单
 * OrderClientDTO order = orderClientService.getOrderById(1L);
 *
 * // 创建订单
 * OrderClientDTO created = orderClientService.createOrder("ORD202608040001", 1001L, "张三");
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public interface OrderClientService {

    /**
     * 按订单 ID 查询订单。
     *
     * @param id 订单 ID
     * @return 订单客户端 DTO
     * @throws IllegalArgumentException 订单不存在时抛出
     */
    OrderClientDTO getOrderById(Long id);

    /**
     * 创建订单。
     *
     * @param orderNo   订单编号（业务主键，必填）
     * @param buyerId   买家 ID（必填）
     * @param buyerName 买家名称（必填）
     * @return 创建成功后的订单客户端 DTO
     */
    default OrderClientDTO createOrder(String orderNo, String buyerId, String buyerName) {
        Objects.requireNonNull(orderNo, "orderNo must not be null");
        Objects.requireNonNull(buyerId, "buyerId must not be null");
        Objects.requireNonNull(buyerName, "buyerName must not be null");
        return createOrder(new CreateOrderRequest(orderNo, buyerId, buyerName));
    }

    /**
     * 创建订单（请求参数封装版本）。
     *
     * @param request 创建订单请求
     * @return 创建成功后的订单客户端 DTO
     */
    OrderClientDTO createOrder(CreateOrderRequest request);

    /**
     * 创建订单请求（客户端契约，与具体实现解耦）。
     *
     * @param orderNo   订单编号
     * @param buyerId   买家 ID
     * @param buyerName 买家名称
     */
    record CreateOrderRequest(String orderNo, String buyerId, String buyerName) {
    }
}
