package io.ddd4j.quarkus.mq.testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;

/** 子 JVM 探针，用于验证 RocketMQ 文件租约的真实跨进程行为。 */
public final class RocketMqPortLeaseProcessProbe {

    private RocketMqPortLeaseProcessProbe() {
    }

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
