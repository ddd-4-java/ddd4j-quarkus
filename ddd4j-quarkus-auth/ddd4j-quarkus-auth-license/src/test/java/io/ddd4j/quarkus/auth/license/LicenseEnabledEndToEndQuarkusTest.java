package io.ddd4j.quarkus.auth.license;

import io.ddd4j.extension.license.LicenseVerify;
import io.ddd4j.extension.license.creator.LicenseCreator;
import io.ddd4j.extension.license.creator.LicenseCreatorParam;
import io.ddd4j.extension.license.keystore.LicenseKeyStoreGenerator;
import io.ddd4j.extension.license.keystore.LicenseKeyStoreParam;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

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
    private static final String FIXTURE_DIRECTORY_PROPERTY =
            LicenseEnabledEndToEndQuarkusTest.class.getName() + ".fixture-directory";
    private static final Set<PosixFilePermission> OWNER_ONLY_DIRECTORY_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
    private static final Set<PosixFilePermission> OWNER_ONLY_FILE_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    @Inject
    LicenseVerify licenseVerify;

    static void generateKeystoreAndLicense(LicenseFixture fixture) throws Exception {
        // 1. keytool 生成测试公私钥库。
        LicenseKeyStoreParam ksParam = LicenseKeyStoreParam.builder()
                .privateAlias(PRIVATE_ALIAS)
                .publicAlias(PUBLIC_ALIAS)
                .storePass(STORE_PASS)
                .keyPass(KEY_PASS)
                .privateKeysStorePath(fixture.privateKeysStorePath().toString())
                .publicKeysStorePath(fixture.publicKeysStorePath().toString())
                .build();
        new LicenseKeyStoreGenerator().generate(ksParam);

        // 2. 在 Quarkus 创建 LicenseVerify Bean 前签发真实测试许可证。
        LicenseCreatorParam param = new LicenseCreatorParam();
        param.setSubject(SUBJECT);
        param.setPrivateAlias(PRIVATE_ALIAS);
        param.setKeyPass(KEY_PASS);
        param.setStorePass(STORE_PASS);
        param.setLicensePath(fixture.licensePath().toString());
        param.setPrivateKeysStorePath(fixture.privateKeysStorePath().toString());
        Calendar expiry = Calendar.getInstance();
        expiry.add(Calendar.DAY_OF_YEAR, 30);
        param.setExpiryTime(expiry.getTime());
        param.setConsumerType("user");
        param.setConsumerAmount(1);
        assertThat(new LicenseCreator(param).generateLicense()).isTrue();
        restrictToOwner(fixture.privateKeysStorePath(), OWNER_ONLY_FILE_PERMISSIONS);
        restrictToOwner(fixture.publicKeysStorePath(), OWNER_ONLY_FILE_PERMISSIONS);
        restrictToOwner(fixture.licensePath(), OWNER_ONLY_FILE_PERMISSIONS);
    }

    @Test
    void licenseVerifyBeanAssembledWhenLicenseEnabled() {
        assertThat(licenseVerify).isNotNull();
        assertThat(licenseVerify.isInstallSuccess()).isTrue();
        assertThat(licenseVerify.verify()).isTrue();
    }

    @Test
    void fixtureUsesUniquePrivateTemporaryDirectory() throws IOException {
        LicenseFixture fixture = LicenseEnabledProfile.currentFixture();

        assertThat(fixture.directory().getFileName().toString())
                .startsWith("quarkus-license-end2end-");
        assertThat(fixture.directory().normalize().startsWith(Path.of(System.getProperty("java.io.tmpdir")).normalize()))
                .isTrue();
        assertThat(System.getProperty(FIXTURE_DIRECTORY_PROPERTY)).isEqualTo(fixture.directory().toString());
        assertThat(Files.isRegularFile(fixture.privateKeysStorePath())).isTrue();
        assertThat(Files.isRegularFile(fixture.publicKeysStorePath())).isTrue();
        assertThat(Files.isRegularFile(fixture.licensePath())).isTrue();
        assertOwnerOnlyPermissions(fixture.directory(), OWNER_ONLY_DIRECTORY_PERMISSIONS);
        assertOwnerOnlyPermissions(fixture.privateKeysStorePath(), OWNER_ONLY_FILE_PERMISSIONS);
        assertOwnerOnlyPermissions(fixture.publicKeysStorePath(), OWNER_ONLY_FILE_PERMISSIONS);
        assertOwnerOnlyPermissions(fixture.licensePath(), OWNER_ONLY_FILE_PERMISSIONS);
    }

    /**
     * Profile 静态持有本 JVM/测试运行的唯一夹具，并在 Quarkus 创建 CDI Bean 前完成准备。
     */
    public static class LicenseEnabledProfile implements QuarkusTestProfile {
        private static LicenseFixture fixture;

        @Override
        public Map<String, String> getConfigOverrides() {
            try {
                LicenseFixture currentFixture = prepareFixture();
                return Map.of(
                        "license.enabled", "true",
                        "license.subject", SUBJECT,
                        "license.public-alias", PUBLIC_ALIAS,
                        "license.store-pass", STORE_PASS,
                        "license.license-path", currentFixture.licensePath().toString(),
                        "license.public-keys-store-path", currentFixture.publicKeysStorePath().toString());
            } catch (Exception exception) {
                throw new IllegalStateException("无法准备 Quarkus License 端到端测试夹具", exception);
            }
        }

        static LicenseFixture currentFixture() {
            synchronized (System.getProperties()) {
                String fixtureDirectoryValue = System.getProperty(FIXTURE_DIRECTORY_PROPERTY);
                if (Objects.isNull(fixtureDirectoryValue) || fixtureDirectoryValue.isBlank()) {
                    throw new IllegalStateException("Quarkus License 测试夹具尚未初始化");
                }
                fixture = fixtureFor(Path.of(fixtureDirectoryValue));
                return fixture;
            }
        }

        @Override
        public List<TestResourceEntry> testResources() {
            try {
                return List.of(new TestResourceEntry(FixtureCleanupResource.class,
                        Map.of("fixture-directory", prepareFixture().directory().toString())));
            } catch (Exception exception) {
                throw new IllegalStateException("无法注册 Quarkus License 测试夹具清理器", exception);
            }
        }

        private static LicenseFixture prepareFixture() throws Exception {
            synchronized (System.getProperties()) {
                String fixtureDirectoryValue = System.getProperty(FIXTURE_DIRECTORY_PROPERTY);
                if (Objects.nonNull(fixtureDirectoryValue) && !fixtureDirectoryValue.isBlank()) {
                    fixture = fixtureFor(Path.of(fixtureDirectoryValue));
                    return fixture;
                }
                Path directory = Files.createTempDirectory("quarkus-license-end2end-");
                LicenseFixture candidate = fixtureFor(directory);
                System.setProperty(FIXTURE_DIRECTORY_PROPERTY, directory.toString());
                try {
                    restrictToOwner(directory, OWNER_ONLY_DIRECTORY_PERMISSIONS);
                    generateKeystoreAndLicense(candidate);
                    fixture = candidate;
                    return candidate;
                } catch (Exception exception) {
                    System.clearProperty(FIXTURE_DIRECTORY_PROPERTY);
                    try {
                        deleteRecursively(directory);
                    } catch (IOException cleanupException) {
                        exception.addSuppressed(cleanupException);
                    }
                    throw exception;
                }
            }
        }
    }

    /**
     * 由 Quarkus 在应用关闭后停止，确保 CDI 的 LicenseVerify 销毁路径完成后再清理夹具。
     */
    public static class FixtureCleanupResource implements QuarkusTestResourceLifecycleManager {
        private Path fixtureDirectory;

        @Override
        public void init(Map<String, String> initArgs) {
            String fixtureDirectoryValue = initArgs.get("fixture-directory");
            if (Objects.isNull(fixtureDirectoryValue) || fixtureDirectoryValue.isBlank()) {
                throw new IllegalArgumentException("缺少 License 测试夹具目录");
            }
            fixtureDirectory = Path.of(fixtureDirectoryValue);
        }

        @Override
        public Map<String, String> start() {
            return Map.of();
        }

        @Override
        public void stop() {
            if (Objects.isNull(fixtureDirectory)) {
                return;
            }
            try {
                deleteRecursively(fixtureDirectory);
                synchronized (System.getProperties()) {
                    if (fixtureDirectory.toString().equals(System.getProperty(FIXTURE_DIRECTORY_PROPERTY))) {
                        System.clearProperty(FIXTURE_DIRECTORY_PROPERTY);
                    }
                }
                fixtureDirectory = null;
            } catch (IOException exception) {
                throw new IllegalStateException("无法清理 Quarkus License 测试夹具目录: " + fixtureDirectory, exception);
            }
        }
    }

    private static void assertOwnerOnlyPermissions(Path path, Set<PosixFilePermission> permissions) throws IOException {
        if (Files.getFileStore(path).supportsFileAttributeView(PosixFileAttributeView.class)) {
            assertThat(Files.getPosixFilePermissions(path)).isEqualTo(permissions);
        }
    }

    private static void restrictToOwner(Path path, Set<PosixFilePermission> permissions) throws IOException {
        if (Files.getFileStore(path).supportsFileAttributeView(PosixFileAttributeView.class)) {
            Files.setPosixFilePermissions(path, permissions);
        }
    }

    private static void deleteRecursively(Path directory) throws IOException {
        List<Path> paths;
        try (Stream<Path> pathStream = Files.walk(directory)) {
            paths = pathStream.sorted(Comparator.reverseOrder()).toList();
        } catch (IOException exception) {
            throw new IOException("无法遍历 License 测试夹具目录: " + directory, exception);
        }
        IOException failure = null;
        for (Path path : paths) {
            try {
                Files.delete(path);
            } catch (IOException exception) {
                IOException pathFailure = new IOException("无法删除 License 测试夹具路径: " + path, exception);
                if (failure == null) {
                    failure = pathFailure;
                } else {
                    failure.addSuppressed(pathFailure);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
        if (Files.exists(directory)) {
            throw new IOException("License 测试夹具目录未清理: " + directory);
        }
    }

    private static LicenseFixture fixtureFor(Path directory) {
        return new LicenseFixture(directory, directory.resolve("license.lic"),
                directory.resolve("publicCerts.keystore"), directory.resolve("privateKeys.keystore"));
    }

    private record LicenseFixture(Path directory, Path licensePath, Path publicKeysStorePath, Path privateKeysStorePath) {
    }
}
