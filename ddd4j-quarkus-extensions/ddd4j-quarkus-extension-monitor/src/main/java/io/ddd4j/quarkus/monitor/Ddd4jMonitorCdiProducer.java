package io.ddd4j.quarkus.monitor;

import io.ddd4j.extension.monitor.Sender;
import io.ddd4j.extension.monitor.channel.dingtalk.DingTalkProperties;
import io.ddd4j.extension.monitor.channel.dingtalk.DingTalkRobotSender;
import io.ddd4j.extension.monitor.channel.feishu.FeishuProperties;
import io.ddd4j.extension.monitor.channel.feishu.FeishuRobotSender;
import io.ddd4j.extension.monitor.channel.wecom.WeComProperties;
import io.ddd4j.extension.monitor.channel.wecom.WeComRobotSender;
import io.ddd4j.extension.monitor.config.BaseMonitorProperties;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * ddd4j-quarkus monitor CDI 装配（对应 boot 的 {@code Ddd4jMonitorBootAutoConfiguration}）。
 *
 * <p>boot 版通过 {@code @AutoConfiguration + @ConditionalOnClass(LoggerContext.class)}
 * 仅暴露 {@link BaseMonitorProperties} Bean（其 Logback 守卫在 Quarkus 下无意义 —— Quarkus
 * 使用 JBoss LogManager）；Quarkus 版等价物为：
 * <ul>
 *   <li>CDI 暴露 {@link BaseMonitorProperties} 配置模型（由 {@link MonitorConfig} 填充，
 *       保持库侧 POJO 契约不变）</li>
 *   <li>复用主仓纯 Java 机器人发送器实现（{@link DingTalkRobotSender} / {@link WeComRobotSender}
 *       / {@link FeishuRobotSender}），按各通道 enable 开关条件装配为 {@link Sender} 单例</li>
 *   <li>{@code @IfBuildProperty} 总开关：{@code ddd4j.monitor.enabled=false} 时整个装配禁用</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
@IfBuildProperty(name = "ddd4j.monitor.enabled", stringValue = "true", enableIfMissing = true)
public class Ddd4jMonitorCdiProducer {

    /**
     * 暴露主仓统一配置模型 {@link BaseMonitorProperties}（业务侧可注入后按需修改）。
     *
     * @param config Quarkus 配置映射
     * @return 由 {@code ddd4j.monitor.*} 填充的配置模型
     */
    @Produces
    @Singleton
    public BaseMonitorProperties baseMonitorProperties(MonitorConfig config) {
        BaseMonitorProperties props = new BaseMonitorProperties();
        BaseMonitorProperties.Log log = props.getLog();
        log.setEnable(config.log().enable());
        log.setRateLimiterPermitsPerSecond(
                config.log().rateLimiterPermitsPerSecond() > 0
                        ? config.log().rateLimiterPermitsPerSecond()
                        : null);

        MonitorConfig.DingTalk dingtalk = config.log().dingtalk();
        DingTalkProperties dingTalkProperties = log.getDingtalk();
        dingTalkProperties.setEnable(dingtalk.enable());
        dingtalk.token().ifPresent(dingTalkProperties::setToken);
        dingtalk.secret().ifPresent(dingTalkProperties::setSecret);

        MonitorConfig.WeCom wecom = config.log().wecom();
        WeComProperties weComProperties = log.getWecom();
        weComProperties.setEnable(wecom.enable());
        wecom.key().ifPresent(weComProperties::setKey);

        MonitorConfig.Feishu feishu = config.log().feishu();
        FeishuProperties feishuProperties = log.getFeishu();
        feishuProperties.setEnable(feishu.enable());
        feishu.webhookUrl().ifPresent(feishuProperties::setWebhookUrl);
        feishu.secret().ifPresent(feishuProperties::setSecret);

        MonitorConfig.App app = config.log().app();
        BaseMonitorProperties.App appProperties = log.getApp();
        app.project().ifPresent(appProperties::setProject);
        app.env().ifPresent(appProperties::setEnv);
        app.name().ifPresent(appProperties::setName);
        return props;
    }

    /**
     * 钉钉群机器人发送器（{@code ddd4j.monitor.log.dingtalk.enable=false} 时不装配）。
     *
     * @param config Quarkus 配置映射
     * @return {@link DingTalkRobotSender}
     */
    @Produces
    @Singleton
    @IfBuildProperty(name = "ddd4j.monitor.log.dingtalk.enable", stringValue = "true", enableIfMissing = true)
    public DingTalkRobotSender dingTalkRobotSender(MonitorConfig config) {
        return new DingTalkRobotSender(
                config.log().dingtalk().token().orElse(""),
                config.log().dingtalk().secret().orElse(""));
    }

    /**
     * 企业微信机器人发送器（{@code ddd4j.monitor.log.wecom.enable=false} 时不装配）。
     *
     * @param config Quarkus 配置映射
     * @return {@link WeComRobotSender}
     */
    @Produces
    @Singleton
    @IfBuildProperty(name = "ddd4j.monitor.log.wecom.enable", stringValue = "true", enableIfMissing = true)
    public WeComRobotSender weComRobotSender(MonitorConfig config) {
        return new WeComRobotSender(config.log().wecom().key().orElse(""));
    }

    /**
     * 飞书机器人发送器（{@code ddd4j.monitor.log.feishu.enable=false} 时不装配）。
     *
     * @param config Quarkus 配置映射
     * @return {@link FeishuRobotSender}
     */
    @Produces
    @Singleton
    @IfBuildProperty(name = "ddd4j.monitor.log.feishu.enable", stringValue = "true", enableIfMissing = true)
    public FeishuRobotSender feishuRobotSender(MonitorConfig config) {
        return new FeishuRobotSender(
                config.log().feishu().webhookUrl().orElse(""),
                config.log().feishu().secret().orElse(""));
    }
}
