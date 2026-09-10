# feature/3.3.x P5-A 能力证据

## P5-B0 当前本地证据（2026-09-10）

最新已验证源码为 `91ceb9b`：Java 17/21 完整 reactor 各 61/61 SUCCESS、61 suites、
224 tests、0 failures、0 errors、3 skips；4.0.x `fce0e1e` 为 61 suites / 228 tests /
0 failures / 0 errors / 3 skips。Testcontainers 全部 `2.0.5`。13 broker 共
52 tests，仅保留 Mica 与 ONS 的两个方法 skip；Security 废弃方法是第三项全仓 skip。
完整命令、原始 XML、逐项恢复条件及双分支差异见 [P5-B0 本地验证](P5-B0-LOCAL-VERIFICATION.md)。
本地还覆盖 BOM 52 叶、auth runner、NATS、严格 Javadoc、security 代码门禁和
Quarkus 配置 warning=0；最新 HEAD 的 hosted CI、发布与空缓存消费尚未完成。
以下 P5-A 记录保留为历史证据，不能将旧 Pulsar/RocketMQ skips 套用到本次结果。

本页只记录 `feature/3.3.x` 在普通独立克隆 `ddd4j-quarkus-33x-sync` 中于 2026-09-09 执行的本地门禁。首轮代码基线为 `7c62a27`；Fix round 1 在 `7c62cdd` 上修复 POM 并重新验证。中断前 Task 5 及 `feature/4.0.x` 的运行结果均未复用。规格事实源为 [P5-A design](superpowers/specs/2026-09-09-feature-33x-p5a-capability-sync-design.md)，实施步骤见 [plan](superpowers/plans/2026-09-09-feature-33x-p5a-capability-sync.md)。

当前状态：**feature/3.3.x P5-A 本地完成**。已验证代码提交为 `597a5b7`，已包含坐标清理、Web/License 审查修正和 CI/BOM 注释修正，并通过双 JDK 完整门禁。以下首轮失败及 185 tests 记录均为历史证据，当前验收统计以本节的 186 tests 为准。

## Actions Snowflake 后续修复（2026-09-09）

GitHub Actions run `34331450813` 的 Java 17/21 实际失败：IP 派生 workerId 超出 `0..31`。下面 `597a5b7` 的 186 tests 是该提交的历史本地全量证据，不能视为 hosted 成功或后续修复的完整 reactor 结果。

修复增加 `ddd4j.quarkus.data.snowflake.worker-id` 可选配置：显式 `0..31` 原值使用，非法显式值立即拒绝；缺省 IP 通过 `Math.floorMod(value, 32)` 归一化。多节点应分配互不重复的显式节点编号，缺省归一化不保证跨机器唯一。

修复提交 `a5add95` 已完成本地 Java 17/21 data-panache `clean verify`，两个 JDK 均为相关 reactor 3/3 SUCCESS、6 suites / 17 tests / 0 failures / 0 errors / 0 skipped。新增六项行为先取得 6 failures / 0 errors 的 RED，修复后全部 GREEN。测试布局收敛为一个 CDI 配置 profile 加普通边界测试，避免多 profile 累积导致 Metaspace 耗尽。完整本地报告保存在 Git 忽略的 `.superpowers/sdd/2026-09-09-feature-33x-p5a-capability-sync/snowflake-fix-report.md`；原始模块日志为 `/private/tmp/ddd4j-33x-snowflake-module-final-j17.log` 和 `/private/tmp/ddd4j-33x-snowflake-module-final-j21.log`。本轮未重跑 62 模块全量或 hosted Actions，未 push/deploy。

## 已验证代码提交 597a5b7 的验收（历史）

两个 JDK 分别执行 `./mvnw -B clean verify -Denforcer.skip=true`，均返回 0，所有 62 个 reactor 模块成功。最终独立报告统计如下：

