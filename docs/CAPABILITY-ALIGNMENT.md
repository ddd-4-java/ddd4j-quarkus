# feature/3.3.x P5-A 能力证据

本页只记录 `feature/3.3.x` 在普通独立克隆 `ddd4j-quarkus-33x-sync` 中于 2026-09-09 执行的本地门禁。代码基线为 `7c62a27`；中断前 Task 5 及 `feature/4.0.x` 的运行结果均未复用。规格事实源为 [P5-A design](superpowers/specs/2026-09-09-feature-33x-p5a-capability-sync-design.md)，实施步骤见 [plan](superpowers/plans/2026-09-09-feature-33x-p5a-capability-sync.md)。

当前状态：**Task 5 被 Java 17 全量门禁阻塞，P5-A 未本地完成**。远端消费者、Web/License 定向测试通过不等于整条维护线通过。

## 版本与远端消费者

| 项目 | 本次基线 |
|---|---|
| 本仓 revision | `3.3.x.20260630-SNAPSHOT` |
| ddd4j parent / managed dependencies / consumer | `2.0.x.20260630-SNAPSHOT` |
| Quarkus BOM、实际 runtime、Maven plugin | `3.37.4` |
| Maven wrapper / POM / 聚合 | `3.8.1` / Model `4.0.0` / `<modules>` |
| Java 17 | Amazon Corretto `17.0.20.1`（XML java.version），macOS aarch64 |
| Java 21 | Microsoft `21.0.12.1`，macOS aarch64 |

Task 1 的版本选择记录说明 `20260730` 缺少 `io.ddd4j:ddd4j-dependencies:pom`，因此统一到 `20260630`。本次没有重新测试失败候选，独立重跑的是最终选定版本：

```bash
./mvnw -B -U -f .github/maven/ddd4j-remote-consumer/pom.xml \
  -Dmaven.repo.local=/tmp/ddd4j-p5a-33-clone-remote.7v5DQj \
  -Dmaven.resolver.transport=wagon dependency:go-offline
```

仓库在命令前由 `mktemp -d` 新建。退出码 0，耗时 04:34，结束于 `2026-09-09T14:46:40+08:00`。以下快照文件均实际存在，且有远端 metadata：

| 坐标（groupId 均为 `io.ddd4j`） | 已解析文件版本 |
|---|---|
| `ddd4j-parent:pom` | `2.0.x.20260630-20260908.150417-5` |
| `ddd4j-dependencies:pom` | `2.0.x.20260630-20260908.150417-9` |
| `ddd4j-runtime-quarkus:jar` | `2.0.x.20260630-20260908.150417-5` |
| `ddd4j-web-quarkus:jar` | `2.0.x.20260630-20260908.150417-5` |
| `ddd4j-extension-license:jar` | `2.0.x.20260630-20260908.150417-5` |

该独立消费者验证 ddd4j 制品及其传递依赖的可下载性；其上游 BOM 自身可拉取 Quarkus 3.38.2，这不代表本仓运行时版本。本仓通过优先导入 Quarkus 3.37.4 BOM 固定版本，实际测试启动日志与构建插件日志是运行版本证据。

## Web 与 License 行为

