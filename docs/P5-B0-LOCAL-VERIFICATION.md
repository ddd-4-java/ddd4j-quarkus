# P5-B0 双分支本地验证证据

日期：2026-09-10（Asia/Shanghai）。范围为 P5-B0 Task 4 与 P3 Task 5 的本地 JVM、结构、依赖和 sample 行为门禁。
规格事实源仍为 feature/4.0.x 的
`docs/superpowers/specs/2026-09-10-p5-b0-dual-branch-release-testcontainers-convergence-design.md`；
本文件是两条分支共享的运行记录，不建立第二套规格。最新源码 HEAD 的 CI、push、
Maven deploy 和发布后空缓存消费尚未执行。

## 已验证源码与完整运行

| 分支 / 源码 HEAD | Java | Maven | Quarkus runtime/BOM/plugin | ddd4j / 本仓 revision | 模型 |
|---|---|---|---|---|---|
| 3.3.x / 91ceb9b | Corretto 17.0.20.1；Microsoft 21.0.12.1 | Maven 3.9.16 | 3.37.4 | 2.0.x.20260630-SNAPSHOT / 3.3.x.20260630-SNAPSHOT | 4.0.0 + modules |
| 4.0.x / fce0e1e | Microsoft 21.0.12.1 | wrapper 4.0.0-rc-6 | 3.38.2 | 3.0.x.20260630-SNAPSHOT / 4.0.x.20260630-SNAPSHOT | 4.1.0 + modules |

每次均串行执行 `./mvnw -B clean verify -Denforcer.skip=true`。没有使用测试跳过参数。
运行前后保存 git HEAD/status、实际工具链及原始日志；每次 clean 之前备份已有 XML，
结束后按模块路径复制新的 Surefire/Failsafe XML。下表只统计各自本次最终报告，不累计重复运行。

| 运行目录 | Reactor | Surefire suites | tests | failures | errors | skipped | Failsafe suites | 耗时 | 完成时间 +08:00 |
|---|---|---:|---:|---:|---:|---:|---:|---|---|
| 33-jdk17 | 61/61 SUCCESS | 61 | 224 | 0 | 0 | 3 | 0 | 最终本地门禁 | 2026-09-10 |
| 33-jdk21 | 61/61 SUCCESS | 61 | 224 | 0 | 0 | 3 | 0 | 最终本地门禁 | 2026-09-10 |
| 40-jdk21 | 61/61 SUCCESS | 61 | 228 | 0 | 0 | 3 | 0 | 最终本地门禁 | 2026-09-10 |

三个 Maven 退出码均为 0。最新结果纳入两分支 auth/MQ sample 行为闭环、auth 双会话
与异常清理，以及 NATS JetStream stream 初始化、幂等保留和隔离清理。3.3 两个 JDK
统计相同；两分支数量差异来自各自版本适配测试，不代表删减共同验收行为。

## 原始证据位置

本机证据根目录：`/tmp/p5b0-task4-gates.MKG9c9/`。三个运行子目录与上表一致，
包含 `head.txt`、`toolchain.log`、`clean-verify.log/.exit`、`xml-before-clean/`、
`xml-final/`、`xml-final/summary.json`、`verified-summary.json`、`structure.json`、
`actionlint.log/.exit`、`diff-check.log/.exit`、运行前后 status。原始日志/XML 属于本机临时证据，
并非已上传的 CI artifact；最新最终门禁汇总以上表为准，不能替代后续 hosted 运行。

3.3/JDK17 的有效依赖树为 `testcontainers-tree-3.8.1.log/.exit`；
3.3/JDK21 与 4.0/JDK21 为 `testcontainers-tree.log/.exit`。
4.0 另含 `easy4j-jackson-tree.log/.exit`、`easy4j-qrcode-tree.log/.exit`、
两份 `easy4j-*-versionless.exit` 及 `retired-regex.log/.exit`。

## 结构与实际依赖门禁

三次 actionlint 均退出 0；完整源码 reactor 的 62 个 POM 分别保持本线模型，
各有 6 个使用 `modules/module` 的聚合 POM，所有已跟踪 POM 均无活动
`subprojects/subproject`。独立远端 consumer 的 Model 4.0.0 不属于 4.0.x 的 62 模块 reactor。
已跟踪文件的退役坐标扫描零匹配，命令为 `git grep -n -E 'io[.]github[.]hiwepy'`，
预期退出码 1；工具错误不能当作零匹配。三次源码/POM未改，运行后 diff 检查通过。

每个 JDK 使用同一完整源码 reactor 命令，不使用已发布 Quarkus 模块替代源码：

```bash
./mvnw -B -Denforcer.skip=true \
  org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree \
  -Dincludes=org.testcontainers -Dverbose
```

三次均返回 0、62/62 SUCCESS；全部 Testcontainers 节点均为 2.0.5：
`testcontainers`、`testcontainers-activemq`、`testcontainers-kafka`、
`testcontainers-localstack`、`testcontainers-pulsar`、`testcontainers-rabbitmq`、
`testcontainers-junit-jupiter`。实际 Surefire XML 的 classpath 独立验证了相同版本。

