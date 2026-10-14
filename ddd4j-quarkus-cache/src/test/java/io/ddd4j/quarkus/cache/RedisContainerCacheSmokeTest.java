package io.ddd4j.quarkus.cache;

import io.ddd4j.cache.lettuce.LettuceCache;
import io.ddd4j.core.cache.Cache;
import io.ddd4j.core.cache.CacheConfig;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 容器集成测试：缓存 CRUD 冒烟。
 *
 * <p>参照 Testcontainers 官方模块清单（https://testcontainers.com/modules/ ）的 Redis 模块
 * 与官方镜像 {@code redis:7.4-alpine}，先用 Lettuce 原生命令验证容器连通，再经
 * {@link LettuceCache}（ddd4j 缓存门面）完成 put/getIfPresent/invalidate 冒烟；
 * 无 Docker 环境时由 {@code @Testcontainers(disabledWithoutDocker = true)} 自动跳过。</p>
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisContainerCacheSmokeTest {

    /** Redis 容器（JUnit 5 扩展在测试类生命周期内启动/停止）。 */
    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
                    .withExposedPorts(6379);

    /**
     * 验证容器连通且 ddd4j 缓存门面完成键值读写与失效冒烟。
     */
    @Test
    void shouldRoundTripThroughLettuceCache() {
        String url = "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379);
        RedisClient client = RedisClient.create(url);
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisCommands<String, String> commands = connection.sync();

            // 容器连通性：原生 SET/GET 冒烟
            commands.set("smoke:key", "hello-redis");
            assertThat(commands.get("smoke:key")).isEqualTo("hello-redis");

            // ddd4j 缓存门面：写入 → 命中 → 失效 → 未命中
            CacheConfig config = CacheConfig.builder("smoke-cache")
                    .expireAfterWriteSeconds(60L)
                    .build();
            Cache<String, String> cache = new LettuceCache<>(commands, config, String.class);

            cache.put("order:1", "created");
            assertThat(cache.getIfPresent("order:1")).isEqualTo("created");

            cache.invalidate("order:1");
            assertThat(cache.getIfPresent("order:1")).isNull();

            // 不存在的键返回 null（读穿语义不产生副作用）
            assertThat(cache.getIfPresent("missing:key")).isNull();
            assertThat(commands.get("missing:key")).isNull();
        } finally {
            client.shutdown();
        }
    }
}
