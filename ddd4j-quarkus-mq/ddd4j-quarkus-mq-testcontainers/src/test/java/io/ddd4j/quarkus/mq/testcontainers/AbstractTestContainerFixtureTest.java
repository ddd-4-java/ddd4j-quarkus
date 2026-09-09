package io.ddd4j.quarkus.mq.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** 验证共享夹具的所有权、失败回收和专用容器适配契约。 */
class AbstractTestContainerFixtureTest {

    @Test
    void shouldStartAndStopOwnedContainerExactlyOnce() {
        RecordingContainer container = new RecordingContainer();
        TestFixture fixture = new TestFixture(container);
        assertThat(fixture.start()).containsEntry("fixture", "started");
        fixture.start();
        fixture.stop();
        fixture.stop();
        assertThat(container.starts).isEqualTo(1);
        assertThat(container.stops).isEqualTo(1);
    }

    @Test
    void shouldNotForceReuseOrInjectReuseProperty() {
        RecordingContainer container = new RecordingContainer();
        assertThat(new TestFixture(container).start()).doesNotContainKey("ddd4j.testcontainers.reuse");
        assertThat(container.reuseRequests).isZero();
    }

    @Test
    void shouldCleanPartialStartupAndAllowRetry() {
        RecordingContainer container = new RecordingContainer();
        container.failStart = true;
        TestFixture fixture = new TestFixture(container);
        assertThatThrownBy(fixture::start).isSameAs(container.startFailure);
        assertThat(container.stops).isEqualTo(1);
        container.failStart = false;
        fixture.start();
        fixture.stop();
        assertThat(container.starts).isEqualTo(2);
        assertThat(container.stops).isEqualTo(2);
    }

    @Test
    void shouldCleanContainerWhenPropertyExtractionFails() {
        RecordingContainer container = new RecordingContainer();
        TestFixture fixture = new TestFixture(container);
        fixture.failProperties = true;
        assertThatThrownBy(fixture::start).isInstanceOf(IllegalStateException.class);
        assertThat(container.stops).isEqualTo(1);
    }

    @Test
    void shouldRetryCleanupBeforeReplacingAnOwnedContainer() {
        RecordingContainer container = new RecordingContainer();
        TestFixture fixture = new TestFixture(container);
        fixture.start();
        container.failStop = true;
        assertThatThrownBy(fixture::stop).isSameAs(container.stopFailure);
        assertThatThrownBy(fixture::start).isSameAs(container.stopFailure);
        assertThat(container.starts).isEqualTo(1);
        assertThat(container.stops).isEqualTo(2);

        container.failStop = false;
        fixture.start();
        fixture.stop();
        assertThat(container.starts).isEqualTo(2);
        assertThat(container.stops).isEqualTo(4);
    }

    @Test
    void shouldPreserveStartupFailureWhenCleanupAlsoFails() {
        RecordingContainer container = new RecordingContainer();
        container.failStart = true;
        container.failStop = true;
        assertThatThrownBy(new TestFixture(container)::start)
                .isSameAs(container.startFailure).hasSuppressedException(container.stopFailure);
    }

    @Test
    void shouldUseDedicatedArtemisAdapter() {
        assertThat(new ActiveMqQuarkusTestResource().container()).isInstanceOf(ArtemisContainer.class);
    }

    @Test
    void shouldUseDedicatedKafkaAdapter() {
        assertThat(new KafkaQuarkusTestResource().container()).isInstanceOf(ConfluentKafkaContainer.class);
    }

    @Test
    void shouldUseDedicatedPulsarAdapter() {
        assertThat(new PulsarQuarkusTestResource().container()).isInstanceOf(PulsarContainer.class);
    }

    @Test
    void shouldUseDedicatedRabbitMqAdapter() {
        assertThat(new RabbitMqQuarkusTestResource().container()).isInstanceOf(RabbitMQContainer.class);
    }

