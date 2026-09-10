package io.ddd4j.quarkus.mq.testcontainers;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Semaphore;

/** 串行化占用 RocketMQ 固定宿主机端口的测试进程。 */
final class RocketMqPortLease implements AutoCloseable {
    private static final Semaphore JVM_LEASE = new Semaphore(1, true);
    private static final Path LOCK_FILE = Path.of(System.getProperty("java.io.tmpdir"), "ddd4j-quarkus-rocketmq-10911.lock");
    private final FileChannel channel;
    private final FileLock lock;
    private boolean closed;
    private RocketMqPortLease(FileChannel channel, FileLock lock) { this.channel = channel; this.lock = lock; }
    static RocketMqPortLease acquire() throws IOException, InterruptedException {
        JVM_LEASE.acquire();
        FileChannel channel = null;
        try {
            channel = FileChannel.open(LOCK_FILE, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            return new RocketMqPortLease(channel, channel.lock());
        } catch (IOException | RuntimeException failure) {
            if (channel != null) channel.close();
            JVM_LEASE.release();
            throw failure;
        }
    }
    @Override public void close() throws IOException {
        if (closed) return;
        closed = true;
        try { lock.release(); } finally {
            try { channel.close(); } finally { JVM_LEASE.release(); }
        }
    }
}
