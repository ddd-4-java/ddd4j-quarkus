package io.ddd4j.quarkus.sample.app.order.service;

import io.ddd4j.core.api.Page;
import io.ddd4j.quarkus.sample.app.order.command.CreateOrderCommand;
import io.ddd4j.quarkus.sample.app.order.dto.OrderDTO;
import io.ddd4j.quarkus.sample.app.order.mapper.OrderMapper;
import io.ddd4j.quarkus.sample.app.order.query.OrderQuery;
import io.ddd4j.quarkus.sample.domain.order.model.aggregate.Order;
import io.ddd4j.quarkus.sample.domain.order.repository.OrderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Objects;

/**
 * 订单应用服务（应用层用例编排）。
 *
 * <p>应用层（Application Layer）负责：接收命令/查询 → 加载聚合根 →
 * 调用聚合根业务方法完成状态流转 → 通过仓储持久化 → 转换为 DTO 返回。
 * 应用服务<b>不承载业务规则</b>，业务规则全部下沉到 {@link Order} 聚合根
 * （充血模型）与领域层。</p>
 *
 * <p>分层调用链：
 * {@code Adapter(Web) → OrderApplicationService → OrderRepository → Order(聚合根)}。</p>
 *
 * <p>仓储通过 CDI 注入：示例默认使用内存实现（由主应用模块
 * {@code LayeredSampleConfig} 装配），生产环境可替换为 Panache/JPA 实现。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class OrderApplicationService {

    private static final Logger log = Logger.getLogger(OrderApplicationService.class);

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Inject
    public OrderApplicationService(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.orderMapper = Objects.requireNonNull(orderMapper, "orderMapper must not be null");
    }

    /**
     * 创建订单。
     *
     * @param command 创建订单命令
     * @return 创建成功后的订单 DTO
     */
    public OrderDTO createOrder(CreateOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        // 1. 充血模型：调用聚合根静态工厂创建订单（初始状态 CREATED）
        Order order = Order.create(command.orderNo(), command.buyerId(), command.buyerName());
        // 2. 仓储持久化
        Order saved = orderRepository.save(order);
        log.infof("Order created: id=%s, orderNo=%s, buyer=%s",
                saved.id(), saved.getOrderNo(), saved.getBuyerName());
        // 3. 转换 DTO 返回
        return orderMapper.toDTO(saved);
    }

    /**
     * 支付订单（状态流转：CREATED → PAID）。
     *
     * @param orderId 订单 ID
     * @return 支付后的订单 DTO
     * @throws IllegalArgumentException 订单不存在或状态不允许支付时抛出
     */
    public OrderDTO payOrder(Long orderId) {
        Order order = findRequired(orderId);
        order.pay();
        return orderMapper.toDTO(orderRepository.save(order));
    }

    /**
     * 发货（状态流转：PAID → SHIPPED）。
     *
     * @param orderId 订单 ID
     * @return 发货后的订单 DTO
     * @throws IllegalArgumentException 订单不存在或状态不允许发货时抛出
     */
    public OrderDTO shipOrder(Long orderId) {
        Order order = findRequired(orderId);
        order.ship();
        return orderMapper.toDTO(orderRepository.save(order));
    }

    /**
     * 取消订单（状态流转：CREATED/PAID → CANCELLED）。
     *
     * @param orderId 订单 ID
     * @return 取消后的订单 DTO
     * @throws IllegalArgumentException 订单不存在或状态不允许取消时抛出
     */
    public OrderDTO cancelOrder(Long orderId) {
        Order order = findRequired(orderId);
        order.cancel();
        return orderMapper.toDTO(orderRepository.save(order));
    }

    /**
     * 按 ID 查询订单。
     *
     * @param orderId 订单 ID
     * @return 订单 DTO
     * @throws IllegalArgumentException 订单不存在时抛出
     */
    public OrderDTO getOrder(Long orderId) {
        return orderMapper.toDTO(findRequired(orderId));
    }

    /**
     * 按查询条件分页查询订单。
     *
     * <p>示例使用内存仓储（{@code findAll()} + 内存过滤 + 手动分页），
     * 演示分层结构为主；生产环境应由仓储层按查询条件下推 SQL（Panache/JPA）。</p>
     *
     * @param query 查询条件（条件均可为空，为空表示不过滤）
     * @param page  页码（从 1 开始）
     * @param size  每页大小
     * @return 分页结果（records 为订单 DTO 列表）
     */
    public Page<OrderDTO> pageOrders(OrderQuery query, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        List<Order> filtered = orderRepository.findAll().stream()
                .filter(order -> query.orderNo() == null || query.orderNo().equals(order.getOrderNo()))
                .filter(order -> query.buyerId() == null || query.buyerId().equals(order.getBuyerId()))
                .filter(order -> query.status() == null || query.status() == order.getStatus())
                .toList();
        int total = filtered.size();
        int from = Math.min((safePage - 1) * safeSize, total);
        int to = Math.min(from + safeSize, total);
        List<OrderDTO> records = filtered.subList(from, to).stream().map(orderMapper::toDTO).toList();
        return Page.succeed(records, total, safePage, safeSize);
    }

    /**
     * 加载订单聚合根，不存在时抛出 {@link IllegalArgumentException}（由适配层统一映射为 400）。
     */
    private Order findRequired(Long orderId) {
        Objects.requireNonNull(orderId, "orderId must not be null");
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));
    }
}
