package io.ddd4j.quarkus.sample.client.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单客户端 DTO（对外 API 契约）。
 *
 * <p>客户端模块（Client Module）是分层架构中的接口契约层，本 DTO 定义了
 * 订单服务对外暴露的数据结构，供调用方（前端、网关、其他服务）使用。
 * 对外 DTO 与内部领域模型解耦：不暴露 {@code AggregateRoot} 的任何内部细节，
 * 状态以字符串形式返回（{@link #status()} 为 {@code OrderStatus} 枚举名称）。</p>
 *
 * <p>使用 {@code record} 声明不可变 DTO，天然支持 Jackson 序列化
 * （Quarkus REST Jackson 对 record 开箱即用），并通过 {@link Schema}
 * 注解生成 OpenAPI 文档。</p>
 *
 * @param id          订单 ID（领域标识）
 * @param orderNo     订单编号（业务主键）
 * @param buyerId     买家 ID
 * @param buyerName   买家名称
 * @param status      订单状态（CREATED/PAID/SHIPPED/COMPLETED/CANCELLED）
 * @param totalAmount 订单总金额
 * @param createdTime 创建时间
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Schema(description = "订单客户端 DTO（对外 API 契约）")
public record OrderClientDTO(
        @Schema(description = "订单ID", example = "1") Long id,
        @Schema(description = "订单编号", example = "ORD202608040001") String orderNo,
        @Schema(description = "买家ID", example = "buyer-1001") String buyerId,
        @Schema(description = "买家名称", example = "张三") String buyerName,
        @Schema(description = "订单状态", example = "CREATED",
                allowableValues = {"CREATED", "PAID", "SHIPPED", "COMPLETED", "CANCELLED"}) String status,
        @Schema(description = "订单总金额", example = "199.00") BigDecimal totalAmount,
        @Schema(description = "创建时间", example = "2026-08-04T10:00:00") LocalDateTime createdTime
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
