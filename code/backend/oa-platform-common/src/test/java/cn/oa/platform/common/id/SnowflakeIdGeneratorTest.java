package cn.oa.platform.common.id;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SnowflakeIdGeneratorTest {

    @Test
    void shouldGenerateUniqueIds() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1, 1);
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(gen.nextId());
        }
        assertThat(ids).hasSize(1000);
    }

    @Test
    void shouldGenerateIdsInConcurrentEnvironment() throws InterruptedException {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(2, 1);
        Set<Long> ids = new HashSet<>();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                for (int j = 0; j < 1000; j++) {
                    synchronized (ids) {
                        ids.add(gen.nextId());
                    }
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(ids).hasSize(10_000);
    }

    @Test
    void shouldRejectInvalidWorkerId() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(100, 1));
    }
}
