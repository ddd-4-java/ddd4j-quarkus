package io.ddd4j.quarkus.data.eventstore.panache;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 事件存储 Quarkus Panache 实体：以追加写（append-only）方式落地
 * {@code io.ddd4j.core.cqrs.eventstore.StoredEvent}（对齐主仓
 * {@code io.ddd4j:data-event-store-panache} 的同名实体，EventStore SPI 见主仓
 * {@code docs/adr/0005-event-store-spi.md}）。
 *
 * <p>表结构与 {@code ddd4j-data-event-store-jpa} 的 {@code StoredEventEntity} 完全一致
 * （{@code ddd4j_stored_event} 表＋{@code uk_aggregate_version} 唯一约束），同一数据库
 * 可被两种实现的集成方分别使用，但勿在同一应用混用（乐观锁语义以单实现内自洽为准）。
 *
 * <p><b>公有字段风格是 Panache 的刻意约定</b>：Quarkus Panache active record 模式
 * （继承 {@link PanacheEntityBase}）以公有字段直接承载列映射，省去 getter/setter 样板；
 * 与 -jpa 模块的私有字段风格并存是有意的（各随其运行时惯例），勿以封装规范统一改写。
 *
 * <p>设计要点（同 -jpa 模块）：
 * <ul>
 *   <li>{@code position} 为代理主键（IDENTITY 自增），即事件存储的全局流位置；
 *       由数据库生成，禁止业务侧改写。</li>
 *   <li>{@code (aggregate_type, aggregate_id, version)} 唯一约束（{@code uk_aggregate_version}）
 *       是乐观并发控制的数据层兜底：同一聚合重复追加同一版本号将违反约束，
 *       配合 SPI 层 {@code AggregateVersionConflictException} 语义。</li>
 *   <li>{@code payload} 为序列化后的事件负载（JSON 文本），由 SPI 层的
 *       {@code EventPayloadSerializer} 负责多态序列化/反序列化。</li>
 *   <li>{@code correlationId}／{@code causationId} 为可空追踪维度。</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @see io.ddd4j.core.cqrs.eventstore.StoredEvent
 * @see PanacheEventStore
 * @since 4.0.x
 */
@Entity
@Table(name = "ddd4j_stored_event",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_aggregate_version",
           columnNames = {"aggregate_type", "aggregate_id", "version"}))
public class PanacheStoredEventEntity extends PanacheEntityBase {

    /** 全局流位置：数据库自增主键，事件存储的追加顺序号。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long position;

    /** 事件 ID（UUID 字符串，36 字符）。 */
    @Column(name = "event_id", nullable = false, length = 36)
    public String eventId;

    /** 聚合类型名（限定名，最长 128 字符）。 */
    @Column(name = "aggregate_type", nullable = false, length = 128)
    public String aggregateType;

    /** 聚合 ID 字符串形式（最长 128 字符）。 */
    @Column(name = "aggregate_id", nullable = false, length = 128)
    public String aggregateId;

    /** 聚合版本号（乐观并发控制维度，从 1 起）。 */
    @Column(name = "version", nullable = false)
    public Long version;

    /** 事件类型名（限定名，最长 256 字符）。 */
    @Column(name = "event_type", nullable = false, length = 256)
    public String eventType;

    /** 序列化事件负载（JSON 文本）。 */
    @Lob
    @Column(name = "payload", nullable = false)
    public String payload;

    /** 关联 ID（可选，36 字符）。 */
    @Column(name = "correlation_id", length = 36)
    public String correlationId;

    /** 因果 ID（可选，36 字符）。 */
    @Column(name = "causation_id", length = 36)
    public String causationId;

    /** 事件发生时间（带时区）。 */
    @Column(name = "created_at", nullable = false)
    public ZonedDateTime createdAt;

    /**
     * 读取聚合流当前版本（最新 {@code version}）。
     *
     * @param aggregateType 聚合类型
     * @param aggregateId   聚合 ID 字符串形式
     * @return 流当前版本；空流为 {@code 0L}
     */
    public static long findCurrentVersion(String aggregateType, String aggregateId) {
        PanacheStoredEventEntity latest = find(
                "aggregateType = ?1 and aggregateId = ?2 order by version desc",
                aggregateType, aggregateId).firstResult();
        return latest != null ? latest.version : 0L;
    }

    /**
     * 按版本升序读取聚合流全部事件实体。
     *
     * @param aggregateType 聚合类型
     * @param aggregateId   聚合 ID 字符串形式
     * @return 版本升序的持久化实体列表
     */
    public static List<PanacheStoredEventEntity> findByAggregate(String aggregateType, String aggregateId) {
        return list("aggregateType = ?1 and aggregateId = ?2 order by version",
                aggregateType, aggregateId);
    }
}
