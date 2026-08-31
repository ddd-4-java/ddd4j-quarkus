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

import io.ddd4j.core.constant.EventStoreConstants;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Panache 事件存储实体（Active Record 公有字段风格）。
 *
 * <p>表结构与主仓 {@code ddd4j-data-event-store-jpa} 的 {@code StoredEventEntity} 对齐：
 * 表 {@code DDD4J_EVENT_STORE}，复合主键 {@code (aggregate_id, version)}，
 * {@code position} 全局唯一递增（应用侧分配，见 {@link #nextPosition()}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 4.0.x
 */
@Entity
@Table(name = EventStoreConstants.TABLE_NAME)
@IdClass(PanacheStoredEventEntityId.class)
public class PanacheStoredEventEntity extends PanacheEntityBase {

    /**
     * 聚合标识（复合主键之一）。
     */
    @Id
    @Column(name = EventStoreConstants.COLUMN_AGGREGATE_ID, nullable = false, length = 255)
    public String aggregateId;

    /**
     * 聚合流内版本号（复合主键之一）。
     */
    @Id
    @Column(name = EventStoreConstants.COLUMN_VERSION, nullable = false)
    public long version;

    /**
     * 全局递增序号（唯一，用于全局分页读取）。
     */
    @Column(name = EventStoreConstants.COLUMN_POSITION, nullable = false, unique = true)
    public long position;

    /**
     * 事件类型全限定名。
     */
    @Column(name = EventStoreConstants.COLUMN_EVENT_TYPE, nullable = false, length = 512)
    public String eventType;

    /**
     * 事件标识（UUID）。
     */
    @Column(name = EventStoreConstants.COLUMN_EVENT_ID, length = 64)
    public String eventId;

    /**
     * 事件载荷（JSON）。
     */
    @Column(name = EventStoreConstants.COLUMN_PAYLOAD, nullable = false, columnDefinition = "CLOB")
    public String payload;

    /**
     * 事件存储时间。
     */
    @Column(name = EventStoreConstants.COLUMN_TIMESTAMP, nullable = false)
    public Instant timestamp;

    /**
     * 查询当前聚合流的最新版本号。
     *
     * @param aggregateId 聚合标识
     * @return 当前版本号；无事件时返回 0
     */
    public static long findCurrentVersion(String aggregateId) {
        Long maxVersion = find("select max(e.version) from PanacheStoredEventEntity e where e.aggregateId = ?1",
                aggregateId).project(Long.class).firstResult();
        return maxVersion == null ? 0L : maxVersion;
    }

    /**
     * 分配下一个全局 position（单实例安全；集群部署需数据库序列或唯一约束重试）。
     *
     * @return 下一个 position
     */
    public static long nextPosition() {
        Long maxPosition = find("select coalesce(max(e.position), 0) + 1 from PanacheStoredEventEntity e")
                .project(Long.class).firstResult();
        return maxPosition == null ? 1L : maxPosition;
    }

    /**
     * 查询指定聚合的全部事件（按版本升序）。
     *
     * @param aggregateId 聚合标识
     * @return 事件列表
     */
    public static java.util.List<PanacheStoredEventEntity> findByAggregateIdOrderByVersionAsc(String aggregateId) {
        return find("aggregateId = ?1 order by version asc", aggregateId).list();
    }

    /**
     * 查询全局事件流（按 position 升序，用于 projection）。
     *
     * @param fromPosition 起始 position（含）
     * @param limit        最大读取数量
     * @return 事件列表
     */
    public static java.util.List<PanacheStoredEventEntity> findByPositionGreaterThanEqual(long fromPosition, int limit) {
        return find("position >= ?1 order by position asc", fromPosition).page(0, limit).list();
    }
}
