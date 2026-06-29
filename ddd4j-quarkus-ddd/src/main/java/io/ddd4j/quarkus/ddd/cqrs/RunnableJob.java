package io.ddd4j.quarkus.ddd.cqrs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * Quartz Job 适配：执行时从 {@link RunnableHolder} 取回 {@link Runnable} 并运行。
 *
 * <p>实际任务（视图增量拉取）由 {@link io.ddd4j.core.cqrs.projection.ViewManager}
 * 在注册时提供，本类仅作为 Quartz 的可实例化 Job 承载。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public class RunnableJob implements Job {

    @Override
    public void execute(JobExecutionContext context) {
        String identity = context.getJobDetail().getJobDataMap().getString(QuarkusViewScheduler.TASK_KEY);
        Runnable task = RunnableHolder.get(identity);
        if (task != null) {
            task.run();
        }
    }
}
