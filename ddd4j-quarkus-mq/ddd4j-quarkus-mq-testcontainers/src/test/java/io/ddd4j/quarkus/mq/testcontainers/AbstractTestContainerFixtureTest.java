package io.ddd4j.quarkus.mq.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 共享 Testcontainers fixture 生命周期测试。
 */
class AbstractTestContainerFixtureTest {

    /**
     * fixture 必须停止由自身启动的容器，避免测试资源泄漏。
     */
    @Test
    void shouldStopTheContainerStartedByTheFixture() {
        RecordingContainer container = new RecordingContainer();
        TestFixture fixture = new TestFixture(container);

        fixture.start();
        fixture.stop();

        assertThat(container.startCount()).isEqualTo(1);
        assertThat(container.stopCount()).isEqualTo(1);
    }

    /**
     * fixture 不得强制开启实验性的容器复用。
     */
    @Test
    void shouldNotForceReusableContainers() {
        RecordingContainer container = new RecordingContainer();

        new TestFixture(container).start();

        assertThat(container.reuseConfigurationCount()).isZero();
    }

    private static final class TestFixture extends AbstractTestContainerFixture {

        private final RecordingContainer container;

        private TestFixture(RecordingContainer container) {
            this.container = container;
        }

        @Override
        protected GenericContainer<?> container() {
            return container;
        }

        @Override
        protected Map<String, String> exposedProperties() {
            return Map.of("fixture", "started");
        }

        @Override
        protected DockerImageName dockerImageName() {
            return DockerImageName.parse("test/fake:1");
        }
    }

    private static final class RecordingContainer extends GenericContainer<RecordingContainer> {

        private int startCount;
        private int stopCount;
        private int reuseConfigurationCount;

        private RecordingContainer() {
            super(DockerImageName.parse("test/fake:1"));
        }

        @Override
        public void start() {
            startCount++;
        }

        @Override
        public void stop() {
            stopCount++;
        }

        @Override
        public RecordingContainer withReuse(boolean reusable) {
            reuseConfigurationCount++;
            return this;
        }

        private int startCount() {
            return startCount;
        }

        private int stopCount() {
            return stopCount;
        }

        private int reuseConfigurationCount() {
            return reuseConfigurationCount;
        }
    }
}