| JDK | Reactor | suites | tests | failures | errors | skipped | 耗时 | 结束时间（+08:00） |
|---|---|---|---|---|---|---|---|---|
| 17 | 62/62 SUCCESS | 53 | 186 | 0 | 0 | 11 | 07:05 | 2026-09-09 16:18:25 |
| 21 | 62/62 SUCCESS | 53 | 186 | 0 | 0 | 11 | 05:57 | 2026-09-09 16:24:56 |

原始日志为 `/tmp/ddd4j-p5a-33-final-jdk17.log` 和 `/tmp/ddd4j-p5a-33-final-jdk21.log`；两份日志逐 suite 汇总均与上述统计一致。该已验证提交的 actionlint、分支合约扫描、退役 groupId 零引用、diff/status 检查通过。11 个 skip 按下方 XML 原始理由保留，不从 tests 中减去。

审查新增的 License 清理测试已计入 186 tests；Web 4 tests、License 5 tests 均包含在本次全量结果中。本次后续提交仅更新文档，不修改已经验证的代码、POM 或 CI。GitHub-hosted Actions、Enforcer、push/deploy、Native、Dev Mode、云服务、P5-B–E、master 和生产验收仍不属于本地完成范围。

## 版本与远端消费者

| 项目 | 本次基线 |
|---|---|
| 本仓 revision | `3.3.x.20260630-SNAPSHOT` |
| ddd4j parent / managed dependencies / consumer | `2.0.x.20260630-SNAPSHOT` |
| Quarkus BOM、实际 runtime、Maven plugin | `3.37.4` |
| Maven wrapper / POM / 聚合 | `3.8.1` / Model `4.0.0` / `<modules>` |
| Java 17 | Amazon Corretto `17.0.20.1`（XML java.version），macOS aarch64 |
| Java 21 | Microsoft `21.0.12.1`，macOS aarch64 |
| 自有第三方组件 groupId | 统一 `io.github.easy4j`；已跟踪源码、POM、注释和文档均清除退役发布组引用 |

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

## 首轮 Web 与 License 行为验证（历史）

JDK 17 上重新执行两个模块门禁，退出码均为 0：

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-web -am test
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-auth/ddd4j-quarkus-auth-license -am test
```

Web 为 2 suites / 4 tests / 0 failures / 0 errors / 0 skipped，结束于 `14:43:12+08:00`。`Ddd4jQuarkusWebConsumerTest` 的 2 个测试证明租户传播、生成非空请求 ID，以及业务资源与响应探针同线程时 tenant/request-id/Authorization 清理完成。另 2 个为健康检查测试。

License 为 2 suites / 4 tests / 0 failures / 0 errors / 0 skipped，结束于 `14:43:40+08:00`。`LicenseEnabledEndToEndQuarkusTest` 的 3 个测试包含真实签发后 CDI 安装与验签、POSIX 目录 0700/文件 0600 断言、失败目录清理，以及嵌套文件和桥接系统属性清理；另 1 个为配置测试。保留直接、无版本的 `truelicense-core`（managed `1.33`），因为本次新仓库中的 ddd4j License POM 只声明 `ddd4j-cache`，未声明 TrueLicense。

License 当时登记的 Minor 为失败准备测试使用空目录；最终审查修正已覆盖嵌套部分材料与签发 false 返回，见下节。此处 4 tests 保留为原始运行统计。

## 审查单轮修正

在 `06c7222` 上修正三个审查问题：Web 资源方法返回实际 `ThreadContext` 中的租户、request-id、Authorization 与线程，客户端先断言输入值已绑定，再断言同一服务线程上的清理状态；License 准备异常测试先创建嵌套占位密钥材料，另新增可读但无效 keystore 令真实 `LicenseCreator.generateLicense()` 返回 false 的测试，两者均验证材料、目录和桥接属性清除；CI 与两个 POM 的说明改为发布制品解析、Quarkus 3.37.4 BOM-first 仲裁及保留的 License 兼容依赖理由。未修改生产过滤器、License 实现、依赖版本或实际安装/验签断言。

Web RED 为新增资源绑定断言得到 null（2 tests / 1 failure，`15:59:15+08:00`）。License 在临时把失败清理指向不存在子目录的 mutation check 下，两条材料清理断言均失败（4 tests / 2 failures，`16:00:34+08:00`）；该 mutation 已恢复，未进入提交。

Java 17 定向 GREEN：Web `-Dtest=Ddd4jQuarkusWebConsumerTest` 为 1 suite / 2 tests；License `-Dtest=LicenseEnabledEndToEndQuarkusTest` 为 1 suite / 4 tests；均 0 failures/errors/skipped，分别结束于 `16:01:02+08:00` 与 `16:01:48+08:00`。

受影响模块命令：

```bash
./mvnw -B -Denforcer.skip=true \
  -pl ddd4j-quarkus-web,ddd4j-quarkus-auth/ddd4j-quarkus-auth-license -am clean test
