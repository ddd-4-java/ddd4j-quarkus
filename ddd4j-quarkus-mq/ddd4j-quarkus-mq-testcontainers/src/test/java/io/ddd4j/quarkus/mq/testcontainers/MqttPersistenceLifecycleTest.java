package io.ddd4j.quarkus.mq.testcontainers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 使用真实 MQTT 生命周期，仅替换 Docker 进程边界，验证全局目录属性的恢复。 */
@ResourceLock("java.lang.System.properties")
class MqttPersistenceLifecycleTest {
    @TempDir
    Path directory;

    @Test
    void shouldKeepOnePersistenceDirectoryAcrossRepeatedStartsAndRestoreIt() {
        withUserDir(() -> {
            TestMqttFixture fixture = new TestMqttFixture();
            try {
                fixture.start();
                fixture.start();
                assertThat(System.getProperty("user.dir")).isEqualTo(persistenceDirectory());
                assertThat(fixture.recording.starts).isEqualTo(1);
            } finally {
                fixture.stop();
            }
            fixture.stop();
            assertThat(System.getProperty("user.dir")).isEqualTo(directory.toString());
            fixture.start();
            try {
                assertThat(System.getProperty("user.dir")).isEqualTo(persistenceDirectory());
            } finally {
                fixture.stop();
            }
            assertThat(System.getProperty("user.dir")).isEqualTo(directory.toString());
        });
    }

    @Test
    void shouldRestoreDirectoryAndCleanFailedStartBeforeRetry() {
        withUserDir(() -> {
            TestMqttFixture fixture = new TestMqttFixture();
            fixture.recording.failStart = true;
            assertThatThrownBy(fixture::start).isInstanceOf(IllegalStateException.class);
            assertThat(System.getProperty("user.dir")).isEqualTo(directory.toString());
            assertThat(fixture.recording.stops).isEqualTo(1);
            fixture.recording.failStart = false;
            fixture.start();
            try {
                assertThat(fixture.recording.directoryAtStart).isEqualTo(persistenceDirectory());
            } finally {
                fixture.stop();
            }
        });
    }

    @Test
    void shouldRestoreDirectoryWhenPropertiesFail() {
        withUserDir(() -> {
            TestMqttFixture fixture = new TestMqttFixture();
            fixture.failProperties = true;
            assertThatThrownBy(fixture::start).isInstanceOf(IllegalStateException.class);
            assertThat(System.getProperty("user.dir")).isEqualTo(directory.toString());
            assertThat(fixture.recording.stops).isEqualTo(1);
        });
    }

    @Test
    void shouldRetryFailedCleanupBeforeInstallingTheNextDirectoryOverride() {
        withUserDir(() -> {
            TestMqttFixture fixture = new TestMqttFixture();
            fixture.start();
            fixture.recording.failStop = true;
            assertThatThrownBy(fixture::stop).isInstanceOf(IllegalStateException.class);
            assertThat(System.getProperty("user.dir")).isEqualTo(directory.toString());
            assertThatThrownBy(fixture::start).isInstanceOf(IllegalStateException.class);
            assertThat(fixture.recording.starts).isEqualTo(1);
            assertThat(System.getProperty("user.dir")).isEqualTo(directory.toString());
            fixture.recording.failStop = false;
            fixture.start();
            try {
                assertThat(fixture.recording.directoryAtStart).isEqualTo(persistenceDirectory());
                assertThat(System.getProperty("user.dir")).isEqualTo(persistenceDirectory());
            } finally {
                fixture.stop();
            }
            assertThat(System.getProperty("user.dir")).isEqualTo(directory.toString());
        });
    }

    @Test
    void shouldRestoreDirectoryIfPersistenceDirectoryCannotBeCreated() throws Exception {
        Files.createFile(directory.resolve("target"));
        withUserDir(() -> {
            TestMqttFixture fixture = new TestMqttFixture();
            assertThatThrownBy(fixture::start).isInstanceOf(java.io.UncheckedIOException.class);
            assertThat(System.getProperty("user.dir")).isEqualTo(directory.toString());
            assertThat(fixture.recording.starts).isZero();
        });
    }

    private String persistenceDirectory() {
        return directory.resolve("target/mqtt-persistence").toString();
    }

    private void withUserDir(Runnable assertions) {
        String original = System.getProperty("user.dir");
        System.setProperty("user.dir", directory.toString());
        try {
            assertions.run();
        } finally {
            System.setProperty("user.dir", original);
        }
    }

    private static final class TestMqttFixture extends MqttQuarkusTestResource {
        private final RecordingContainer recording = new RecordingContainer();
        private boolean failProperties;

        @Override
        protected GenericContainer<?> container() {
            return recording;
        }

        @Override
        protected Map<String, String> exposedProperties() {
            if (failProperties) {
                throw new IllegalStateException("properties failed");
            }
            return Map.of("ddd4j.mq.broker", "MQTT");
        }
    }

    private static final class RecordingContainer extends GenericContainer<RecordingContainer> {
        private int starts;
        private int stops;
        private boolean failStart;
        private boolean failStop;
        private String directoryAtStart;

        private RecordingContainer() {
            super(DockerImageName.parse("test/fake:1"));
        }

        @Override
        public void start() {
            starts++;
            directoryAtStart = System.getProperty("user.dir");
            if (failStart) {
                throw new IllegalStateException("start failed");
            }
        }

        @Override
        public void stop() {
            stops++;
            if (failStop) {
                throw new IllegalStateException("stop failed");
            }
        }
    }
}