JDK 17 上重新执行两个模块门禁，退出码均为 0：

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-web -am test
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-auth/ddd4j-quarkus-auth-license -am test
```

Web 为 2 suites / 4 tests / 0 failures / 0 errors / 0 skipped，结束于 `14:43:12+08:00`。`Ddd4jQuarkusWebConsumerTest` 的 2 个测试证明租户传播、生成非空请求 ID，以及业务资源与响应探针同线程时 tenant/request-id/Authorization 清理完成。另 2 个为健康检查测试。

License 为 2 suites / 4 tests / 0 failures / 0 errors / 0 skipped，结束于 `14:43:40+08:00`。`LicenseEnabledEndToEndQuarkusTest` 的 3 个测试包含真实签发后 CDI 安装与验签、POSIX 目录 0700/文件 0600 断言、失败目录清理，以及嵌套文件和桥接系统属性清理；另 1 个为配置测试。保留直接、无版本的 `truelicense-core`（managed `1.33`），因为本次新仓库中的 ddd4j License POM 只声明 `ddd4j-cache`，未声明 TrueLicense。

License 已登记 Minor：失败准备测试使用空目录，尚未覆盖部分密钥已生成或签发返回 false 的情形；不能把该测试描述为覆盖所有签发失败路径。

## Java 17 全量门禁与发布依赖阻塞

执行 `JAVA_HOME` 指向 Corretto 17.0.20.1、对应 `bin` 在 PATH 首位的 `./mvnw -B clean verify -Denforcer.skip=true`，返回 1；耗时 06:03，结束于 `2026-09-09T14:50:27+08:00`。Reactor 共 62 个模块：37 SUCCESS、`ddd4j-quarkus-extension-qrcode` FAILURE、后续 24 SKIPPED。

首个编译错误为 `com.google.zxing.exception.QrCodeErrorCode` / `QrCodeException` 的 class major version 65，而 Java 17 编译器要求 61。后续找不到符号、RenderRequest getter 缺失均发生在这两个类无法加载之后；本任务未修改源代码以掩盖依赖版本问题。

为排除共享 Maven 缓存污染，本次又将 `io.github.hiwepy:zxing-extension:2.0.x.20260630-SNAPSHOT` 以 `dependency:get -Dtransitive=false` 下载到隔离仓库，解析到 `2.0.x.20260630-20260715.051948-2`，退出码 0。新下载与原缓存 JAR 的 SHA256 均为 `a7ae2680633f5199ab6a351fc5dc22643c6d6dfd9ff30fe45fe42c34dccc8707`；JDK 21 `javap -verbose` 确认 `QrCodeErrorCode` major version 为 65。这证明发布制品不满足 Java 17 字节码基线。

Java 17 已完成模块的新 XML 为 **32 suites / 106 tests / 0 failures / 0 errors / 10 skipped**，仅为部分 reactor 结果。原始目录遍历得到 36 suites / 114 tests，是因为 Maven 在 qrcode 中断，尚未到达 Web/License 模块的 clean，遗留此前定向门禁的 4 suites / 8 tests。原始混合快照保存在 `jdk17-failed/`，按本次 reactor SUCCESS 模块筛选后的新报告保存在 `jdk17-fresh-only/`，不得将混合统计称为全量通过。

## Java 21 全量门禁

`JAVA_HOME` 指向 Microsoft 21.0.12.1、对应 `bin` 在 PATH 首位，独立执行 `./mvnw -B clean verify -Denforcer.skip=true`，返回 0；耗时 08:29，结束于 `2026-09-09T14:59:41+08:00`，62/62 模块 SUCCESS。最终 XML 为 **53 suites / 185 tests / 0 failures / 0 errors / 11 skipped**；报告存于 `jdk21/xml/`，摘要 `jdk21/summary.json`，日志 `jdk21-clean-verify.log`。XML java.version 唯一值为 `21.0.12.1`；Java 17 新报告的唯一值为 `17.0.20.1`。

Java 21 的通过不能替代 Java 17 维护基线，当前双 JDK 完成门禁仍失败。最终独立审查与修复后的 final-HEAD 全门禁待执行。

## XML skip 原因

以下为 Java 17 新报告及 Java 21 完整报告共同记录的 10 个 skip。类名前缀均为 `io.ddd4j.quarkus.mq.`，表中列出全部方法及 XML 原始 message，不从 tests 总数减去 skipped。

| 类（加上述前缀） | 方法 | XML 原始原因 |
|---|---|---|
| `mqttmica.MicaMqttQuarkusIntegrationTest` | `shouldPublishAndConsumeOrderCreatedEventEndToEnd` | mica-mqtt AIO 在 macOS arm64 的已知缺陷，对齐 javalin Ddd4jMicaMqttMqIT 先例；CI linux 可移除 |
| `ons.OnsQuarkusIntegrationTest` | `shouldPublishAndConsumeOrderCreatedEventEndToEnd` | ONS 商业协议无 Testcontainers 镜像，对齐 javalin Ddd4jOnsMqIT 先例 |
| `pulsar.PulsarQuarkusIntegrationTest` | `shouldPublishAndConsumeOrderCreatedEventEndToEnd` | Pulsar broker startup race condition; skipping test until fixed |
| `pulsar.PulsarQuarkusIntegrationTest` | `shouldInjectMQClient` | Pulsar broker startup race condition; skipping test until fixed |
| `pulsar.PulsarQuarkusIntegrationTest` | `shouldInjectMQProperties` | Pulsar broker startup race condition; skipping test until fixed |
| `pulsar.PulsarQuarkusIntegrationTest` | `shouldInjectSerialization` | Pulsar broker startup race condition; skipping test until fixed |
| `rocket.RocketMqQuarkusIntegrationTest` | `shouldPublishAndConsumeOrderCreatedEventEndToEnd` | RocketMQ warm-up send fails intermittently; broker startup race condition — skip until fixed |
| `rocket.RocketMqQuarkusIntegrationTest` | `shouldInjectMQClient` | RocketMQ warm-up send fails intermittently; broker startup race condition — skip until fixed |
| `rocket.RocketMqQuarkusIntegrationTest` | `shouldInjectMQProperties` | RocketMQ warm-up send fails intermittently; broker startup race condition — skip until fixed |
| `rocket.RocketMqQuarkusIntegrationTest` | `shouldInjectSerialization` | RocketMQ warm-up send fails intermittently; broker startup race condition — skip until fixed |

Java 21 另有 1 项：`io.ddd4j.quarkus.auth.security.SecurityQuarkusConfigTest#subjectProviderExposedAsCdiBeanAndRegisteredInSubjectKit`，原始原因 `Module is deprecated since 3.3.1; see docs/MIGRATION-auth-security-to-satoken.md`。Java 17 未运行到该模块，因此不计入 Java 17 新报告。

