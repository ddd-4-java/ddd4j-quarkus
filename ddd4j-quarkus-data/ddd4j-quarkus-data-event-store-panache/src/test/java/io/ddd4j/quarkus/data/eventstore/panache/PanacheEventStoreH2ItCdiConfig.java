package io.ddd4j.quarkus.data.eventstore.panache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ddd4j.core.cqrs.eventstore.jackson.EventPayloadSerializer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.transaction.Transactional;

/**
 * 集成测试 CDI 装配（H2 内存库 @QuarkusTest 轨专用）。
 *
 * <p>{@link EventPayloadSerializer} 是纯类（无容器注解，跨运行时共享），此处按
 * {@link PanacheEventStore} javadoc 的 Quarkus 集成方装配方式注册真实 Bean：
 * mapper 以 {@code findAndRegisterModules} 构建（Jackson 2 / {@code com.fasterxml.jackson}，
 * 本仓 ddd4j.version=2.0.x 主线为 Jackson 2 基线；java.time 等经 ServiceLoader
 * 发现的 module 一并注册），序列化/反序列化零 mock——这正是集成测试要验的真实往返。
 *
 * <p>{@link #clearStream} 提供用例间隔离：每用例前清空 {@code ddd4j_stored_event}
 * （position 为 IDENTITY 自增不重置，凡涉 position 的断言只做单调性与相对顺序，
 * 不做绝对值断言）。写操作需活动事务，故以 {@code @Transactional} 包装。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x（backport from 4.0.x；Jackson 3 → Jackson 2 适配）
 */
@ApplicationScoped
class PanacheEventStoreH2ItCdiConfig {

    /**
     * 注册真实的事件 payload 序列化器（mapper 经 findAndRegisterModules 构建，非 mock）。
     *
     * @return 序列化器 Bean
     */
    @Produces
    EventPayloadSerializer eventPayloadSerializer() {
        return new EventPayloadSerializer(new ObjectMapper().findAndRegisterModules());
    }

    /**
     * 清空事件存储表（用例间隔离）。
     */
    @Transactional
    void clearStream() {
        PanacheStoredEventEntity.deleteAll();
    }
}
