package io.ddd4j.quarkus.mq.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQS（LocalStack）fixture 的 Docker 真实启动契约测试。
 *
 * <p>与 {@link NatsQuarkusTestResourceTest} 同款风格：fixture 自持容器生命周期
 * （{@code container()} 由 fixture 在 {@code start()} 内创建并拥有），静态
 * {@code @Container} 会造成双所有权，故只用 {@code @Testcontainers(disabledWithoutDocker = true)}
 * 做 Docker 门禁 + {@code start/stop} 手动编排；无 Docker 时整体跳过，不伪报通过。
 *
 * <p>补齐的覆盖缺口：本模块此前只有 NATS fixture 有 Docker 真实启动用例
 * （其余 fixture 契约均为 {@link AbstractTestContainerFixtureTest} 的
 * {@code RecordingContainer} 单元级验证，或依赖各 broker 模块的
 * {@code @QuarkusTest} 间接覆盖）。本测试直接验证 SQS fixture 的
 * {@code exposedProperties} 键契约 + endpoint 真实可达性 + 生命周期可重启。
 */
// 测试镜像来源: https://testcontainers.com/modules/localstack/
// （容器由 SqsQuarkusTestResource fixture 持有：localstack/localstack:3.8.0，仅 sqs 服务）
@Testcontainers(disabledWithoutDocker = true)
class SqsQuarkusTestResourceContractTest {

    /** fixture 暴露给 Quarkus config 的键（与业务模块读取的键必须一致）。 */
    private static final String ENDPOINT_KEY = "ddd4j.mq.sqs.endpoint";
    private static final String BROKER_KEY = "ddd4j.mq.broker";

    @Test
    void shouldExposeReachableLocalStackSqsEndpointContract() throws Exception {
        SqsQuarkusTestResource fixture = new SqsQuarkusTestResource();
        try {
            Map<String, String> properties = fixture.start();

            // 键契约：业务模块（ddd4j-quarkus-mq-sqs 的 IT）按这两个键读取连接信息
            assertThat(properties).containsKeys(ENDPOINT_KEY, BROKER_KEY);
            assertThat(properties.get(BROKER_KEY)).isEqualTo("SQS");

            // 值契约：endpoint 必须是 http 回环可达地址（禁止只注入字符串不验证连通性）
            String endpoint = properties.get(ENDPOINT_KEY);
            assertThat(endpoint).startsWith("http://");
            URI uri = URI.create(endpoint);
            assertThat(uri.getPort()).isPositive();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(uri.getHost(), uri.getPort()), 10_000);
                assertThat(socket.isConnected()).isTrue();
            }
        } finally {
            fixture.stop();
        }
    }

    @Test
    void shouldRestartCleanlyAfterStopWithUnstartedStopBeingIdempotent() throws Exception {
        SqsQuarkusTestResource fixture = new SqsQuarkusTestResource();
        try {
            // 未启动时 stop 幂等（清理路径不得抛错）
            fixture.stop();

            Map<String, String> first = fixture.start();
            fixture.stop();

            // 停止后可再次启动（替换容器、重新暴露端点），键契约保持一致
            Map<String, String> second = fixture.start();
            assertThat(second).containsKeys(ENDPOINT_KEY, BROKER_KEY);
            assertThat(second.get(BROKER_KEY)).isEqualTo("SQS");
            assertThat(second.get(ENDPOINT_KEY)).startsWith("http://");
            // 动态端口分配，重启后端点允许不同，但必须仍然可达
            URI uri = URI.create(second.get(ENDPOINT_KEY));
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(uri.getHost(), uri.getPort()), 10_000);
                assertThat(socket.isConnected()).isTrue();
            }
            assertThat(first).containsKey(ENDPOINT_KEY);
        } finally {
            fixture.stop();
        }
    }
}
