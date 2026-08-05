package io.ddd4j.quarkus.dubbo;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;

/**
 * ddd4j-quarkus Dubbo CDI 装配（对应 boot 的 {@code DubboAutoConfiguration}）。
 *
 * <p>boot 版通过 {@code @AutoConfiguration + @ConditionalOnClass(EnableDubbo) +
 * @ConditionalOnProperty(ddd4j.dubbo.enabled)} 激活，并依赖 Spring 的 {@code @EnableDubbo}
 * 注解扫描；Quarkus 无 Spring 等价物，本类扮演自动配置角色：
 * <ul>
 *   <li>{@code @IfBuildProperty} 条件开关：{@code ddd4j.dubbo.enabled=false} 时整个装配禁用
 *       （对应 boot 的条件属性）</li>
 *   <li>{@link DubboConfig}：SmallRye {@code @ConfigMapping} 绑定 {@code ddd4j.dubbo.*}
 *       配置（由 Quarkus 构建期自动注册为 CDI Bean）</li>
 *   <li>CDI 暴露 Dubbo 核心配置对象（{@link ApplicationConfig} / {@link RegistryConfig} /
 *       {@link ProtocolConfig}，均为 Dubbo 核心 API、无 Spring 依赖），业务侧注入后交给
 *       自建 Dubbo 服务/引用装配</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
@IfBuildProperty(name = "ddd4j.dubbo.enabled", stringValue = "true", enableIfMissing = true)
public class DubboCdiProducer {

    /**
     * Dubbo 应用配置（{@code ddd4j.dubbo.application.name}）。
     *
     * @param config Quarkus 配置映射
     * @return {@link ApplicationConfig}
     */
    @Produces
    @Singleton
    public ApplicationConfig applicationConfig(DubboConfig config) {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        config.application().name().ifPresent(applicationConfig::setName);
        return applicationConfig;
    }

    /**
     * Dubbo 注册中心配置（{@code ddd4j.dubbo.registry.address}）。
     *
     * @param config Quarkus 配置映射
     * @return {@link RegistryConfig}
     */
    @Produces
    @Singleton
    public RegistryConfig registryConfig(DubboConfig config) {
        RegistryConfig registryConfig = new RegistryConfig();
        config.registry().address().ifPresent(registryConfig::setAddress);
        return registryConfig;
    }

    /**
     * Dubbo 服务协议配置（{@code ddd4j.dubbo.protocol.name / port}）。
     *
     * @param config Quarkus 配置映射
     * @return {@link ProtocolConfig}
     */
    @Produces
    @Singleton
    public ProtocolConfig protocolConfig(DubboConfig config) {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName(config.protocol().name());
        protocolConfig.setPort(config.protocol().port());
        return protocolConfig;
    }
}
