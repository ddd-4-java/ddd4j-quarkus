package io.ddd4j.quarkus.mq.mqttmica;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MicaPlatformConditionTest {

    @Test
    void disablesRoundTripOnlyOnMacArm64() {
        assertThat(MicaMqttQuarkusIntegrationTest.isMacArm64("Mac OS X", "aarch64")).isTrue();
        assertThat(MicaMqttQuarkusIntegrationTest.isMacArm64("Mac OS X", "arm64")).isTrue();
        assertThat(MicaMqttQuarkusIntegrationTest.isMacArm64("Linux", "aarch64")).isFalse();
        assertThat(MicaMqttQuarkusIntegrationTest.isMacArm64("Mac OS X", "x86_64")).isFalse();
        assertThat(MicaMqttQuarkusIntegrationTest.isMacArm64(null, null)).isFalse();
    }
}
