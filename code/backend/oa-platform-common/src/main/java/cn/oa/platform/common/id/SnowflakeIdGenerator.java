package cn.oa.platform.common.id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 雪花 ID 生成器 (v2).
 *
 * <p>64 位结构: 1 位符号 + 41 位时间戳 + 5 位数据中心 + 5 位工作机器 + 12 位序列
 */
public class SnowflakeIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    private static final long EPOCH = 1288834974657L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1L;
    private static final long MAX_DATACENTER_ID = (1L << DATACENTER_ID_BITS) - 1L;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1L;
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId 范围 [0, " + MAX_WORKER_ID + "]");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId 范围 [0, " + MAX_DATACENTER_ID + "]");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            log.error("Clock moved backwards. Refusing to generate id for {}ms", lastTimestamp - timestamp);
            throw new RuntimeException("Clock moved backwards");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0L) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }
}
