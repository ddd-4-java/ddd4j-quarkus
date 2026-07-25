package io.ddd4j.quarkus.ddd.cqrs;

import io.ddd4j.core.cqrs.readmodel.ViewScheduler;
import org.jboss.logging.Logger;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Quartz 调度句柄：取消时从 Scheduler 移除 Job 并清理 {@link RunnableHolder}。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
class QuartzViewScheduleHandle implements ViewScheduler.ViewScheduleHandle {

    private static final Logger logger = Logger.getLogger(QuartzViewScheduleHandle.class);

    private final Scheduler scheduler;
    private final String identity;
    private final AtomicBoolean active = new AtomicBoolean(true);

    QuartzViewScheduleHandle(Scheduler scheduler, String identity) {
        this.scheduler = scheduler;
        this.identity = identity;
    }

    @Override
    public void cancel() {
        if (active.compareAndSet(true, false)) {
            try {
                scheduler.deleteJob(new JobKey(identity + "-job"));
            } catch (SchedulerException e) {
                logger.warnf(e, "Failed to cancel scheduled view job '%s'", identity);
            } finally {
                RunnableHolder.remove(identity);
            }
        }
    }

    @Override
    public boolean isActive() {
        return active.get();
    }
}
