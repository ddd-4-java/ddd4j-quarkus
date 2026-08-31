/*
 * Copyright (c) 2024-2026 ddd4j project. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ddd4j.quarkus.data.eventstore.panache;

import io.ddd4j.core.cqrs.eventstore.EventDeserializer;
import io.ddd4j.core.cqrs.eventstore.EventStore;
import io.ddd4j.core.cqrs.eventstore.StoredEvent;
import io.ddd4j.kit.lang.JsonKit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 基于 Quarkus Panache 的 {@link EventStore} 实现。
 *
 * <p>实现主仓 {@code io.ddd4j.core.cqrs.eventstore.EventStore}（同步 SPI，唯一事实源），
 * 表结构与主仓 {@code ddd4j-data-event-store-jpa} 的 JPA 实现对齐
 * （表 {@code DDD4J_EVENT_STORE}，复合主键 {@code (aggregate_id, version)}）。
 *
 * <h3>并发控制</h3>
 * <p>乐观锁：先查询当前流最大版本，与 {@code expectedVersion} 不一致即抛
 * {@link IllegalStateException}。复合主键 {@code (aggregate_id, version)} 是数据层兜底。
 * 全程运行在同一事务内，冲突或序列化失败时整体回滚，不留半截流。
 *
 * <h3>payload 序列化</h3>
 * <p>事件载荷通过 {@link JsonKit#toJson} 序列化为 JSON 文本存储，
 * 读取时通过 {@link EventDeserializer#deserialize} 按 {@code event_type} 反序列化
 * （不可信类名白名单校验，类缺失时回退 {@code Map}）。
 *
 * <p>生命周期不入 SPI：事务由 {@code @Transactional} 声明式管理（jakarta/Narayana），
 * 资源由 Quarkus 容器托管。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @see PanacheStoredEventEntity
 * @since 4.0.x
 */
@ApplicationScoped
public class PanacheEventStore implements EventStore {

    /**
     * {@inheritDoc}
     *
     * <p>版本从 {@code expectedVersion + 1} 开始递增（与主仓 JPA 实现一致）。
     */
    @Override
    @Transactional
    public void append(String aggregateId, List<Object> events, long expectedVersion) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(events, "events must not be null");
        if (events.isEmpty()) {
            return;
        }

        long currentVersion = PanacheStoredEventEntity.findCurrentVersion(aggregateId);
        if (currentVersion != expectedVersion) {
            throw new IllegalStateException(
                    "Version conflict: expected " + expectedVersion + " but was " + currentVersion);
        }

        Instant now = Instant.now();
        long version = expectedVersion;
        for (Object event : events) {
            PanacheStoredEventEntity entity = new PanacheStoredEventEntity();
            entity.aggregateId = aggregateId;
            entity.version = version;
            entity.position = PanacheStoredEventEntity.nextPosition();
            entity.eventType = event.getClass().getName();
            entity.payload = JsonKit.toJson(event);
            entity.timestamp = now;
            entity.persist();
            version++;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>事件载荷经 {@link EventDeserializer} 反序列化，类型无法还原时回退为 {@code Map}。
     */
    @Override
    public List<StoredEvent> read(String aggregateId) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        return PanacheStoredEventEntity.findByAggregateIdOrderByVersionAsc(aggregateId)
                .stream()
                .map(PanacheEventStore::toStoredEvent)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StoredEvent> readAll(long fromPosition, int limit) {
        return PanacheStoredEventEntity.findByPositionGreaterThanEqual(fromPosition, limit)
                .stream()
                .map(PanacheEventStore::toStoredEvent)
                .toList();
    }

    /**
     * 把持久化实体重建为 {@link StoredEvent}（core record 形态）。
     *
     * @param entity 持久化实体
     * @return 重建的存储事件
     */
    private static StoredEvent toStoredEvent(PanacheStoredEventEntity entity) {
        Object event = EventDeserializer.deserialize(entity.payload, entity.eventType);
        return new StoredEvent(entity.aggregateId, entity.version, event, entity.position, entity.timestamp);
    }
}
