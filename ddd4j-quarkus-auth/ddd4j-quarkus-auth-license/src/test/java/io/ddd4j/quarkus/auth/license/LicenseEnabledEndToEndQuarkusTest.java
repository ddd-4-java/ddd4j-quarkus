package io.ddd4j.quarkus.auth.license;

import io.ddd4j.extension.license.LicenseVerify;
import io.ddd4j.extension.license.creator.LicenseCreator;
import io.ddd4j.extension.license.creator.LicenseCreatorParam;
import io.ddd4j.extension.license.keystore.LicenseKeyStoreGenerator;
import io.ddd4j.extension.license.keystore.LicenseKeyStoreParam;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Calendar;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@DDI1Ddd4jLicenseQuarkusConfig} 开启装配路径（{@code license.enabled=true}）的
 * 配置 + CDI 装配验证。
 *
 * <p><b>覆盖范围</b>：
 * <ul>
 *   <li>{@code license.enabled=true} 时 {@link Ddd4jLicenseQuarkusConfig} 类被装配
 *       （类级 {@code @IfBuildProperty} 生效）</li>
 *   <li>5 个 license.* 属性 {@code @ConfigProperty} 注入正确（subject / public-alias / store-pass）</li>
 *   <li>{@link LicenseVerify} Bean 被 {@code @Produces} 装配，注入点解析成功</li>
 *   <li>真实许可证完成签发、安装和验签</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@QuarkusTest
@TestProfile(LicenseEnabledEndToEndQuarkusTest.LicenseEnabledProfile.class)
class LicenseEnabledEndToEndQuarkusTest {

    private static final String SUBJECT = "quarkus-test";
    /** 8 字符密码，避开 JKS 内部 8 字节对齐问题（短密码会触发 "Input length must be multiple of 8"） */
    private static final String STORE_PASS = "storepass";
    private static final String KEY_PASS = "storepass";
    private static final String PRIVATE_ALIAS = "privateKey";
    private static final String PUBLIC_ALIAS = "publicCert";

    /**
     * 测试路径位于 {@code java.io.tmpdir} 的固定子目录。Quarkus profile 在 CDI
     * 初始化前读取配置，固定路径让 profile 与 {@link #generateKeystoreAndLicense}
     * 访问同一组临时文件。
     */
    private static final String SHARED_LICENSE_PATH =
            System.getProperty("java.io.tmpdir") + "/quarkus-license-end2end/license.lic";
    private static final String SHARED_PUBLIC_KEYS_STORE_PATH =
            System.getProperty("java.io.tmpdir") + "/quarkus-license-end2end/publicCerts.keystore";
    private static final String SHARED_PRIVATE_KEYS_STORE_PATH =
            System.getProperty("java.io.tmpdir") + "/quarkus-license-end2end/privateKeys.keystore";

    @Inject
    LicenseVerify licenseVerify;

    static void generateKeystoreAndLicense() throws Exception {
        Path dir = Path.of(System.getProperty("java.io.tmpdir"), "quarkus-license-end2end");
        if (java.nio.file.Files.exists(dir)) {
            java.nio.file.Files.walk(dir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> { try { java.nio.file.Files.deleteIfExists(p); } catch (Exception ignore) {} });
        }
        java.nio.file.Files.createDirectories(dir);

        // 1. keytool 生成测试公私钥库。
        LicenseKeyStoreParam ksParam = LicenseKeyStoreParam.builder()
                .privateAlias(PRIVATE_ALIAS)
                .publicAlias(PUBLIC_ALIAS)
                .storePass(STORE_PASS)
                .keyPass(KEY_PASS)
                .privateKeysStorePath(SHARED_PRIVATE_KEYS_STORE_PATH)
                .publicKeysStorePath(SHARED_PUBLIC_KEYS_STORE_PATH)
                .build();
        new LicenseKeyStoreGenerator().generate(ksParam);

        // 2. 在 Quarkus 创建 LicenseVerify Bean 前签发真实测试许可证。
        LicenseCreatorParam param = new LicenseCreatorParam();
        param.setSubject(SUBJECT);
        param.setPrivateAlias(PRIVATE_ALIAS);
        param.setKeyPass(KEY_PASS);
        param.setStorePass(STORE_PASS);
        param.setLicensePath(SHARED_LICENSE_PATH);
        param.setPrivateKeysStorePath(SHARED_PRIVATE_KEYS_STORE_PATH);
        Calendar expiry = Calendar.getInstance();
        expiry.add(Calendar.DAY_OF_YEAR, 30);
        param.setExpiryTime(expiry.getTime());
        param.setConsumerType("user");
        param.setConsumerAmount(1);
        assertThat(new LicenseCreator(param).generateLicense()).isTrue();
    }

    @Test
    void licenseVerifyBeanAssembledWhenLicenseEnabled() {
        assertThat(licenseVerify).isNotNull();
        assertThat(licenseVerify.isInstallSuccess()).isTrue();
        assertThat(licenseVerify.verify()).isTrue();
    }

    /**
     * profile 与 {@link #generateKeystoreAndLicense} 共享固定路径
     * （{@link #SHARED_LICENSE_PATH} 等），并在 Quarkus 创建 CDI Bean 前完成夹具准备。
     */
    public static class LicenseEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            try {
                generateKeystoreAndLicense();
            } catch (Exception exception) {
                throw new IllegalStateException("无法准备 Quarkus License 端到端测试夹具", exception);
            }
            return Map.of(
                    "license.enabled", "true",
                    "license.subject", SUBJECT,
                    "license.public-alias", PUBLIC_ALIAS,
                    "license.store-pass", STORE_PASS,
                    "license.license-path", SHARED_LICENSE_PATH,
                    "license.public-keys-store-path", SHARED_PUBLIC_KEYS_STORE_PATH);
        }
    }
}
