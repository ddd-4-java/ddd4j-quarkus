package io.ddd4j.quarkus.dubbo;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link DubboCdiProducer} Quarkus 集成测试：验证 Dubbo 配置对象的 CDI 装配
 * 与默认值暴露（不启动真实注册中心/服务）。
 */
@QuarkusTest
class DubboQuarkusTest {

    @Inject
    DubboConfig dubboConfig;

    @Inject
    ApplicationConfig applicationConfig;

    @Inject
    RegistryConfig registryConfig;

    @Inject
    ProtocolConfig protocolConfig;

    @Test
    void beansShouldBeInjectable() {
        assertNotNull(dubboConfig);
        assertNotNull(applicationConfig);
        assertNotNull(registryConfig);
        assertNotNull(protocolConfig);
    }

    @Test
    void protocolShouldUseDubboDefault() {
        // DubboConfig.protocol().name() 默认 "dubbo"
        assertEquals("dubbo", protocolConfig.getName());
    }

    @Test
    void exceptionMapperShouldBePresent() {
        // DubboExceptionMapper 以 @Provider 注册，验证类可加载
        assertNotNull(DubboExceptionMapper.class);
    }
}
