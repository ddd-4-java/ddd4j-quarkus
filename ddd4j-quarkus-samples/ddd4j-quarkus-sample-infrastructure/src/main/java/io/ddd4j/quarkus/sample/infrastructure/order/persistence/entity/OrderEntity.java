package io.ddd4j.quarkus.sample.infrastructure.order.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单持久化实体（基础设施层）。
 *
 * <p>基于 Quarkus Panache（{@link PanacheEntityBase}），字段与领域聚合根
 * {@code Order} 一一对应；{@code status} 以 {@code OrderStatus.getCode()} 字符串存储。
 * 使用 Panache 静态方法 {@code find / list / persist / deleteById} 完成数据访问，
 * 多租户场景可改用 {@code TenantAwareEntity} 基类（复合主键 tenantId + 雪花 id）。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@Entity
@Table(name = "t_order")
public class OrderEntity extends PanacheEntityBase {

    /**
     * 主键（数据库自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /**
     * 订单编号
     */
    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    public String orderNo;

    /**
     * 买家 ID
     */
    @Column(name = "buyer_id", nullable = false, length = 64)
    public String buyerId;

    /**
     * 买家名称
     */
    @Column(name = "buyer_name", length = 64)
    public String buyerName;

    /**
     * 订单状态（OrderStatus.getCode() 编码值）
     */
    @Column(nullable = false, length = 32)
    public String status;

    /**
     * 订单总金额
     */
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    public BigDecimal totalAmount;

    /**
     * 创建时间
     */
    @Column(name = "created_time")
    public LocalDateTime createdTime;
}
