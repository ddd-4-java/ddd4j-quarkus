package io.ddd4j.quarkus.data.jpa;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL 容器集成测试：建表 + CRUD 冒烟。
 *
 * <p>使用 Testcontainers 官方 PostgreSQL 模块（org.testcontainers:postgresql）与官方镜像
 * {@code postgres:16-alpine}，通过 JDBC 直连容器完成 建表 → 插入 → 查询 → 更新 → 删除
 * 全链路冒烟；无 Docker 环境时由 {@code @Testcontainers(disabledWithoutDocker = true)}
 * 自动跳过，不会造成假失败。</p>
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgresContainerCrudSmokeTest {

    /** PostgreSQL 容器（JUnit 5 扩展在测试类生命周期内启动/停止）。 */
    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    /**
     * 验证容器内建表与增删改查冒烟全通过。
     *
     * @throws Exception JDBC 连接或 SQL 执行失败
     */
    @Test
    void shouldCreateTableAndCrudRoundTrip() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {

            // 建表：订单冒烟表（主键 + 非空约束）
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP TABLE IF EXISTS t_smoke_order");
                statement.execute("CREATE TABLE t_smoke_order ("
                        + "order_no VARCHAR(32) PRIMARY KEY, "
                        + "buyer_name VARCHAR(64) NOT NULL, "
                        + "amount NUMERIC(18,2) NOT NULL)");
            }

            // 插入两条记录
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO t_smoke_order(order_no, buyer_name, amount) VALUES (?, ?, ?)")) {
                insert.setString(1, "SO-001");
                insert.setString(2, "buyer-a");
                insert.setBigDecimal(3, new java.math.BigDecimal("100.50"));
                assertThat(insert.executeUpdate()).isEqualTo(1);

                insert.setString(1, "SO-002");
                insert.setString(2, "buyer-b");
                insert.setBigDecimal(3, new java.math.BigDecimal("200.00"));
                assertThat(insert.executeUpdate()).isEqualTo(1);
            }

            // 查询：行数与主键命中
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM t_smoke_order")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong(1)).isEqualTo(2L);
            }

            // 更新：金额改写后回读校验
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE t_smoke_order SET amount = ? WHERE order_no = 'SO-001'")) {
                update.setBigDecimal(1, new java.math.BigDecimal("999.99"));
                assertThat(update.executeUpdate()).isEqualTo(1);
            }
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT amount FROM t_smoke_order WHERE order_no = 'SO-001'");
                 ResultSet rs = query.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getBigDecimal(1)).isEqualByComparingTo("999.99");
            }

            // 删除：单行删除后剩余行数校验
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM t_smoke_order WHERE order_no = 'SO-002'")) {
                assertThat(delete.executeUpdate()).isEqualTo(1);
            }
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT order_no FROM t_smoke_order ORDER BY order_no")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("SO-001");
                assertThat(rs.next()).isFalse();
            }
        }
    }
}