```

Java 17（XML `17.0.20.1`）返回 0，4/4 模块 SUCCESS，22.865s，结束 `16:02:21+08:00`；Java 21（XML `21.0.12.1`）返回 0，4/4 模块 SUCCESS，16.582s，结束 `16:02:57+08:00`。每个 JDK 独立保存 4 suites / 9 tests / 0 failures / 0 errors / 0 skipped：Web 4、License 5。此处只统计这两模块 fresh XML，历史 full reactor 185 tests 不因新增用例被改写成未经执行的全量统计。

新日志及按 JDK 保存的 XML/JSON 位于 `/tmp/ddd4j-p5a-33-final-fixes.orpSyP`。定向报告只选运行的测试类，分别位于 `web-focused-only`、`license-focused-only`；初始 `web-focused` 混入旧健康检查 XML，不作为新定向证据。`modules-jdk17` 与 `modules-jdk21` 均源自各自 clean test。actionlint、零退役 groupId、CI 源码修改模式扫描、目标 POM 结构扫描与 diff 检查均通过。真实 false 路径会记录预期的 `Invalid keystore format` 错误；日志不包含口令或真实私钥材料。这些修正提交于 `597a5b7`，已由本页开头所列双 JDK 全量门禁覆盖。

## 首轮 Java 17 全量门禁与发布依赖阻塞（修复前 RED）

执行 `JAVA_HOME` 指向 Corretto 17.0.20.1、对应 `bin` 在 PATH 首位的 `./mvnw -B clean verify -Denforcer.skip=true`，返回 1；耗时 06:03，结束于 `2026-09-09T14:50:27+08:00`。Reactor 共 62 个模块：37 SUCCESS、`ddd4j-quarkus-extension-qrcode` FAILURE、后续 24 SKIPPED。

首个编译错误为 `com.google.zxing.exception.QrCodeErrorCode` / `QrCodeException` 的 class major version 65，而 Java 17 编译器要求 61。后续找不到符号、RenderRequest getter 缺失均发生在这两个类无法加载之后；本任务未修改源代码以掩盖依赖版本问题。

为排除共享 Maven 缓存污染，首轮又将退役发布组下的 `zxing-extension:2.0.x.20260630-SNAPSHOT` 以 `dependency:get -Dtransitive=false` 下载到隔离仓库，解析到 `2.0.x.20260630-20260715.051948-2`，退出码 0。新下载与原缓存 JAR 的 SHA256 均为 `a7ae2680633f5199ab6a351fc5dc22643c6d6dfd9ff30fe45fe42c34dccc8707`；JDK 21 `javap -verbose` 确认 `QrCodeErrorCode` major version 为 65。这证明旧发布制品不满足 Java 17 字节码基线；完整旧坐标保留于本机原始失败日志，不作为当前推荐依赖。

Java 17 已完成模块的新 XML 为 **32 suites / 106 tests / 0 failures / 0 errors / 10 skipped**，仅为部分 reactor 结果。原始目录遍历得到 36 suites / 114 tests，是因为 Maven 在 qrcode 中断，尚未到达 Web/License 模块的 clean，遗留此前定向门禁的 4 suites / 8 tests。原始混合快照保存在 `jdk17-failed/`，按本次 reactor SUCCESS 模块筛选后的新报告保存在 `jdk17-fresh-only/`，不得将混合统计称为全量通过。

## 首轮 Java 21 全量门禁（修复前）

`JAVA_HOME` 指向 Microsoft 21.0.12.1、对应 `bin` 在 PATH 首位，独立执行 `./mvnw -B clean verify -Denforcer.skip=true`，返回 0；耗时 08:29，结束于 `2026-09-09T14:59:41+08:00`，62/62 模块 SUCCESS。该轮历史 XML 为 **53 suites / 185 tests / 0 failures / 0 errors / 11 skipped**；报告存于 `jdk21/xml/`，摘要 `jdk21/summary.json`，日志 `jdk21-clean-verify.log`。XML java.version 唯一值为 `21.0.12.1`；Java 17 新报告的唯一值为 `17.0.20.1`。

首轮 Java 21 的通过未替代当时失败的 Java 17 维护基线。Fix round 1 的历史修复结果如下，当前最终代码验收以本页开头的 `597a5b7` 双 JDK 门禁为准。

## Fix round 1：恢复 QR 依赖的 Java 17 合约

在 `7c62cdd` 文档提交后，获准将 qrcode 叶模块的 ZXing groupId 改为 `io.github.easy4j`，保留无版本依赖；dependencies POM 移除退役发布组的 ZXing 管理项与相关陈旧注释，版本由导入的 ddd4j BOM 管理。未修改 QR 业务源码和 ci.yml。

新坐标解析到 `2.0.x.20260630-20260907.070807-1`，Java 17 javap 确认 `QrCodeErrorCode` major 61。依赖树只包含 `io.github.easy4j:zxing-extension:jar:2.0.x.20260630-SNAPSHOT:compile`。父任务的独立制品审查确认 39 个类均为 major 61，公开 JVM API 与旧坐标一致。

Java 17 `./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-extensions/ddd4j-quarkus-extension-qrcode -am clean test` 返回 0，3/3 模块成功，结束于 `2026-09-09T15:13:42+08:00`（29.129s）。`QrCodeQuarkusTest` 的 2 个测试实际验证 CDI 生成二维码和 REST render/decode 往返，1 suite / 2 tests / 0 failures / 0 errors / 0 skipped。

Fix round 1 的新日志与 XML 独立保存于 `/tmp/ddd4j-p5a-33-fix1.jQvV1w`。首轮失败与修复后验证证据分别保留，不把定向测试通过当作双 JDK 全量通过。

修复后的 Java 17 完整 `clean verify -Denforcer.skip=true` 已返回 0，62/62 SUCCESS，耗时 10:51，结束于 `2026-09-09T15:25:12+08:00`。独立 `jdk17/` XML 归档为 53 suites / 185 tests / 0 failures / 0 errors / 11 skipped；所有模块均重新执行 clean/test。

修复后的 Java 21 使用同一组 POM 重新执行完整 `clean verify -Denforcer.skip=true`，返回 0，62/62 SUCCESS，耗时 13:33，结束于 `2026-09-09T15:39:08+08:00`。独立 `jdk21/` XML 归档同为 53 suites / 185 tests / 0 failures / 0 errors / 11 skipped。

双 JDK 全量完成后，按新增 groupId 规则删除 dependencies POM 中已无消费者的旧 Jackson 管理项；当前发布的 ddd4j-data-crypto POM 未声明 Jackson 扩展，导入的 ddd4j BOM 已管理 `io.github.easy4j:jackson-extension:2.0.x.20260630-SNAPSHOT`。Jackson 模块仅更新 description/Javadoc，继续使用自包含 Jackson 2 序列化实现，未新增运行时依赖。该清理当时以 qrcode/Jackson 依赖树和模块 clean test 补验，以上历史双 JDK 日志对应清理前状态；此后 `597a5b7` 的完整门禁已覆盖全部清理与审查修正。

最后清理后的依赖树返回 0：qrcode 只含 easy4j ZXing；Jackson 模块未增加 Jackson extension 运行时依赖。qrcode/Jackson 的双 JDK `-am clean test` 补验各为 4/4 模块、2 suites / 6 tests / 0 failures / 0 errors / 0 skipped。Java 17 结束于 `15:40:48+08:00`（15.973s），Java 21 结束于 `15:43:34+08:00`（15.749s）；独立归档为 `final-modules-jdk17/`、`final-modules-jdk21/`。全仓已跟踪文件的退役 groupId 扫描为零匹配，结果保存在 `final-retired-group-scan.log`。

## XML skip 原因

以下为修复后 Java 17/21 完整报告共同记录的 10 个 MQ skip。类名前缀均为 `io.ddd4j.quarkus.mq.`，表中列出全部方法及 XML 原始 message，不从 tests 总数减去 skipped。

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

修复后两个 JDK 均另有 1 项：`io.ddd4j.quarkus.auth.security.SecurityQuarkusConfigTest#subjectProviderExposedAsCdiBeanAndRegisteredInSubjectKit`，原始原因 `Module is deprecated since 3.3.1; see docs/MIGRATION-auth-security-to-satoken.md`。首轮失败的 Java 17 未运行到该模块，故其历史部分报告只有 10 个 skip。

