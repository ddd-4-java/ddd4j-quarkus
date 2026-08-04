package io.ddd4j.quarkus.ddd.cqrs;

import io.ddd4j.core.cqrs.readmodel.ViewScheduler;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link QuarkusViewScheduler} 集成测试：基于 {@code quarkus-quartz} 的真实 {@link org.quartz.Scheduler}，
 * 验证动态 CRON 调度的完整生命周期：
 *
 * <ul>
 *   <li>{@code schedule(viewName, cron, task)} 注册成功并返回活跃句柄</li>
 *   <li>任务实际被 Quartz 触发执行（每秒一次的 CRON，{@link RunnableJob} + {@link RunnableHolder} 链路）</li>
 *   <li>{@code handle.cancel()} 取消后 {@code isActive()} 为 false，重复取消幂等</li>
 * </ul>
 */
@QuarkusTest
class QuarkusViewSchedulerTest {

    @Inject
    QuarkusViewScheduler viewScheduler;

    @Test
    void schedule_registersCronJobAndRunsTask() throws InterruptedException {
        CountDownLatch fired = new CountDownLatch(1);

        ViewScheduler.ViewScheduleHandle handle =
                viewScheduler.schedule("view-test", "* * * * * ?", fired::countDown);

        try {
            assertThat(handle).isNotNull();
            assertThat(handle.isActive()).as("刚注册的调度句柄应为活跃状态").isTrue();
            // 每秒触发一次，5 秒内应至少执行一次（验证 RunnableJob/RunnableHolder 取回任务）
            assertThat(fired.await(5, TimeUnit.SECONDS))
                    .as("CRON 任务应在超时前被 Quartz 触发执行").isTrue();
        } finally {
            handle.cancel();
        }
    }

    @Test
    void cancel_deactivatesHandleAndIsIdempotent() {
        ViewScheduler.ViewScheduleHandle handle =
                viewScheduler.schedule("view-cancel", "0 0 0 1 1 ? 2099", () -> {
                });

        assertThat(handle.isActive()).isTrue();

        handle.cancel();
        assertThat(handle.isActive()).as("cancel 后句柄应失活").isFalse();

        // 重复 cancel 不应抛异常（QuartzViewScheduleHandle 内部 compareAndSet 幂等）
        handle.cancel();
        assertThat(handle.isActive()).isFalse();
    }

    @Test
    void multipleSchedulesCreateIndependentHandles() {
        ViewScheduler.ViewScheduleHandle first = viewScheduler.schedule("view-a", "0 0 0 1 1 ? 2099", () -> {
        });
        ViewScheduler.ViewScheduleHandle second = viewScheduler.schedule("view-b", "0 0 0 1 1 ? 2099", () -> {
        });

        assertThat(first.isActive()).isTrue();
        assertThat(second.isActive()).isTrue();

        first.cancel();
        assertThat(first.isActive()).isFalse();
        assertThat(second.isActive()).as("取消一个调度不应影响其他调度").isTrue();

        second.cancel();
    }
}
