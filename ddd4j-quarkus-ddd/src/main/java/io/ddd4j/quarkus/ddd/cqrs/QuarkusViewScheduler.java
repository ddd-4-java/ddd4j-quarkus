package io.ddd4j.quarkus.ddd.cqrs;

import io.ddd4j.core.cqrs.readmodel.ViewScheduler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Quarkus CQRS 视图调度器：基于 {@code quarkus-quartz} 的 {@link Scheduler} 实现动态 CRON 调度。
 *
 * <p>实现 ddd4j-core 的 {@link ViewScheduler} SPI，对标 ddd4j-runtime-spring 的 {@code SpringViewScheduler}。
 * 与 Quarkus 声明式 {@code @Scheduled}（编译期固定 cron）不同，本实现支持运行时动态注册，
 * 满足 {@link io.ddd4j.core.cqrs.projection.ViewManager} 按视图名注册不同 cron 的需求。
 *
 * <p>调度任务通过 Quartz {@link Job} 封装，用 {@code JobDataMap} 携带实际 {@link Runnable}。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@ApplicationScoped
public class QuarkusViewScheduler implements ViewScheduler {

    /**
     * JobDataMap 中携带 Runnable 的键。
     */
    static final String TASK_KEY = "ddd4j.task";
    private static final Logger logger = Logger.getLogger(QuarkusViewScheduler.class);
    @Inject
    Scheduler scheduler;

    @Override
    public ViewScheduleHandle schedule(String viewName, String cron, Runnable task) {
        String identity = viewName + "-" + UUID.randomUUID();
        try {
            JobDetail job = JobBuilder.newJob(RunnableJob.class)
                    .withIdentity(identity + "-job")
                    .usingJobData(TASK_KEY, identity)
                    .storeDurably()
                    .build();
            // 将 Runnable 存入 Scheduler 的 JobDataMap（通过 JobDetail.storeDurably + 运行时注入）
            // Quartz 的 JobDataMap 不支持存任意对象，故用 RunnableHolder 静态持有
            RunnableHolder.put(identity, task);

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(identity + "-trigger")
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                    .build();

            scheduler.scheduleJob(job, trigger);
            logger.infof("Scheduled view '%s' with cron '%s'", viewName, cron);
            return new QuartzViewScheduleHandle(scheduler, identity);
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to schedule view '" + viewName + "' with cron '" + cron + "'", e);
        }
    }
}