3.3/JDK17 首次 standalone `dependency:tree` 默认选择旧 plugin 2.8，记录了
“Failed to build parent project”并读取陈旧 shared-Harness 描述符，显示旧 kafka/rabbitmq 依赖。
该日志保留为诊断记录，不作为通过证据。使用本机已存在的 plugin 3.8.1 后，完整源码树和实际
测试 classpath 一致；没有修改 POM、安装项目制品或通过本地发布掩盖差异。

4.0 以相同 Java/Maven 分别运行 Jackson、QR-code 的 `-pl <leaf> -am` 依赖树：
`io.github.easy4j:jackson-extension:jar:3.0.x.20260630-SNAPSHOT:compile`、
`io.github.easy4j:zxing-extension:jar:3.0.x.20260630-SNAPSHOT:compile` 均实际解析成功。
两个叶子均为直接 versionless dependency，版本由 ddd4j 3.0.x BOM 管理。
前置修复为 `738d5f7`，结构门禁文档为 `702a1db`；本次全量运行已覆盖修复。

## 13 行 broker 对比

下表的结果顺序为 3.3/JDK17、3.3/JDK21、4.0/JDK21；`4/0` 表示 4 tests、0 skips，
三次所有 broker 的 failures/errors 都是 0。相同镜像和 readiness 在两条分支列中均适用。

| Broker | 两线镜像 | 3.3 fixture / readiness | 4.0 fixture / readiness | 三次 tests/skips | 行为与边界 |
|---|---|---|---|---|---|
| ActiveMQ | apache/activemq-artemis:2.33.0-alpine | ArtemisContainer / dedicated Artemis ready | 同左 | 4/0；4/0；4/0 | Artemis CORE 往返，非 Classic OpenWire |
| Kafka | confluentinc/cp-kafka:7.7.2 | ConfluentKafkaContainer / dedicated KRaft-bootstrap | 同左 | 4/0；4/0；4/0 | Kafka 往返；类与 Confluent 镜像匹配 |
| RabbitMQ | rabbitmq:3.13-management-alpine | RabbitMQContainer / dedicated broker ready | 同左 | 4/0；4/0；4/0 | AMQP 往返 |
| Pulsar | apachepulsar/pulsar:3.2.0 | PulsarContainer / HTTP clusters response | 同左 | 4/0；4/0；4/0 | Pulsar 往返，类级 skip 已移除 |
| SQS | localstack/localstack:3.8.0 | LocalStackContainer / dedicated ready log | 同左 | 4/0；4/0；4/0 | 本地 AWS 模拟的队列往返 |
| RocketMQ | apache/rocketmq:5.3.2 | FixedHostPortGenericContainer / nameserver + broker 日志，3 分钟总超时 | 同左 | 4/0；4/0；4/0 | 真实 broker 往返；WARMUP 与 CREATED tag 分离 |
| NATS | nats:2.10.22 | GenericContainer / JetStream HTTP health + run-scoped `ORDER.CREATED` stream，1 分钟 | 同左 | 4/0；4/0；4/0 | JetStream 往返；默认 fixture 不创建业务拓扑，具体测试拥有并清理唯一 stream |
| MQTT | eclipse-mosquitto:2.0 | GenericContainer / Mosquitto running 日志，1 分钟 | 同左；另有持久化目录生命周期覆盖 | 4/0；4/0；4/0 | Paho/Mosquitto 往返 |
| MQTT-Mica | eclipse-mosquitto:2.0 | GenericContainer / Mosquitto running 日志，1 分钟 | 同左 | 历史门禁 4/1；当前 Linux CI 要求往返执行 | 仅 macOS arm64 条件 skip；Linux round-trip 必须执行并由 XML 门禁确认测试类非全 skip |
| Redis Stream | redis:7.4-alpine | GenericContainer / redis-cli PING/PONG，1 分钟 | 同左 | 4/0；4/0；4/0 | Stream 往返 |
| ONS | apache/rocketmq:5.3.2 | GenericContainer / nameserver 日志，2 分钟 | 同左 | 4/1；4/1；4/1 | 协议占位/装配；商业往返 skip |
| TDMQ | apachepulsar/pulsar:3.2.0 | GenericContainer / HTTP brokers/health，3 分钟 | PulsarContainer / HTTP clusters response，原生 30 秒 | 4/0；4/0；4/0 | fixture ready + SPI/内存 fallback 往返；非云端或 Pulsar-backed 往返 |
| Disruptor | 无镜像 | 无容器 / 进程内初始化 | 同左 | 4/0；4/0；4/0 | 内存往返 |

每次均为 **13 broker suites / 52 tests / 0 failures / 0 errors / 2 skips**。
MQ core 每次 3 suites / 6 tests。3.3 Harness 每次 2 suites / 19 tests，4.0 Harness
3 suites / 25 tests，均无失败、错误或 skip。MQ 总计分别为 18 suites / 77 tests 和
19 suites / 83 tests，均仅有上述两项 skip，不把重复 upstream 执行累计为额外 broker 覆盖。

