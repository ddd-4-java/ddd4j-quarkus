package io.ddd4j.quarkus.ddd.cqrs;

import io.ddd4j.core.cqrs.readmodel.ProjectionPosition;
import io.ddd4j.core.cqrs.readmodel.ProjectionPositionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Quarkus CQRS 投影拉取服务：周期性从事件流拉取增量事件，更新读模型（投影）。
 *
 * <p>由 {@link io.quarkus.scheduler.Scheduled}（quarkus-quartz）触发，对标 ddd4j-runtime-spring 的
 * {@code SpringJpaProjectionService}。每次执行：
 * <ol>
 *   <li>读取各视图当前的 {@link ProjectionPosition}（上次处理到的事件号）</li>
 *   <li>从 EventStore 拉取该位置之后的增量事件</li>
 *   <li>调用对应的事件处理器（{@code @CreateEvent}/@UpdateEvent/@DeleteEvent）更新读模型</li>
 *   <li>持久化新的 ProjectionPosition</li>
 * </ol>
 *
 * <p><b>默认调度周期</b>：每 30 秒执行一次，可通过配置项 {@code ddd4j.quarkus.cqrs.projection.cron} 覆盖。
 *
 * <p><b>ProjectionPositionRepository 来源</b>：通过 CDI {@link Instance} 可选注入。
 * 业务项目需提供该 SPI 的实现 Bean（如基于 Panache 的实现），否则本服务在无注册视图时跳过执行。
 *
 * <p>子类可 override {@link #pullAndApply(String)} 实现具体的投影拉取逻辑。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@ApplicationScoped
public class QuarkusJpaProjectionService {

    private static final Logger logger = Logger.getLogger(QuarkusJpaProjectionService.class);

    /**
     * 可选注入：业务方提供的投影位置仓储实现。未提供时 isUnsatisfied() 为 true，跳过执行。
     */
    @Inject
    Instance<ProjectionPositionRepository> projectionPositionRepository;

    /**
     * 默认调度入口：quarkus-quartz 每 30 秒触发一次。
     *
     * <p>cron 表达式可通过配置项 {@code ddd4j.quarkus.cqrs.projection.cron} 覆盖。
     * 业务项目也可自行提供 {@code @Scheduled} 方法 override 调用 {@link #runOnce()}。
     */
    @io.quarkus.scheduler.Scheduled(cron = "${ddd4j.quarkus.cqrs.projection.cron:*/30 * * * * ?}")
    void scheduled() {
        runOnce();
    }

    /**
     * 执行一次全量投影拉取（所有已注册视图）。
     */
    @Transactional
    public void runOnce() {
        if (projectionPositionRepository.isUnsatisfied()) {
            logger.debug("ProjectionPositionRepository not provided, skip projection");
            return;
        }
        var positions = projectionPositionRepository.get().findAll();
        if (positions.isEmpty()) {
            logger.debug("No projection positions registered, skip");
            return;
        }
        for (ProjectionPosition position : positions) {
            try {
                pullAndApply(position.getStreamId());
            } catch (Exception e) {
                logger.warnf(e, "Failed to pull and apply projection for stream '%s'", position.getStreamId());
            }
        }
    }

    /**
     * 拉取并应用单个事件流的增量事件到投影。
     *
     * <p>业务侧应 override 本方法，实现具体的 EventStore 拉取 + 读模型更新逻辑。
     * 默认实现仅记录日志。
     *
     * @param streamId 事件流 ID（通常对应聚合根类型）
     */
    protected void pullAndApply(String streamId) {
        logger.debugf("pullAndApply for stream '%s' (override me to implement actual projection)", streamId);
    }
}
