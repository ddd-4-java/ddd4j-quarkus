# Maven 4 / Quarkus settings 兼容说明

`feature/4.0.x` 使用 Maven Model 4.1.0 和 Maven 4 wrapper，同时保留
`<modules>` 聚合器。Quarkus 3.38.2 的 bootstrap resolver 仍通过 Maven 3
settings reader 读取 Maven 4 安装目录下的全局 `conf/settings.xml`，无法识别
Maven 4 新增的顶层 `<repositories>`，因而会为每次 Quarkus 测试启动输出
`Settings problem encountered`。

Unix/macOS 使用 `./mvnw-quarkus`，Windows 使用 `mvnw-quarkus.cmd`。
两个仓库级入口会解析自身所在的仓库绝对路径，通过标准 `-gs`
参数显式传入仓库内最小 Maven 3 兼容 global settings，并用
`-Dddd4j.maven.home` 将其 Maven home 传入 Surefire/Failsafe
测试进程。绝对路径是必要的：Quarkus
测试可能从嵌套模块目录启动，相对路径会回退到 Maven 4 内置 settings。
该文件不包含仓库、镜像、服务器或凭据，也不会替代用户级
`~/.m2/settings.xml`。GitHub Actions 仍由
`MAVEN_SETTINGS_XML` 写入用户 settings，并从其中取得阿里云私有仓库配置。

这是 Quarkus 3.38.2 Maven 4 前瞻适配的临时兼容层，不代表上游已完整支持
Maven 4 settings 模型。上游 resolver 能直接解析 Maven 4 global settings 后，
应删除 CI/wrapper 命令中的 `-gs` 参数和本兼容文件，并重新执行空缓存消费、
完整测试和 CI 门禁。

## 标准命令

Unix/macOS 本地验证：

```bash
./mvnw-quarkus -B clean verify -DskipTests=false
```

Windows 本地验证：

```bat
mvnw-quarkus.cmd -B clean verify -DskipTests=false
```

阿里云 SNAPSHOT 发布（发布前必须完成门禁并取得发布授权）：

```bash
./mvnw-quarkus -B clean deploy -Dmaven.test.skip=true
```

发布仍从用户级 `~/.m2/settings.xml` 读取
`MAVEN_SETTINGS_XML` 安装的私库地址与凭据。