两个受控实现差异已有版本/API理由：TDMQ 在 3.3 保留允许的 Generic 协议夹具，
4.0 保留 TC 2.0.5 专用 PulsarContainer 的 standalone、集群响应 readiness 和 advertised URL API；
两种 readiness 均在本次运行中通过，额外 contract 检查了 4.0 原生等待条件。
MQTT 3.3 保持已批准的忽略 Paho UUID 目录策略，4.0 保留原有
`target/mqtt-persistence` 和 `user.dir` 恢复策略，并以五项测试覆盖重复/失败生命周期；
本次未为表面一致性更改两线实现。其余共享 fixture 生产源码对比无差异。

## 每个 skip 的原始原因与恢复条件

三次每个 skip 都是单个方法。以下完整类名适用于各自 XML；tests 总数包含 skips。

| 运行 | 完整类名与方法 | XML 原始原因 | 恢复条件 |
|---|---|---|---|
| 3.3/JDK17、3.3/JDK21 | io.ddd4j.quarkus.auth.security.SecurityQuarkusConfigTest#subjectProviderExposedAsCdiBeanAndRegisteredInSubjectKit | Module is deprecated since 3.3.1; see docs/MIGRATION-auth-security-to-satoken.md | 仅在重新支持旧 Security 模块且补齐契约时恢复；正常迁移目标为 Sa-Token |
| 4.0/JDK21 | io.ddd4j.quarkus.auth.security.SecurityQuarkusConfigTest#subjectProviderExposedAsCdiBeanAndRegisteredInSubjectKit | Module is deprecated since 4.1.0; see docs/MIGRATION-auth-security-to-satoken.md | 同上；保留此分支 XML 的原始版本文字 |
| macOS arm64 | io.ddd4j.quarkus.mq.mqttmica.MicaMqttQuarkusIntegrationTest#shouldPublishAndConsumeOrderCreatedEventEndToEnd | `@DisabledIf("isMacArm64")`：mica-mqtt AIO 的已知平台缺陷 | 修复并验证 macOS arm64 AIO 后移除条件；Linux CI 不再跳过且必须完成真实 round-trip |
| 三次 | io.ddd4j.quarkus.mq.ons.OnsQuarkusIntegrationTest#shouldPublishAndConsumeOrderCreatedEventEndToEnd | ONS 商业协议无 Testcontainers 镜像，对齐 javalin Ddd4jOnsMqIT 先例 | 使用已授权商业 endpoint/凭据/隔离资源，或支持实际商业协议的实现后恢复 |

Docker 本次可用，LocalStack 构造阶段的 Docker assumption 未触发。没有其它 skip。

## 最终本地生产化门禁补充

- 两分支 BOM 对齐门禁覆盖 52 个 reactor 叶子；Testcontainers 依赖和测试 classpath 均为 2.0.5。
- 两分支 auth runner 均完成可执行 JAR 启动/健康验证；NATS 保留 3.3.x 已验证的
  run-scoped JetStream stream 初始化、幂等保留与隔离清理契约。
- 严格 Javadoc 验证覆盖 52 个叶子并核对 53 个 Javadoc 归档；security workflow 的
  本地代码门禁通过，但不替代在线 Dependency-Check、SARIF 上传或 schedule 运行。
- Quarkus 日志门禁记录忽略/未知配置 warning 为 0；4.0.x 保持 Model 4.1.0 + `<modules>`。

## 兼容性与尚未完成的验收

- 所有 Maven 运行显式 `-Denforcer.skip=true`，不声明 Enforcer 或 dependency-check 通过。
- 4.0 仍为 Model 4.1.0 + modules 的可执行兼容模式；不宣称 `<subprojects>` 已获 Quarkus
  workspace 支持。当前门禁确认 Quarkus 忽略/未知配置 warning 为 0；NATS 已改为显式
  run-scoped JetStream 拓扑，不再依赖 core NATS fallback。
- 所有公开 Docker broker 往返的通过，不表示 ONS、腾讯云 TDMQ、AWS 云服务或 JetStream 持久订阅验收。
- 阿里云仓库中 ddd4j 2.0.x/3.0.x 当前 timestamped snapshot 仍缺少已审核的
  Disruptor/Kafka 修复，3.0.x 还缺 COLA BOM 修复；因此最新 ddd4j-quarkus HEAD 的
  Actions、deploy 与远端空缓存消费均保持开放。
- GitHub 当前缺少 `NVD_API_KEY`，且 `master`、`feature/3.3.x`、`feature/4.0.x` 尚未启用
  branch protection；在线 Dependency-Check/SARIF 上传和 default-branch schedule 未验收。
- Native、Dev Mode、扩展 runtime/deployment 产品化和其它 P5-B–E 能力未在本次扩大范围。