## CI 与结构证据

`actionlint .github/workflows/ci.yml` 返回 0；包含非法 `invalid.context` 与空 steps 的临时 workflow 返回 1。静态工作流中 lint 使用 `fail_level: error`、`filter_mode: nofilter`，unit/contract 为 Java 17/21，broker 为 Java 17 的 13 项矩阵。Maven job 均先 setup-java，然后运行安全 settings 配置与空缓存远端解析动作。

配置动作以临时文件解码、校验 server id、0600 权限和原子替换安装 settings，失败时保留旧目标并清除临时文件。Task 4 已有配置成功/失败路径验证记录；Task 5 不将该历史记录当成本次新执行的 shell 测试。

本次扫描目标 POM 和 `.github`：无 Model 4.1.0、`<subprojects>`、`<subproject>`、`3.0.x.20260630-SNAPSHOT`，无 `install-ddd4j`、`sed -i`、`Lint disabled` 匹配。Fix round 1 在 dependencies POM 删除退役的 ZXing/Jackson 管理项，未修改 ci.yml；其后审查单轮修正纠正了历史 BOM/Agroal/Hibernate/SmallRye 说明、License 兼容依赖说明、根 POM 和 CI checkout 陈旧注释，保留实际依赖与工作流行为。

本次 MQTT 测试生成的两个 UUID `.lck` 文件均由 `.gitignore:111` 的 `**/ddd4j-mq-*-tcplocalhost*/` 忽略；文档暂存前 status 只包含六份授权文档，`git diff --check` 返回 0。

