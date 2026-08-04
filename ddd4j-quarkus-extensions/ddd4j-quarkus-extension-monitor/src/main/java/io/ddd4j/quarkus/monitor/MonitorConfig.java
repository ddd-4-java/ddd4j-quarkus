package io.ddd4j.quarkus.monitor;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * ddd4j-quarkus-monitor 配置（绑定 {@code ddd4j.monitor.*}）。
 *
 * <p>对应主仓 {@code io.ddd4j.extension.monitor.config.BaseMonitorProperties}（boot 侧
 * {@code @ConfigurationProperties(prefix = "monitor")}，前缀 {@code monitor}）。
 * Quarkus 版收敛到 ddd4j 命名空间 {@code ddd4j.monitor.*}，字段结构与
 * {@code BaseMonitorProperties}（log → dingtalk / wecom / feishu / app）一一对应。
 *
 * <p>配置示例（application.properties）：
 * <pre>{@code
 * ddd4j.monitor.enabled=true
 * ddd4j.monitor.log.enable=true
 * ddd4j.monitor.log.rate-limiter-permits-per-second=0.5
 * ddd4j.monitor.log.dingtalk.enable=true
 * ddd4j.monitor.log.dingtalk.token=钉钉机器人access_token
 * ddd4j.monitor.log.dingtalk.secret=钉钉机器人加签密钥
 * ddd4j.monitor.log.wecom.enable=true
 * ddd4j.monitor.log.wecom.key=企业微信机器人webhook_key
 * ddd4j.monitor.log.feishu.enable=true
 * ddd4j.monitor.log.feishu.webhook-url=https://open.feishu.cn/open-apis/bot/v2/hook/xxx
 * ddd4j.monitor.log.feishu.secret=加签密钥（无则留空）
 * ddd4j.monitor.log.app.project=my-project
 * ddd4j.monitor.log.app.env=prod
 * ddd4j.monitor.log.app.name=my-app
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ConfigMapping(prefix = "ddd4j.monitor")
public interface MonitorConfig {

    /**
     * 是否启用 ddd4j-monitor 装配（总开关，默认 true）。
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * 日志 / 告警相关配置（对齐 {@code BaseMonitorProperties.Log}）。
     */
    Log log();

    /**
     * 日志告警配置。
     */
    interface Log {

        /**
         * 是否启用监控模块总开关。
         */
        @WithDefault("true")
        boolean enable();

        /**
         * 机器人发送速度上限（条/秒）。{@code 0} 表示不限速（对应 boot 的 null）。
         */
        @WithDefault("0")
        double rateLimiterPermitsPerSecond();

        /**
         * 钉钉机器人配置（对齐 {@code DingTalkProperties}）。
         */
        DingTalk dingtalk();

        /**
         * 企业微信机器人配置（对齐 {@code WeComProperties}，was {@code qiwei}）。
         */
        WeCom wecom();

        /**
         * 飞书机器人配置（对齐 {@code FeishuProperties}）。
         */
        Feishu feishu();

        /**
         * 应用基本信息（用于告警内容上下文）。
         */
        App app();
    }

    /**
     * 钉钉群机器人配置。
     */
    interface DingTalk {

        /**
         * 是否启用。
         */
        @WithDefault("true")
        boolean enable();

        /**
         * 钉钉机器人 Webhook access_token。
         */
        @WithDefault("")
        String token();

        /**
         * 钉钉机器人加签密钥。
         */
        @WithDefault("")
        String secret();
    }

    /**
     * 企业微信群机器人配置。
     */
    interface WeCom {

        /**
         * 是否启用。
         */
        @WithDefault("true")
        boolean enable();

        /**
         * 企业微信机器人 Webhook key。
         */
        @WithDefault("")
        String key();
    }

    /**
     * 飞书自定义机器人配置。
     */
    interface Feishu {

        /**
         * 是否启用。
         */
        @WithDefault("true")
        boolean enable();

        /**
         * 飞书机器人 webhook 完整地址（含 hook token）。
         */
        @WithDefault("")
        String webhookUrl();

        /**
         * 加签密钥（"不勾选签名校验"时留空）。
         */
        @WithDefault("")
        String secret();
    }

    /**
     * 应用基本信息。
     */
    interface App {

        /**
         * 项目名称。
         */
        @WithDefault("")
        String project();

        /**
         * 当前环境。
         */
        @WithDefault("")
        String env();

        /**
         * 应用名称。
         */
        @WithDefault("")
        String name();
    }
}
