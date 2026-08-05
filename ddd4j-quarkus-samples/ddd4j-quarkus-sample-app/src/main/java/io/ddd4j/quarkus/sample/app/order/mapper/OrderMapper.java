package io.ddd4j.quarkus.sample.app.order.mapper;

import io.ddd4j.quarkus.sample.app.order.dto.OrderDTO;
import io.ddd4j.quarkus.sample.domain.order.model.aggregate.Order;
import org.mapstruct.Mapper;

/**
 * 订单对象映射器（MapStruct）。
 *
 * <p>负责领域聚合根 {@link Order} 与应用层 DTO {@link OrderDTO} 之间的转换，
 * 隔离领域模型与上层模型，避免上层直接依赖聚合根内部结构。</p>
 *
 * <p>使用 {@code componentModel = "jakarta"}：生成的实现类由 Quarkus Arc
 * （CDI）自动发现并装配，应用服务直接注入本接口即可（需要 MapStruct 1.6+）。</p>
 *
 * <p>转换规则：{@code Order} 与 {@code OrderDTO} 字段同名同类型
 * （id/orderNo/buyerId/buyerName/status/totalAmount/createdTime），
 * MapStruct 自动映射，无需显式声明。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Mapper(componentModel = "jakarta")
public interface OrderMapper {

    /**
     * 领域聚合根转应用层 DTO。
     *
     * @param order 订单聚合根
     * @return 订单应用层 DTO（{@code order} 为 {@code null} 时返回 {@code null}）
     */
    OrderDTO toDTO(Order order);
}
