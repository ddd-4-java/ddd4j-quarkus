package io.ddd4j.quarkus.auth.license;

import io.ddd4j.auth.license.LicenseVerify;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

/**
 * License Quarkus CDI 装配。
 *
 * <p>纯 Java 实现来自 {@code io.ddd4j:ddd4j-auth-license}，本类仅负责从 Quarkus 配置创建
 * {@link LicenseVerify} 并对齐 boot 模块的 init/destroy 生命周期。
 *
 * <p><b>默认不装配</b>（需 {@code license.enabled=true} 显式开启）：LicenseVerify 构造器对全部
 * 5 个参数要求非空（requireText），无条件装配会让未配置 license.* 的应用启动即失败。
 * 开启后需配齐 license.subject/public-alias/store-pass/license-path/public-keys-store-path。
 */
@ApplicationScoped
@IfBuildProperty(name = "license.enabled", stringValue = "true")
public class Ddd4jLicenseQuarkusConfig {

    private LicenseVerify created;

    @ConfigProperty(name = "license.subject")
    Optional<String> subject;

    @ConfigProperty(name = "license.public-alias")
    Optional<String> publicAlias;

    @ConfigProperty(name = "license.store-pass")
    Optional<String> storePass;

    @ConfigProperty(name = "license.license-path")
    Optional<String> licensePath;

    @ConfigProperty(name = "license.public-keys-store-path")
    Optional<String> publicKeysStorePath;

    @Produces
    @Singleton
    public LicenseVerify licenseVerify() {
        LicenseVerify licenseVerify = new LicenseVerify(subject.orElse(null), publicAlias.orElse(null),
                storePass.orElse(null), licensePath.orElse(null), publicKeysStorePath.orElse(null));
        licenseVerify.installLicense();
        created = licenseVerify;
        return licenseVerify;
    }

    /**
     * 对齐 boot 的 destroy 生命周期（替代 @Disposes——类级 @IfBuildProperty 禁用整 Bean 时
     * 无 producer/disposer 失配问题）。
     */
    @PreDestroy
    public void shutdown() {
        if (created != null) {
            created.unInstallLicense();
        }
    }
}
