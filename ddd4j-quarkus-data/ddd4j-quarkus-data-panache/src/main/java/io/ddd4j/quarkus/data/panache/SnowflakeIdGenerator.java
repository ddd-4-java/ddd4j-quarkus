package io.ddd4j.quarkus.data.panache;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Enumeration;

/**
 * 类雪花算法的 Hibernate 主键生成器：时间戳 + 节点 ID + 毫秒内序列，保证单机内递增、多节点可区分。
 * <p>
 * 对标 cloud-das 的 {@code SequenceGenerator} + {@code IdGenerator} 组合，
 * 作为 Quarkus Panache 实体的 {@code @GenericGenerator} 策略使用。
 * </p>
 */
public class SnowflakeIdGenerator implements IdentifierGenerator {

    private static final int NODE_ID_BITS = 2;
    private static final int SEQUENCE_BITS = 4;
    private static final int MAX_NODE_ID = (int) (Math.pow(2, NODE_ID_BITS) - 1);
    private static final int MAX_SEQUENCE = (int) (Math.pow(2, SEQUENCE_BITS) - 1);
    private static final long CUSTOM_EPOCH = 1640966400000L;

    private static final SnowflakeIdGenerator INSTANCE = new SnowflakeIdGenerator();

    private final int nodeId;
    private volatile long lastTimestamp = -1L;
    private volatile long sequence = 0L;

    public SnowflakeIdGenerator() {
        this.nodeId = createNodeId();
    }

    /**
     * 静态入口：生成下一个全局唯一长整型 ID。
     *
     * @return 下一个 ID
     */
    public static long nextId() {
        return INSTANCE.generateId();
    }

    private static long timestamp() {
        return Instant.now().toEpochMilli() - CUSTOM_EPOCH;
    }

    private static int createNodeId() {
        int nodeId;
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                byte[] mac = networkInterface.getHardwareAddress();
                if (mac != null) {
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b));
                    }
                }
            }
            nodeId = sb.toString().hashCode();
        } catch (Exception ex) {
            nodeId = new SecureRandom().nextInt();
        }
        return nodeId & MAX_NODE_ID;
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        return generateId();
    }

    private synchronized long generateId() {
        long currentTimestamp = timestamp();
        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Invalid System Clock!");
        }
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = currentTimestamp;
        long id = currentTimestamp << (NODE_ID_BITS + SEQUENCE_BITS);
        id |= ((long) nodeId << SEQUENCE_BITS);
        id |= sequence;
        return id;
    }

    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp == lastTimestamp) {
            currentTimestamp = timestamp();
        }
        return currentTimestamp;
    }
}
