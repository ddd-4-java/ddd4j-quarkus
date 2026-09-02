package io.ddd4j.quarkus.data.eventstore.panache;

import com.fasterxml.jackson.annotation.JsonValue;
import io.ddd4j.core.ddd.event.AggregateRootId;
import io.ddd4j.core.ddd.event.DomainEvent;
import io.ddd4j.core.ddd.event.EntityType;
import io.ddd4j.core.ddd.event.EventId;
import io.ddd4j.core.ddd.event.StringEntityType;
import io.ddd4j.core.cqrs.eventstore.AggregateVersionConflictException;
import io.ddd4j.core.cqrs.eventstore.EventStore;
import io.ddd4j.core.cqrs.eventstore.StoredEvent;
import io.ddd4j.core.cqrs.eventstore.jackson.EventPayloadSerializer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 基于 Quarkus Hibernate ORM Panache 的 {@link EventStore} 实现（对齐主仓
 * {@code io.ddd4j:data-event-store-panache} 的 {@code PanacheEventStore}，EventStore SPI
 * 见主仓 {@code docs/adr/0005-event-store-spi.md}）。
 *
 * <p>适用于 Quarkus 3.x 运行时；Spring 系运行时请用 {@code ddd4j-data-event-store-jpa}，
 * Javalin 请用 {@code ddd4j-data-event-store-jdbi}，响应式请用
 * {@code ddd4j-data-event-store-r2dbc}。本类只做「SPI 语义 ↔ Panache 持久化原语」的
 * 适配组装：active record 实体（{@link PanacheStoredEventEntity}）承载列映射与静态
 * 查询原语，并发检查、异常翻译、序列化均在本层完成。
 *
 * <h3>序列化器装配</h3>
 * <p>{@link EventPayloadSerializer} 是纯类（无任何容器注解，跨运行时共享），Quarkus
 * 集成方需自行注册其 Bean，例如声明 {@code @ApplicationScoped} 生产者：
 * {@code @Produces EventPayloadSerializer eventPayloadSerializer(ObjectMapper mapper)}
 * （mapper 建议 {@code findAndAddModules} 构建）。
 *
 * <p>生命周期不入 SPI（ADR-0003）：事务由 {@code jakarta.transaction.Transactional}
 * （Quarkus Narayana JTA，勿与 Spring 的同名注解混用）声明式管理，资源由运行时容器托管，
 * 无隐式 open/close。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @see PanacheStoredEventEntity
 * @since 4.0.x
 */
@ApplicationScoped
public class PanacheEventStore implements EventStore {

    private final EventPayloadSerializer serializer;

    /**
     * 创建 Panache 事件存储。
     *
     * @param serializer 领域事件 payload 序列化器（集成方供 Bean）
     */
    @Inject
    public PanacheEventStore(EventPayloadSerializer serializer) {
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
    }

    /**
     * {@inheritDoc}
     *
     * <p>乐观锁＋唯一约束双保险：本方法先以
     * {@link PanacheStoredEventEntity#findCurrentVersion} 读取流当前版本，
     * 与 {@code expectedVersion} 不一致即抛 {@link AggregateVersionConflictException}
     * （第一道，语义层）；即便并发窗口漏检，{@code uk_aggregate_version} 唯一约束
     * 也会让重复版本号插入失败（第二道，数据层兜底）。全程运行在同一事务内
     * （{@code @Transactional}），冲突或序列化失败时整体回滚，不留半截流。
     */
    @Override
    @Transactional
    public void append(String aggregateType, AggregateRootId aggregateId,
                       List<? extends DomainEvent<?>> events, long expectedVersion) {
        Objects.requireNonNull(events, "events must not be null");
        long actualVersion = PanacheStoredEventEntity.findCurrentVersion(
                aggregateType, aggregateId.asString());
        if (actualVersion != expectedVersion) {
            throw new AggregateVersionConflictException(
                    aggregateType, aggregateId.asString(), expectedVersion, actualVersion);
        }
        ZonedDateTime now = ZonedDateTime.now();
        long version = expectedVersion;
        for (DomainEvent<?> event : events) {
            version++;
            PanacheStoredEventEntity entity = new PanacheStoredEventEntity();
            entity.eventId = event.getEventId().asString();
            entity.aggregateType = aggregateType;
            entity.aggregateId = aggregateId.asString();
            entity.version = version;
            entity.eventType = event.getClass().getName();
            entity.payload = serializer.serialize(event);
            entity.correlationId = event.getCorrelationId() != null ? event.getCorrelationId().asString() : null;
            entity.causationId = event.getCausationId() != null ? event.getCausationId().asString() : null;
            entity.createdAt = now;
            entity.persist();
        }
    }

