package io.ddd4j.quarkus.data.jpa;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL 官方容器冒烟测试：验证本模块声明的 {@code testcontainers-postgresql}
 * （Testcontainers 2.x 官方 PostgreSQL 模块）可被构建收集并完成一次真实
 * JDBC 建表 / 写入 / 读取往返。
 *
 * <p>背景：{@code ddd4j-quarkus-dependencies} 一直管理 {@code testcontainers-mysql} /
 * {@code testcontainers-postgresql} 版本，但仓内此前没有任何容器用例消费它们；
 * 本测试补齐该集成点的最小冒烟覆盖。
 *
 * <p>Docker daemon 不可达时由 {@code @Testcontainers(disabledWithoutDocker = true)}
 * 优雅跳过（不计入失败）；有 Docker 的环境（含 CI）真实启动容器执行往返。
 */
// 测试镜像来源: https://testcontainers.com/modules/postgresql/
@Testcontainers(disabledWithoutDocker = true)
class PostgresJdbcContainerSmokeTest {

    /** 钉版镜像（固定补丁标签，非浮动别名）。 */
    private static final String IMAGE = "postgres:16.15-alpine";

    // Testcontainers 2.x 的 org.testcontainers.postgresql.PostgreSQLContainer 是非泛型类
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(IMAGE)
            .withDatabaseName("ddd4j_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void jdbcRoundTripAgainstContainer() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE smoke (id INT PRIMARY KEY, val VARCHAR(32) NOT NULL)");
            int inserted = statement.executeUpdate("INSERT INTO smoke (id, val) VALUES (1, 'pong')");
            assertEquals(1, inserted);
            try (ResultSet resultSet = statement.executeQuery("SELECT val FROM smoke WHERE id = 1")) {
                assertTrue(resultSet.next(), "应读取到刚写入的行");
                assertEquals("pong", resultSet.getString("val"));
            }
        }
    }
}
