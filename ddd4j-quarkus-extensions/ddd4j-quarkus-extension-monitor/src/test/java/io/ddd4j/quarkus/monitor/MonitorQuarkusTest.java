package io.ddd4j.quarkus.monitor;

import io.ddd4j.extension.monitor.channel.dingtalk.DingTalkRobotSender;
import io.ddd4j.extension.monitor.channel.feishu.FeishuRobotSender;
import io.ddd4j.extension.monitor.channel.wecom.WeComRobotSender;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link Ddd4jMonitorCdiProducer} Quarkus 集成测试：验证监控配置与
 * 三个机器人发送器的 CDI 装配（不实际发送消息）。
 */
@QuarkusTest
class MonitorQuarkusTest {

    @Inject
    MonitorConfig monitorConfig;

    @Inject
    DingTalkRobotSender dingTalkRobotSender;

    @Inject
    WeComRobotSender weComRobotSender;

    @Inject
    FeishuRobotSender feishuRobotSender;

    @Test
    void beansShouldBeInjectable() {
        assertNotNull(monitorConfig);
        assertNotNull(dingTalkRobotSender);
        assertNotNull(weComRobotSender);
        assertNotNull(feishuRobotSender);
    }

    @Test
    void configShouldExposeEnabledDefaults() {
        // @IfBuildProperty 默认启用（enableIfMissing=true）
        assertNotNull(monitorConfig.log());
    }
}