## CI 与结构证据

`actionlint .github/workflows/ci.yml` 返回 0；包含非法 `invalid.context` 与空 steps 的临时 workflow 返回 1。静态工作流中 lint 使用 `fail_level: error`、`filter_mode: nofilter`，unit/contract 为 Java 17/21，broker 为 Java 17 的 13 项矩阵。Maven job 均先 setup-java，然后运行安全 settings 配置与空缓存远端解析动作。

配置动作以临时文件解码、校验 server id、0600 权限和原子替换安装 settings，失败时保留旧目标并清除临时文件。Task 4 已有配置成功/失败路径验证记录；Task 5 不将该历史记录当成本次新执行的 shell 测试。

本次扫描目标 POM 和 `.github`：无 Model 4.1.0、`<subprojects>`、`<subproject>`、`3.0.x.20260630-SNAPSHOT`，无 `install-ddd4j`、`sed -i`、`Lint disabled` 匹配。CI 与 dependencies POM 中的陈旧说明性注释作为最终审查 Minor 保留，本任务未修改这两份代码/配置文件。

本次 MQTT 测试生成的两个 UUID `.lck` 文件均由 `.gitignore:111` 的 `**/ddd4j-mq-*-tcplocalhost*/` 忽略；文档暂存前 status 只包含六份授权文档，`git diff --check` 返回 0。

## 证据边界

本次日志的非阻塞警告包括：Hibernate GenericGenerator/数据库生成配置弃用，部分模块忽略 datasource/hibernate-orm/flyway 配置，无 JDBC datasource，Quarkus Maven plugin 未启用 extensions。NATS 容器内部检查缺少 `/bin/sh`，且 JetStream 无匹配 stream 时回退 core NATS；TDMQ 未注入 BrokerPublisher/BrokerSubscriber 时使用测试内存 broker。对应日志和测试结果不构成 JetStream、TDMQ 云服务或所有 broker 生产能力验收。

GitHub-hosted Actions 尚未推送并实际执行验证。Enforcer（本阶段显式 skip）、Native、Dev Mode、deploy、云服务、P5-B–E、master 和生产验收均不在本次完成声明内。GitHub/Codeup push 与 Maven deploy 需要分别授权，未执行。独立克隆的 `origin` 是本机源仓库，不能将其 tracking 状态解释为 GitHub/Codeup 同步证据。

本次原始日志和 XML 归档目录为 `/tmp/ddd4j-p5a-33-clone-task5.cECCPQ`。这是本机临时证据，未作为公开 CI artifact 发布；日志 `remote-consumer.log`、`focused-web.log`、`focused-license.log` 与各自 XML 可用于复核。`focused-license/xml` 包含此前 Web 报告，License 的独立统计只取 auth-license 模块的 2 份 XML。
