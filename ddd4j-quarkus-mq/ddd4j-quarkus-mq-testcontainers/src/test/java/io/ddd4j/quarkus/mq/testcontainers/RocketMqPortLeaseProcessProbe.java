package io.ddd4j.quarkus.mq.testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;

/** 子 JVM 探针，用于验证 RocketMQ 文件租约的真实跨进程行为。 */
public final class RocketMqPortLeaseProcessProbe {

    private RocketMqPortLeaseProcessProbe() {
    }

    /**
     * 探针入口：按模式尝试获取 RocketMQ 端口文件租约，并把尝试/获得结果写入指定标记文件。
     *
     * @param args [模式, 尝试标记文件, 获得标记文件, 持有时长毫秒]
     * @throws Exception 文件读写或租约操作异常
     */
    public static void main(String[] args) throws Exception {
        String mode = args[0];
        Path attemptedFile = Path.of(args[1]);
        Path acquiredFile = Path.of(args[2]);
        long holdMillis = Long.parseLong(args[3]);
        Files.writeString(attemptedFile, "attempted");
        if ("fail".equals(mode)) {
            RocketMqPortLease.acquire();
            Files.writeString(acquiredFile, "acquired");
            throw new IllegalStateException("intentional holder failure");
        }
        try (RocketMqPortLease ignored = RocketMqPortLease.acquire()) {
            Files.writeString(acquiredFile, "acquired");
            if ("hold".equals(mode)) {
                Thread.sleep(holdMillis);
            }
        }
    }
}
