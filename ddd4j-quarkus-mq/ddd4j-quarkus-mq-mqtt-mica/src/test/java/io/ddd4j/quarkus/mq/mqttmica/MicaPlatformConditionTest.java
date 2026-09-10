package io.ddd4j.quarkus.mq.mqttmica;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MicaPlatformConditionTest {

    @Test
    void disablesRoundTripOnlyOnMacArm64() {
        String originalOs = System.getProperty("os.name");
        String originalArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.name", "Mac OS X");
            System.setProperty("os.arch", "aarch64");
            assertThat(MicaMqttQuarkusIntegrationTest.isMacArm64()).isTrue();

            System.setProperty("os.name", "Linux");
            assertThat(MicaMqttQuarkusIntegrationTest.isMacArm64()).isFalse();

            System.setProperty("os.name", "Mac OS X");
            System.setProperty("os.arch", "x86_64");
            assertThat(MicaMqttQuarkusIntegrationTest.isMacArm64()).isFalse();
        } finally {
            restore("os.name", originalOs);
            restore("os.arch", originalArch);
        }
    }

    private static void restore(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
