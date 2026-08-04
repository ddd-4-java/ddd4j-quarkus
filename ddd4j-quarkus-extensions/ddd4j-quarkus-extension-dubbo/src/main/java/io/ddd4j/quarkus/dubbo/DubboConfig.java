package io.ddd4j.quarkus.dubbo;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * ddd4j-quarkus-dubbo 配置（绑定 {@code ddd4j.dubbo.*}）。
 *
 * <p>对应 boot 版 {@code DubboAutoConfiguration} 文档中的 Dubbo 配置项
 * （{@code dubbo.application.name / dubbo.registry.address / dubbo.protocol.* /
 * dubbo.scan.base-packages}），Quarkus 版收敛到 ddd4j 命名空间 {@code ddd4j.dubbo.*}；
 * 总开关 {@code ddd4j.dubbo.enabled} 与 boot 的
 * {@code @ConditionalOnProperty(prefix = "ddd4j.dubbo", name = "enabled")} 语义一致。
 *
 * <p>配置示例（application.properties）：
 * <pre>{@code
 * ddd4j.dubbo.enabled=true
 * ddd4j.dubbo.application.name=my-app
 * ddd4j.dubbo.registry.address=nacos://127.0.0.1:8848
 * ddd4j.dubbo.protocol.name=dubbo
 * ddd4j.dubbo.protocol.port=20880
 * ddd4j.dubbo.scan.base-packages=com.example.provider
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ConfigMapping(prefix = "ddd4j.dubbo")
public interface DubboConfig {

    /**
     * 是否启用 ddd4j-dubbo 装配（总开关，默认 true；
     * 对应 boot 的 {@code ddd4j.dubbo.enabled} 条件属性）。
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * 应用名（对应 {@code dubbo.application.name}）。
     */
    Application application();

    /**
     * 注册中心（对应 {@code dubbo.registry.address}）。
     */
    Registry registry();

    /**
     * 服务协议（对应 {@code dubbo.protocol.name / dubbo.protocol.port}）。
     */
    Protocol protocol();

    /**
     * 服务扫描（对应 {@code dubbo.scan.base-packages}）。
     */
    Scan scan();

    /**
     * 应用配置。
     */
    interface Application {

        /**
         * 应用名称。
         */
        @WithDefault("")
        String name();
    }

    /**
     * 注册中心配置。
     */
    interface Registry {

        /**
         * 注册中心地址，如 {@code nacos://127.0.0.1:8848}。
         */
        @WithDefault("")
        String address();
    }

    /**
     * 服务协议配置。
     */
    interface Protocol {

        /**
         * 协议名称（默认 {@code dubbo}）。
         */
        @WithDefault("dubbo")
        String name();

        /**
         * 协议端口（默认 20880）。
         */
        @WithDefault("20880")
        int port();
    }

    /**
     * 服务扫描配置。
     */
    interface Scan {

        /**
         * Dubbo Service 扫描包（逗号分隔，可空）。
         */
        @WithDefault("")
        String basePackages();
    }
}
