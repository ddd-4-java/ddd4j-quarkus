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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link Ddd4jLicenseQuarkusConfig} 开启时的真实许可证生命周期契约。
 *
 * <p>Profile 在 CDI 创建 {@link LicenseVerify} 前生成本次测试唯一的密钥库和许可证；测试资源在
 * Quarkus 关闭后删除全部临时密钥材料。该测试直接验证安装和验签结果，而不是仅验证 Bean 已装配。
 */
@QuarkusTest
@TestProfile(LicenseEnabledEndToEndQuarkusTest.LicenseEnabledProfile.class)
class LicenseEnabledEndToEndQuarkusTest {

    private static final String SUBJECT = "quarkus-test";
    private static final String STORE_PASS = "storepass";
    private static final String KEY_PASS = "storepass";
    private static final String PRIVATE_ALIAS = "privateKey";
    private static final String PUBLIC_ALIAS = "publicCert";
    private static final String FIXTURE_DIRECTORY_PROPERTY =
            "io.ddd4j.quarkus.auth.license.end2end.fixture-directory";
    private static final String FAILED_FIXTURE_DIRECTORY_PROPERTY =
            "io.ddd4j.quarkus.auth.license.end2end.failed-fixture-directory";
    private static final String FIXTURE_DIRECTORY_PREFIX = "quarkus-license-end2end-";

    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE);
    private static final Set<PosixFilePermission> FILE_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE);

    @Inject
    LicenseVerify licenseVerify;

    /**
     * 如果 CDI 在签发前安装许可证，或签名/验签链路失效，此测试必须失败。
     */
    @Test
    void licenseVerifyBeanInstallsAndVerifiesTheProfileLicense() throws IOException {
        assertThat(licenseVerify).isNotNull();
        assertThat(licenseVerify.isInstallSuccess()).isTrue();
        assertThat(licenseVerify.verify()).isTrue();

        Fixture fixture = fixtureFromSystemProperty();
        assertThat(fixture.directory().getFileName().toString()).startsWith(FIXTURE_DIRECTORY_PREFIX);
        assertSensitiveFixturePermissions(fixture);
    }

    /**
     * 如果签发失败后保留私钥、许可证目录或跨类加载器桥接属性，此测试必须失败。
     */
    @Test
    void failedFixturePreparationDeletesTemporaryKeyMaterial() throws IOException {
        Path directory = Files.createTempDirectory("quarkus-license-signing-failure-");

        try {
            Path partialKey = Files.createDirectories(directory.resolve("partial/nested"))
                    .resolve("private-key.placeholder");
            Files.writeString(partialKey, "test-only-sensitive-placeholder");
            System.setProperty(FAILED_FIXTURE_DIRECTORY_PROPERTY, directory.toString());
            assertThat(Files.isRegularFile(partialKey)).isTrue();
            assertThat(System.getProperty(FAILED_FIXTURE_DIRECTORY_PROPERTY)).isEqualTo(directory.toString());

            assertThatThrownBy(() -> LicenseEnabledProfile.prepareFixture(directory,
                    FAILED_FIXTURE_DIRECTORY_PROPERTY, false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("无法生成 Quarkus License 端到端测试许可证")
                    .hasRootCauseMessage("私钥库不存在或不可读: " + directory.resolve("privateKeys.keystore"));
            assertThat(Files.exists(partialKey)).isFalse();
            assertThat(Files.exists(directory)).isFalse();
            assertThat(System.getProperty(FAILED_FIXTURE_DIRECTORY_PROPERTY)).isNull();
        } finally {
            if (Files.exists(directory)) {
                deleteFixtureDirectory(directory, null);
            }
            System.clearProperty(FAILED_FIXTURE_DIRECTORY_PROPERTY);
        }
    }

    /**
     * 可读但无效的私钥库令真实签发器返回 false；该分支也必须删除部分材料与桥接属性。
     */
    @Test
    void falseSigningResultDeletesPartialKeyMaterialAndClearsBridgeProperty() throws IOException {
        Path directory = Files.createTempDirectory("quarkus-license-false-signing-");

        try {
            Fixture fixture = Fixture.at(directory);
            Path nestedKey = Files.createDirectories(directory.resolve("partial/nested"))
                    .resolve("private-key.placeholder");
            Files.writeString(nestedKey, "test-only-sensitive-placeholder");
            Files.writeString(fixture.privateKeysStore(), "test-only-invalid-keystore");
            System.setProperty(FAILED_FIXTURE_DIRECTORY_PROPERTY, directory.toString());
            assertThat(Files.isReadable(fixture.privateKeysStore())).isTrue();
            assertThat(Files.isRegularFile(nestedKey)).isTrue();

            assertThatThrownBy(() -> LicenseEnabledProfile.prepareFixture(directory,
                    FAILED_FIXTURE_DIRECTORY_PROPERTY, false))
                    .isInstanceOf(IllegalStateException.class)
                    .cause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("无法生成 Quarkus License 端到端测试许可证")
                    .hasNoCause();
            assertThat(Files.exists(nestedKey)).isFalse();
            assertThat(Files.exists(fixture.privateKeysStore())).isFalse();
            assertThat(Files.exists(directory)).isFalse();
            assertThat(System.getProperty(FAILED_FIXTURE_DIRECTORY_PROPERTY)).isNull();
        } finally {
            if (Files.exists(directory)) {
                deleteFixtureDirectory(directory, null);
            }
            System.clearProperty(FAILED_FIXTURE_DIRECTORY_PROPERTY);
        }
    }

    /**
     * 如果递归清理遗漏嵌套文件或未清除关联系统属性，此测试必须失败。
     */
    @Test
    void fixtureCleanupRemovesNestedFilesAndClearsItsBridgeProperty() throws IOException {
        Path directory = Files.createTempDirectory("quarkus-license-cleanup-");
        Path nestedFile = Files.createDirectories(directory.resolve("nested")).resolve("secret");
        Files.writeString(nestedFile, "test-only");
        System.setProperty(FAILED_FIXTURE_DIRECTORY_PROPERTY, directory.toString());

        deleteFixtureDirectory(directory, FAILED_FIXTURE_DIRECTORY_PROPERTY);

        assertThat(Files.exists(directory)).isFalse();
        assertThat(System.getProperty(FAILED_FIXTURE_DIRECTORY_PROPERTY)).isNull();
    }

    private static Fixture fixtureFromSystemProperty() {
        String directory = System.getProperty(FIXTURE_DIRECTORY_PROPERTY);
        assertThat(directory).isNotBlank();
        return Fixture.at(Path.of(directory));
    }

    private static void assertSensitiveFixturePermissions(Fixture fixture) throws IOException {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return;
        }

        assertThat(Files.getPosixFilePermissions(fixture.directory())).isEqualTo(DIRECTORY_PERMISSIONS);
        assertThat(Files.getPosixFilePermissions(fixture.privateKeysStore())).isEqualTo(FILE_PERMISSIONS);
        assertThat(Files.getPosixFilePermissions(fixture.publicKeysStore())).isEqualTo(FILE_PERMISSIONS);
        assertThat(Files.getPosixFilePermissions(fixture.license())).isEqualTo(FILE_PERMISSIONS);
    }

    private static void applySensitiveFixturePermissions(Fixture fixture) throws IOException {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return;
        }

        Files.setPosixFilePermissions(fixture.directory(), DIRECTORY_PERMISSIONS);
        Files.setPosixFilePermissions(fixture.privateKeysStore(), FILE_PERMISSIONS);
        Files.setPosixFilePermissions(fixture.publicKeysStore(), FILE_PERMISSIONS);
        Files.setPosixFilePermissions(fixture.license(), FILE_PERMISSIONS);
    }

    private static void deleteFixtureDirectory(Path directory, String bridgeProperty) throws IOException {
        List<IOException> failures = new ArrayList<>();

        try {
            if (Files.notExists(directory)) {
                return;
            }
            try (Stream<Path> paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException exception) {
                        failures.add(new IOException("无法删除 License 测试临时文件: " + path, exception));
                    }
                });
            }
        } finally {
            if (Objects.nonNull(bridgeProperty)
                    && Objects.equals(System.getProperty(bridgeProperty), directory.toString())) {
                System.clearProperty(bridgeProperty);
            }
        }

        if (!failures.isEmpty()) {
            IOException cleanupFailure = new IOException("无法完整清理 License 测试临时目录: " + directory);
            failures.forEach(cleanupFailure::addSuppressed);
            throw cleanupFailure;
        }
    }

    private record Fixture(Path directory, Path privateKeysStore, Path publicKeysStore, Path license) {

        private static Fixture at(Path directory) {
            return new Fixture(directory, directory.resolve("privateKeys.keystore"),
                    directory.resolve("publicCerts.keystore"), directory.resolve("license.lic"));
        }
    }

    /**
     * 在 Quarkus 构造 CDI 容器前签发许可证，并通过系统属性向测试资源的独立类加载器公开目录路径。
     */
    public static class LicenseEnabledProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            Fixture fixture = prepareFixture();
            return Map.of(
                    "license.enabled", "true",
                    "license.subject", SUBJECT,
                    "license.public-alias", PUBLIC_ALIAS,
                    "license.store-pass", STORE_PASS,
                    "license.license-path", fixture.license().toString(),
                    "license.public-keys-store-path", fixture.publicKeysStore().toString());
        }

        @Override
        public List<TestResourceEntry> testResources() {
            prepareFixture();
            return List.of(new TestResourceEntry(LicenseFixtureResource.class));
        }

        private static Fixture prepareFixture() {
            String configuredDirectory = System.getProperty(FIXTURE_DIRECTORY_PROPERTY);
            if (Objects.nonNull(configuredDirectory)) {
                Path existingDirectory = Path.of(configuredDirectory);
                if (Files.isDirectory(existingDirectory)) {
                    return Fixture.at(existingDirectory);
                }
                System.clearProperty(FIXTURE_DIRECTORY_PROPERTY);
            }

            try {
                return prepareFixture(Files.createTempDirectory(FIXTURE_DIRECTORY_PREFIX),
                        FIXTURE_DIRECTORY_PROPERTY, true);
            } catch (IOException exception) {
                throw new IllegalStateException("无法创建 Quarkus License 端到端测试临时目录", exception);
            }
        }

        private static Fixture prepareFixture(Path directory, String bridgeProperty, boolean generateKeyStores) {
            Fixture fixture = Fixture.at(directory);
            if (Objects.nonNull(bridgeProperty)) {
                System.setProperty(bridgeProperty, directory.toString());
            }

            try {
                if (generateKeyStores) {
                    LicenseKeyStoreParam keyStoreParam = LicenseKeyStoreParam.builder()
                            .privateAlias(PRIVATE_ALIAS)
                            .publicAlias(PUBLIC_ALIAS)
                            .storePass(STORE_PASS)
                            .keyPass(KEY_PASS)
                            .privateKeysStorePath(fixture.privateKeysStore().toString())
                            .publicKeysStorePath(fixture.publicKeysStore().toString())
                            .build();
                    new LicenseKeyStoreGenerator().generate(keyStoreParam);
                }

                LicenseCreatorParam param = new LicenseCreatorParam();
                param.setSubject(SUBJECT);
                param.setPrivateAlias(PRIVATE_ALIAS);
                param.setKeyPass(KEY_PASS);
                param.setStorePass(STORE_PASS);
                param.setLicensePath(fixture.license().toString());
                param.setPrivateKeysStorePath(fixture.privateKeysStore().toString());
                Calendar expiry = Calendar.getInstance();
                expiry.add(Calendar.DAY_OF_YEAR, 30);
                param.setExpiryTime(expiry.getTime());
                param.setConsumerType("user");
                param.setConsumerAmount(1);

                if (!new LicenseCreator(param).generateLicense()) {
                    throw new IllegalStateException("无法生成 Quarkus License 端到端测试许可证");
                }
                applySensitiveFixturePermissions(fixture);
                return fixture;
            } catch (Exception exception) {
                try {
                    deleteFixtureDirectory(directory, bridgeProperty);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
                throw new IllegalStateException("无法生成 Quarkus License 端到端测试许可证", exception);
            }
        }
    }

    /**
     * Quarkus 停止应用后清理由 Profile 创建的临时密钥材料。
     */
    public static class LicenseFixtureResource implements QuarkusTestResourceLifecycleManager {

        private Path fixtureDirectory;

        @Override
        public Map<String, String> start() {
            String directory = System.getProperty(FIXTURE_DIRECTORY_PROPERTY);
            if (Objects.isNull(directory)) {
                throw new IllegalStateException("找不到 Quarkus License 端到端测试临时目录");
            }
            fixtureDirectory = Path.of(directory);
            return Map.of();
        }

        @Override
        public void stop() {
            if (Objects.isNull(fixtureDirectory)) {
                return;
            }
            try {
                deleteFixtureDirectory(fixtureDirectory, FIXTURE_DIRECTORY_PROPERTY);
            } catch (IOException exception) {
                throw new IllegalStateException("无法清理 Quarkus License 端到端测试临时目录", exception);
            }
        }
    }
}