    @Override
    public List<StoredEvent> read(String aggregateType, AggregateRootId aggregateId) {
        return PanacheStoredEventEntity.findByAggregate(aggregateType, aggregateId.asString())
                .stream().map(this::toStoredEvent).toList();
    }

    @Override
    public List<StoredEvent> read(String aggregateType, AggregateRootId aggregateId,
                                  long fromVersion, long toVersion) {
        // 局部变量显式目标类型：继承自 PanacheEntityBase 的静态泛型 find 在链式方法引用
        // 场景下 javac 无法把 this::toStoredEvent 的参数类型回传推断，会兜底为 PanacheEntityBase
        List<PanacheStoredEventEntity> entities = PanacheStoredEventEntity
                .find("aggregateType = ?1 and aggregateId = ?2 and version between ?3 and ?4 order by version",
                        aggregateType, aggregateId.asString(), fromVersion, toVersion)
                .list();
        return entities.stream().map(this::toStoredEvent).toList();
    }

    @Override
    public List<StoredEvent> readAll(long fromPosition, int limit) {
        List<PanacheStoredEventEntity> entities = PanacheStoredEventEntity
                .find("position >= ?1 order by position", fromPosition)
                .page(0, limit).list();
        return entities.stream().map(this::toStoredEvent).toList();
    }

    /**
     * 把持久化实体重建为 {@link StoredEvent}：事件类型经 {@link Class#forName} 还原，
     * payload 经 {@link EventPayloadSerializer#deserialize} 反序列化，
     * {@code eventId}／{@code correlationId}／{@code causationId} 经
     * {@link EventId#valueOf}（空安全）解析。
     *
     * <p>{@code position} 由数据库生成：持久化读回必非空，此处 fail-loud 断言——
     * 瞬态实体（未落库、无 position）进入重建路径视为编程错误，直接抛
     * {@link NullPointerException} 而非静默按 0 处理。
     *
     * @param entity 持久化实体
     * @return 重建的持久化事件快照
     * @throws NullPointerException entity 未持久化（position 为 null）
     * @throws IllegalStateException eventType 类不存在（事件类被重命名/删除后旧流不可读）
     */
    private StoredEvent toStoredEvent(PanacheStoredEventEntity entity) {
        DomainEvent<?> payload = serializer.deserialize(entity.payload, resolveEventType(entity.eventType));
        return new StoredEvent(
                EventId.valueOf(entity.eventId),
                entity.aggregateType,
                new StringAggregateRootId(entity.aggregateId),
                entity.version,
                Objects.requireNonNull(entity.position,
                        "position must not be null in read path (transient entities unsupported)"),
                entity.createdAt,
                payload,
                EventId.valueOf(entity.correlationId),
                EventId.valueOf(entity.causationId));
    }

    /**
     * 按限定名还原事件类型。
     *
     * @param eventType 事件类型限定名
     * @return 事件类型
     * @throws IllegalStateException 类不存在
     */
    @SuppressWarnings("unchecked")
    private Class<? extends DomainEvent<?>> resolveEventType(String eventType) {
        try {
            return (Class<? extends DomainEvent<?>>) Class.forName(eventType);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unknown event type: " + eventType, e);
        }
    }

    /**
     * 字符串聚合根标识适配器：实体列 {@code aggregate_id} 只存字符串，读回侧需重建
     * {@link AggregateRootId} 接口实例（{@code StringEntityId} 仅实现
     * {@code EntityId}，不满足 {@link StoredEvent} 构造器约束）。
     *
     * <p>三方法契约与 {@code StringEntityId} 一致：类型固定 {@code String}、
     * 原值与 {@code 类型:值} 形式（与主仓 {@code PanacheEventStore} 内同名适配器对齐）。
     */
    private record StringAggregateRootId(String value) implements AggregateRootId {

        /** 字符串聚合根标识的固定类型。 */
        private static final StringEntityType TYPE = new StringEntityType("String");

        @Override
        public EntityType getType() {
            return TYPE;
        }

        @Override
        @JsonValue
        public String asString() {
            return value;
        }

        @Override
        public String asTypedString() {
            return TYPE.asString() + ":" + value;
        }
    }
}
