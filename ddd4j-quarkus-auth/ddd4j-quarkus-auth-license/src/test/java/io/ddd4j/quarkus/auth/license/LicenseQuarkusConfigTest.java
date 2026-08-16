package io.ddd4j.quarkus.auth.license;

import io.ddd4j.auth.license.LicenseVerify;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Ddd4jLicenseQuarkusConfig} 条件装配测试。
 *
 * <p>LicenseVerify 构造器要求全部 5 个参数非空（requireText），因此装配改为
 * {@code @IfBuildProperty(name = "license.enabled", stringValue = "true")} 显式开启——
 * 本测试验证<b>默认（未开启）</b>时应用正常启动且 LicenseVerify Bean 不存在
 * （未配置 license.* 的应用不会被启动失败拖垮）。
 *
 * <p>开启后的成功装配路径需要真实公钥库与许可证文件，登记待办（见 superpowers README）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@QuarkusTest
class LicenseQuarkusConfigTest {

    @Test
    void licenseVerifyNotInstalledByDefault() {
        InstanceHandle<LicenseVerify> handle = Arc.container().instance(LicenseVerify.class);
        assertThat(handle.isAvailable())
                .as("未设置 license.enabled=true 时不应装配 LicenseVerify（避免未配置应用启动失败）")
                .isFalse();
    }
}