## 证据边界

本次日志的非阻塞警告包括：Hibernate GenericGenerator/数据库生成配置弃用，部分模块忽略 datasource/hibernate-orm/flyway 配置，无 JDBC datasource，Quarkus Maven plugin 未启用 extensions。NATS 集成测试使用自有、唯一、可清理的 run-scoped JetStream stream，不再依赖 core NATS fallback；TDMQ 未注入 BrokerPublisher/BrokerSubscriber 时使用测试内存 broker。对应日志和测试结果不构成磁盘型 JetStream 持久化、TDMQ 云服务或所有 broker 生产能力验收。

GitHub-hosted Actions 尚未推送并实际执行验证。Enforcer（本阶段显式 skip）、Native、Dev Mode、deploy、云服务、P5-B–E、master 和生产验收均不在本次完成声明内。GitHub/Codeup push 与 Maven deploy 需要分别授权，未执行。独立克隆的 `origin` 是本机源仓库，不能将其 tracking 状态解释为 GitHub/Codeup 同步证据。

本次原始日志和 XML 归档目录为 `/tmp/ddd4j-p5a-33-clone-task5.cECCPQ`。这是本机临时证据，未作为公开 CI artifact 发布；日志 `remote-consumer.log`、`focused-web.log`、`focused-license.log` 与各自 XML 可用于复核。`focused-license/xml` 包含此前 Web 报告，License 的独立统计只取 auth-license 模块的 2 份 XML。