    @Test
    void shouldUseDedicatedLocalStackAdapter() {
        // LocalStack 2.0.5 构造器会解析远端 Docker socket；仅此适配器检查依赖 Docker。
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "LocalStack 2.0.5 constructor requires Docker socket discovery");
        assertThat(new SqsQuarkusTestResource().container()).isInstanceOf(LocalStackContainer.class);
    }

    @Test
    void shouldWaitForBrokerReadinessBeyondOpenPorts() {
        for (AbstractTestContainerFixture fixture : new AbstractTestContainerFixture[] {
                new NatsQuarkusTestResource(), new MqttQuarkusTestResource(),
                new MicaMqttQuarkusTestResource(), new RedisStreamQuarkusTestResource(),
                new OnsQuarkusTestResource(), new TdmqQuarkusTestResource()}) {
            assertThat(fixture.waitStrategy()).as(fixture.getClass().getSimpleName())
                    .isNotNull().isNotInstanceOf(HostPortWaitStrategy.class);
        }
    }

    @Test
    void shouldWaitForBothRocketMqProcesses() throws Exception {
        WaitStrategy strategy = new RocketMqQuarkusTestResource().waitStrategy();
        assertThat(strategy).isInstanceOf(WaitAllStrategy.class);
        List<?> children = (List<?>) strategyField(strategy, WaitAllStrategy.class, "strategies");
        assertThat(children).hasSize(2);
        String nameserverReady = "The Name Server boot success. serializeType=JSON\n";
        String brokerReady = "The broker[broker-a, 127.0.0.1:10911] boot success.\n";
        int nameserverConditions = 0;
        int brokerConditions = 0;
        for (Object child : children) {
            assertThat(child).isInstanceOf(LogMessageWaitStrategy.class);
            Pattern condition = Pattern.compile((String) strategyField(child,
                    LogMessageWaitStrategy.class, "regEx"), Pattern.DOTALL);
            boolean acceptsNameserver = condition.matcher(nameserverReady).matches();
            boolean acceptsBroker = condition.matcher(brokerReady).matches();
            // 每个子条件必须只接受对应进程的 ready 日志，不能拿另一进程或无关日志充数。
            assertThat(acceptsNameserver ^ acceptsBroker).isTrue();
            assertThat(condition.matcher("port 9876 listening\n").matches()).isFalse();
            assertThat(strategyField(child, LogMessageWaitStrategy.class, "times")).isEqualTo(1);
            nameserverConditions += acceptsNameserver ? 1 : 0;
            brokerConditions += acceptsBroker ? 1 : 0;
        }
        assertThat(nameserverConditions).isEqualTo(1);
        assertThat(brokerConditions).isEqualTo(1);
    }

    @Test
    void shouldBoundTheWholeRocketMqReadinessSequenceToThreeMinutes() throws Exception {
        WaitStrategy strategy = new RocketMqQuarkusTestResource().waitStrategy();
        assertThat(strategyField(strategy, WaitAllStrategy.class, "mode"))
                .isEqualTo(WaitAllStrategy.Mode.WITH_OUTER_TIMEOUT);
        assertThat(strategyField(strategy, WaitAllStrategy.class, "timeout"))
                .isEqualTo(Duration.ofMinutes(3));
    }

    /** 2.0.5 未公开组合等待配置 getter；只读实际配置，避免等待三分钟或启动额外 Docker 进程。 */
    private static Object strategyField(Object strategy, Class<?> declaringClass, String name) throws Exception {
        Field field = declaringClass.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(strategy);
    }

    /** 使用真实生命周期代码，仅替换 Docker 进程边界。 */
    private static final class TestFixture extends AbstractTestContainerFixture {
        private final RecordingContainer container;
        private boolean failProperties;

        private TestFixture(RecordingContainer container) {
            this.container = container;
        }

        @Override
        protected GenericContainer<?> container() {
            return container;
        }

        @Override
        protected Map<String, String> exposedProperties() {
            if (failProperties) {
                throw new IllegalStateException("properties failed");
            }
            return Map.of("fixture", "started");
        }

        @Override
        protected DockerImageName dockerImageName() {
            return DockerImageName.parse("test/fake:1");
        }
    }

    /** 无 Docker 的容器边界记录器，保留明确的启动和停止失败。 */
    private static final class RecordingContainer extends GenericContainer<RecordingContainer> {
        private int starts;
        private int stops;
        private int reuseRequests;
        private boolean failStart;
        private boolean failStop;
        private final RuntimeException startFailure = new IllegalStateException("start failed");
        private final RuntimeException stopFailure = new IllegalStateException("stop failed");

        private RecordingContainer() {
            super(DockerImageName.parse("test/fake:1"));
        }

        @Override
        public void start() {
            starts++;
            if (failStart) {
                throw startFailure;
            }
        }

        @Override
        public void stop() {
            stops++;
            if (failStop) {
                throw stopFailure;
            }
        }

        @Override
        public RecordingContainer withReuse(boolean reusable) {
            reuseRequests++;
            return this;
        }
    }
}
