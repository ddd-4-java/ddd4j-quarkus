package io.ddd4j.quarkus.ddd.cqrs;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Runnable 暂存容器：Quartz 的 JobDataMap 只支持可序列化的基本类型，
 * 无法直接携带任意 {@link Runnable}，故用静态 Map 暂存，Job 执行时按 identity 取回。
 *
 * <p>调度句柄 {@link QuartzViewScheduleHandle#cancel()} 在取消时移除条目，避免内存泄漏。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
final class RunnableHolder {

    private static final ConcurrentMap<String, Runnable> TASKS = new ConcurrentHashMap<>();

    private RunnableHolder() {
    }

    static void put(String identity, Runnable task) {
        TASKS.put(identity, task);
    }

    static Runnable get(String identity) {
        return TASKS.get(identity);
    }

    static void remove(String identity) {
        TASKS.remove(identity);
    }
}
