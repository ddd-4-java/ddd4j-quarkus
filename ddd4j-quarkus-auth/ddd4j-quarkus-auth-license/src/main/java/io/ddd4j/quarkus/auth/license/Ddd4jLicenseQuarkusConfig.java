package io.ddd4j.quarkus.auth.license;

import io.ddd4j.auth.license.LicenseVerify;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

/**
 * License Quarkus CDI 装配。
 *
 * <p>纯 Java 实现来自 {@code io.ddd4j:ddd4j-auth-license}，本类仅负责从 Quarkus 配置创建
 * {@link LicenseVerify} 并对齐 boot 模块的 init/destroy 生命周期。
 */
@ApplicationScoped
public class Ddd4jLicenseQuarkusConfig {

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
        return licenseVerify;
    }

    public void destroyLicenseVerify(@Disposes LicenseVerify licenseVerify) {
        licenseVerify.unInstallLicense();
    }
}
