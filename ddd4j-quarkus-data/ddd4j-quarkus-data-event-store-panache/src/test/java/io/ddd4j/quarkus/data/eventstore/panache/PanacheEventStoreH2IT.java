package io.ddd4j.quarkus.data.eventstore.panache;

import io.ddd4j.core.cqrs.eventstore.EventStore;
import io.ddd4j.core.ddd.event.AggregateRootId;
import io.ddd4j.core.ddd.event.DomainEvent;
import io.ddd4j.core.ddd.event.EntityType;
import io.ddd4j.core.ddd.event.StringEntityId;
import io.ddd4j.core.ddd.event.StringEntityType;
import io.ddd4j.core.cqrs.eventstore.AggregateVersionConflictException;
import io.ddd4j.core.cqrs.eventstore.StoredEvent;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * {@link PanacheEventStore} H2 全量契约集成测试（本地必跑轨，Docker 无关）。
 *
 * <p>真实 Quarkus 容器（@QuarkusTest）＋真实 H2 内存库（{@code application.properties}，
 * devservices 关闭）＋真实 {@code EventPayloadSerializer} Bean（mapper 经
 * {@code findAndAddModules} 构建，零 mock）——完整验证 SPI 语义↔Panache 持久化原语的
 * 组装：追加/读回往返（真实 Jackson 序列化）、乐观锁冲突、版本区间读、全局 position
 * 读流。契约面与主仓 {@code PanacheEventStoreIT} 对齐，fixture 复用其模式
 * （自有 record 聚合根标识＋带业务字段的样本事件）。
 *
 * <p>用例间隔离：每用例前清空表（{@link PanacheEventStoreH2ItCdiConfig#clearStream}）；
 * position 为 IDENTITY 自增不重置，凡涉 position 的断言只做单调性与相对顺序，
 * 不做绝对值断言。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 4.0.x
 */
@DisplayName("PanacheEventStore H2 全量契约 IT")
@QuarkusTest
class PanacheEventStoreH2IT {

    private static final String AGGREGATE_TYPE = "SampleAggregate";

    @Inject
    EventStore eventStore;

    @Inject
    PanacheEventStoreH2ItCdiConfig itConfig;

    @BeforeEach
    void cleanStream() {
        itConfig.clearStream();
    }

    @Test
    void appendThenRead_三事件追加_应全量读回且真实Jackson往返() {
        OccurredEvent first = new OccurredEvent("created");
        OccurredEvent second = new OccurredEvent("renamed");
        OccurredEvent third = new OccurredEvent("removed");
        ItAggregateId aggregateId = new ItAggregateId("agg-1");

        eventStore.append(AGGREGATE_TYPE, aggregateId, List.of(first, second, third), 0L);

        List<StoredEvent> stored = eventStore.read(AGGREGATE_TYPE, aggregateId);
        assertThat(stored).hasSize(3);
        assertThat(stored).extracting(StoredEvent::version).containsExactly(1L, 2L, 3L);
        // eventId／聚合定位经持久化往返保持
        assertThat(stored).extracting(event -> event.eventId().asString())
                .containsExactly(first.getEventId().asString(), second.getEventId().asString(),
                        third.getEventId().asString());
        assertThat(stored).extracting(StoredEvent::aggregateType).containsOnly(AGGREGATE_TYPE);
        assertThat(stored).allSatisfy(event -> {
            assertThat(event.aggregateId().asString()).isEqualTo("agg-1");
            // 真实 Jackson 往返：payload 反序列化为原事件类型且业务字段等值
            assertThat(event.payload()).isInstanceOf(OccurredEvent.class);
            assertThat(event.timestamp()).isNotNull();
            assertThat(event.position()).isPositive();
            // 无因果事件的 correlationId/causationId 为 null（空安全解析路径）
            assertThat(event.correlationId()).isNull();
            assertThat(event.causationId()).isNull();
        });
        assertThat(((OccurredEvent) stored.get(0).payload()).getFact()).isEqualTo("created");
        assertThat(((OccurredEvent) stored.get(1).payload()).getFact()).isEqualTo("renamed");
        assertThat(((OccurredEvent) stored.get(2).payload()).getFact()).isEqualTo("removed");
    }

    @Test
    void append_乐观锁冲突_应抛异常且库中事件数不变() {
        ItAggregateId aggregateId = new ItAggregateId("agg-1");
        eventStore.append(AGGREGATE_TYPE, aggregateId,
                List.of(new OccurredEvent("created"), new OccurredEvent("renamed")), 0L);

        AggregateVersionConflictException exception = catchThrowableOfType(
                AggregateVersionConflictException.class,
                () -> eventStore.append(AGGREGATE_TYPE, aggregateId, List.of(new OccurredEvent("removed")), 0L));

        assertThat(exception).isNotNull();
        assertThat(exception.aggregateType()).isEqualTo(AGGREGATE_TYPE);
        assertThat(exception.aggregateId()).isEqualTo("agg-1");
        assertThat(exception.expectedVersion()).isZero();
        assertThat(exception.actualVersion()).isEqualTo(2L);
        // 冲突短路：库中仍只有已提交的 2 条，未留半截流
        assertThat(eventStore.read(AGGREGATE_TYPE, aggregateId)).hasSize(2);
    }

    @Test
    void readByVersionRange_应返回闭区间内事件() {
        ItAggregateId aggregateId = new ItAggregateId("agg-1");
        eventStore.append(AGGREGATE_TYPE, aggregateId,
                List.of(new OccurredEvent("v1"), new OccurredEvent("v2"), new OccurredEvent("v3")), 0L);

        assertThat(eventStore.read(AGGREGATE_TYPE, aggregateId, 2L, 3L))
                .extracting(StoredEvent::version).containsExactly(2L, 3L);
        assertThat(eventStore.read(AGGREGATE_TYPE, aggregateId, 1L, 1L))
                .extracting(StoredEvent::version).containsExactly(1L);
    }

    @Test
    void readAll_跨聚合追加_应按全局position递增且limit生效() {
        ItAggregateId firstAggregate = new ItAggregateId("agg-a");
        ItAggregateId secondAggregate = new ItAggregateId("agg-b");
        eventStore.append(AGGREGATE_TYPE, firstAggregate,
                List.of(new OccurredEvent("a1"), new OccurredEvent("a2")), 0L);
        eventStore.append(AGGREGATE_TYPE, secondAggregate, List.of(new OccurredEvent("b1")), 0L);

        List<StoredEvent> all = eventStore.readAll(0L, 10);
        assertThat(all).hasSize(3);
        // 追加顺序即全局流顺序：跨聚合按 position 递增（含端点、无重复）
        assertThat(all).extracting(StoredEvent::position).isSorted().doesNotHaveDuplicates();
        assertThat(all).extracting(event -> event.aggregateId().asString())
                .containsExactly("agg-a", "agg-a", "agg-b");

        // limit 截断：只取全局流前 2 条（StoredEvent 无 equals，按 position 比对）
        assertThat(eventStore.readAll(0L, 2)).extracting(StoredEvent::position)
                .containsExactly(all.get(0).position(), all.get(1).position());

        // fromPosition 含端点：从首条 position 起读为全量，越过后为尾部 2 条
        long firstPosition = all.get(0).position();
        assertThat(eventStore.readAll(firstPosition, 10)).hasSize(3);
        assertThat(eventStore.readAll(firstPosition + 1, 10)).extracting(StoredEvent::position)
                .containsExactly(all.get(1).position(), all.get(2).position());
    }

    /**
     * IT 样本事件：带一个业务字段的 payload 载体，验证真实 Jackson 往返等值。
     */
    static final class OccurredEvent extends DomainEvent<StringEntityId> {

        private String fact;

        OccurredEvent() {
            super("agg-1");
        }

        OccurredEvent(String fact) {
            this();
            this.fact = fact;
        }

        public String getFact() {
            return fact;
        }

        public void setFact(String fact) {
            this.fact = fact;
        }
    }

    /**
     * IT 自有字符串聚合根标识（受测类内部 StringAggregateRootId 为 private，无法引用；
     * 契约与 AggregateRootId 三方法一致）。
     */
    private record ItAggregateId(String value) implements AggregateRootId {

        private static final EntityType TYPE = new StringEntityType("SampleAggregate");

        @Override
        public EntityType getType() {
            return TYPE;
        }

        @Override
        public String asString() {
            return value;
        }

        @Override
        public String asTypedString() {
            return TYPE.asString() + ":" + value;
        }
    }
}
