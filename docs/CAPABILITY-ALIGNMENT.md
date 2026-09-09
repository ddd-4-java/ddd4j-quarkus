# ddd4j-boot ↔ ddd4j-quarkus 能力对齐矩阵

> 对齐基线：`ddd4j-boot` 3.4.x.20260630（Spring Boot 3.4.13 / Java 17 / ddd4j 2.0.x）
> 对照轨：`ddd4j-quarkus` feature/3.3.x（Quarkus 3.37.4，ddd4j 2.0.x）与 feature/4.0.x（Quarkus 3.38.2，ddd4j 3.0.x）
> 更新日期：2026-09-10；新增 P5-B0 双分支本地验证，以下 P5-A 数据保留为历史记录。

状态按证据划分：`已验证` 限定到明确的行为与运行模式；`部分完成` 表示已有适配但仍有
契约缺口；`占位` 表示只有描述/POM/fallback；`未验证` 表示缺少对应运行证据；
`上游阻塞` 表示受外部实现限制。模块映射、CDI 注入和构建通过均不等于生产能力全量对齐。
后续范围以 [P5 设计](superpowers/specs/2026-09-08-quarkus-production-extension-convergence-design.md) 为准。

最新 P5-B0 本地验证：4.0.x `702a1db` 的 Java 21 完整 reactor 为 62/62 SUCCESS、
51 suites / 170 tests / 0 failures / 0 errors / 3 skips；3.3.x `c37c843` 的 Java 17/21
各为 57 suites / 211 tests / 0 failures / 0 errors / 3 skips。两线 Testcontainers 全部
2.0.5，13 broker 的行为与差异、skip 恢复条件和结构证据见 [P5-B0 本地验证](P5-B0-LOCAL-VERIFICATION.md)。
该本地结果尚未包含目标 HEAD 的 hosted CI、Maven 发布和发布后空缓存消费。

历史 P5-A 本地完成，P5-B–E 待实施。2026-09-09 新空仓远端解析成功，`clean verify`
62/62 模块成功；最终 XML 为 47 Surefire suites、141 tests、0 failures、0 errors、
3 skipped，0 Failsafe suites。跳过类、原因和已知警告见
[P5-A 完成证据](superpowers/specs/2026-09-08-quarkus-production-extension-convergence-design.md#9-p5-a-本地完成证据2026-09-09)。
Maven 4 `subprojects` 仍为上游阻塞；本轮没有 push/deploy 或 GitHub Actions 运行证据。

## Snowflake 共享修复补充（2026-09-09）

3.3.x GitHub Actions run `34331450813` 因 IP 派生 workerId 越界失败；4.0.x 原有同样
实现，本轮通过本地 RED 重现。该 run 不属于 4.0.x，当前没有本修复的 4.0.x hosted
运行证据，两分支仍须各自重跑 Actions，历史 P5-A 全量成功不能替代这一门禁。

4.0.x 修复提交 `fd94b18` 移植已评审的 3.3.x `a5add959` 契约：可选配置
`ddd4j.quarkus.data.snowflake.worker-id`；缺省 IP 归一化至 `0..31`；显式合法编号
原值使用，非法编号创建策略时拒绝。多节点部署须分配不重复的显式编号，缺省映射不保证
跨机器唯一。配置影响编程式 `SnowflakeIdStrategy` 和 CDI producer，未改 ORM 独立生成器。

Java `21.0.12.1` / Maven `4.0.0-rc-6` 下新增六项行为 RED 为 6 failures / 0 errors，
GREEN 为 6 tests 全通过；完整 data-panache `-am clean verify` 为 3/3 模块成功、
6 suites / 17 tests / 0 failures / 0 errors / 0 skipped（12.032 秒）。日志：
`/private/tmp/ddd4j-40x-snowflake-module-final-j21.log`；完整报告见 Git 忽略的
`.superpowers/sdd/2026-09-09-snowflake-worker-id/snowflake-40-report.md`。
未重跑 62 模块、空仓消费、Native、Dev Mode 或 hosted Actions，未 push/deploy。
Java 17 不是本分支支持基线：POM 为 Java 21，实际 `IdKit` 字节码 major=65，未降级验证。
已有 effective-model、wrapper settings、只读 resources 与未配置 datasource 警告仍存在，
Enforcer 和 dependency-check 未作为本轮通过门禁。

## 1. 模块映射总表

| ddd4j-boot 模块 | ddd4j-quarkus 对应 | 状态 | 说明 |
|---|---|---|---|
| ddd4j-boot-core | 主仓 `io.ddd4j:ddd4j-runtime-quarkus` | 部分完成 | P5-A 验证远端可解析；标准扩展发现、Dev Mode、Native 属于 P5-B/E |
| ddd4j-boot-cache | ddd4j-quarkus-cache | 部分完成 | 已有适配；CacheKit 行为及 runtime/deployment/IT 边界待 P5-B |
| ddd4j-boot-data-crypto / -datascope / -external / -logs | ddd4j-quarkus-data-{crypto,datascope,external,logs} | 部分完成 | external/logs 装配、datascope 默认与业务覆盖契约待 P5-C |
| ddd4j-boot-data-mybatis | ddd4j-quarkus-data-panache + data-jpa | 部分完成 | ORM 采用 Panache/JPA；Repository 按聚合根类型注册及关闭清理待 P5-C |
| （无对应） | ddd4j-quarkus-data-event-store-panache | 部分完成 | 已有行为测试；与主仓确定唯一实现所有权待 P5-C |
| ddd4j-boot-auth-license | ddd4j-quarkus-auth-license | 已验证（P5-A JVM） | 真实签发、安装与验签；enabled/disabled profile 与临时夹具隔离共 3 tests；不含 Native |
| ddd4j-boot-auth-{satoken,security,shiro} | ddd4j-quarkus-auth-{satoken,security,shiro} | 部分完成 | 候选 provider、互斥 capability、SPI 生命周期待 P5-D |
| （无对应） | ddd4j-quarkus-auth-jwt | 部分完成 | SmallRye JWT 适配；Auth 组合和生命周期治理待 P5-D |
| ddd4j-boot-mq-core + 13 broker | ddd4j-quarkus-mq-core + 13 broker + mq-testcontainers | 部分完成 | 公开镜像测试、云服务与 fallback 证据分开；required/readiness/关闭与 reload 契约待 P5-D |
| ddd4j-boot-extension-{akka,cola,dubbo,excel,jackson,monitor,qlexpress,qrcode} | ddd4j-quarkus-extension-{同 8 个} | 未验证（完整生产契约） | 保留模块映射；本次不证明各扩展的外部 consumer、业务覆盖、Native 等价性 |
| ddd4j-boot-extension-pf4j | 无对应模块 | 未验证（本次范围外） | 历史盘点未纳入；P5-A 未重新核对跨仓 pf4j 实现 |
| ddd4j-boot-auth-datascope（extensions 聚合） | ddd4j-quarkus-data-datascope（data 聚合） | 部分完成 | 领域归类不同；默认/disabled/custom override 待 P5-C |
| （无对应） | ddd4j-quarkus-extension-validation | 未验证（完整生产契约） | 已有文件上传约束适配；本次不证明外部消费者和 Native 能力 |
| ddd4j-boot-web-{webmvc,webflux,javalin,vertx} | ddd4j-quarkus-web | 已验证（P5-A JVM 契约） | 4 tests：健康、主仓 Filter 发现、tenant/request-id、同线程响应后 tenant/request-id/Authorization 清理；标准扩展产品化待 P5-B |

## 2. 架构性不适用项（非缺失）

| boot 能力 | Quarkus 处置 | 原因 |
|---|---|---|
| web 四栈分包（webmvc/webflux/javalin/vertx） | 单一 `ddd4j-quarkus-web` | Quarkus HTTP 层单一实现（RESTEasy Reactive + Vert.x），多容器分包无意义 |
| data-mybatis（MyBatis 适配） | data-panache / data-jpa | Quarkus 生态 ORM 主线为 Hibernate ORM + Panache |
| sample-starter-druid-* / hikaricp-*（13 个连接池 starter） | `ddd4j-quarkus-parent` 的 `ddd4j-data` profile（quarkus-agroal + jdbc-mysql） | Quarkus 连接池统一 Agroal，无多池选型场景 |
| sample-starter-r2dbc-webflux | 无（P2-7 评估推迟） | 无业务需求证据，见 `docs/P2-7-mongo-r2dbc-feasibility.md` |
| `AutoConfiguration.imports` 自动发现 | P5-B 目标：runtime/deployment 描述符、Jandex 和默认 Bean 覆盖 | 现有普通 JAR/CDI 不能直接作为标准扩展验收 |
| ApplicationContextRunner 契约测试 | `@QuarkusTest` + 外部可观察行为断言 | 注入断言只证明装配；默认、disabled、业务覆盖与生命周期需分别验证 |

## 3. ddd4j-quarkus 独有能力（boot 无对应）

| 模块/机制 | 说明 |
|---|---|
| ddd4j-quarkus-data-panache | 多租户实体基类（tenantId+雪花ID 复合主键）、IdGenerationStrategy 构建期三选一（snowflake/auto-increment/uuid） |
| ddd4j-quarkus-data-event-store-panache | EventStore SPI 适配；P5-A 未验证 3.3.x/4.0.x 双线一致性 |
| ddd4j-quarkus-data-jpa | EntityManager CDI 桥接；注册正确性属于 P5-C 待办 |
| ddd4j-quarkus-auth-jwt | SmallRye JWT（无状态验证、非请求线程兜底） |
| ddd4j-quarkus-extension-validation | 文件上传 Bean Validation 约束校验器 |
| ddd4j-quarkus-ddd | EntityId 白名单安全注册（Class.forName 加固：类名校验+前缀白名单+禁 setAccessible） |
| ddd4j-quarkus-mq-testcontainers | 13 broker 集中式 testcontainers fixture + QuarkusTestResource 桥接 |
| cache BuildStep/Recorder | 已有构建期适配形态；标准 runtime/deployment/IT 结构待 P5-B |

## 4. 测试基建对齐（testcontainers）

### 4.1 镜像版本统一（对齐 boot，与 javalin 协同）

| broker | quarkus 原值 | 统一后 | 备注 |
|---|---|---|---|
| activemq | activemq-classic:5.18.3 | **activemq-artemis:2.33.0-alpine** | 协议修复：主仓客户端为 artemis-jakarta-client |
| kafka | cp-kafka:7.6.1 | cp-kafka:7.7.2 | 对齐 boot |
| rocketmq | 5.3.0（仅 namesrv） | 5.3.2（namesrv+broker） | round-trip 需要 broker |
| nats | 2.10-alpine | 2.10.22 | 对齐 boot |
| sqs(localstack) | 3.4 | 3.8.0 | 对齐 boot |
| rabbitmq / pulsar / mqtt / redis | 3.13-management-alpine / 3.2.0 / 2.0 / 7.4-alpine | 保持 | 已与 boot 一致或非歧义 |

### 4.2 Testcontainers 2.0.5 与端到端 round-trip 覆盖

- 对齐 boot 的 10 broker `*ClientIntegrationTest` 与 javalin 的 `AbstractMqIntegrationTest.shouldPublishAndConsumeRoundTrip` 模式
- ActiveMQ Artemis、Kafka、RabbitMQ、Pulsar、SQS 分别使用 Testcontainers 2.0.5 官方 `ArtemisContainer`、`ConfluentKafkaContainer`、`RabbitMQContainer`、`PulsarContainer`、`LocalStackContainer`
- 2026-09-07 的 4.0.x 全量验证中，公开镜像 broker 往返均通过；ONS 云服务往返跳过，MQTT-Mica 在 macOS arm64 跳过，TDMQ 使用测试专用内存 fallback，不能作为云服务验收
- RocketMQ 与 Pulsar 类级 `@Disabled` 已移除，真实公开镜像往返通过；共享 fixture 不再强制容器复用并会关闭自有容器
- payload 使用 DDD 业务事件（OrderCreated 模式），不再使用空 PingEvent

### 4.3 javalin 独有、quarkus 暂缓的 fixture（记录）

postgres/mysql/mariadb/mongodb/keycloak/wiremock fixture 仅 javalin 有；quarkus 轨数据层走 DevServices（H2）+ 主仓 IT，待业务需求出现再补。

## 5. CI 能力对齐（对照 boot verify.yml）

| boot 能力 | quarkus 现状 | 处置 |
|---|---|---|
| reviewdog/action-actionlint lint | 已接入且本地 actionlint 通过 | GitHub Actions 运行待单独验证 |
| scripts/consistency/*.sh 双分支一致性 | 无 | 可选后续引入（quarkus 双分支结构下高价值） |
| coverage-gate（LINE≥90%） | 无 | 可选后续引入 |
| SBOM/license/CVE 报告 | 无 | 可选后续引入 |
| 13 broker matrix + 无 continue-on-error | 已配置为阻塞性任务，`fail-fast: false` | 云服务/平台 skips 与内存 fallback 不等于 broker 云端验收 |
| composite action 复用 | `configure-maven` 验证 settings 并从空仓解析已发布 ddd4j | 已移除 ddd4j 源码安装与改写步骤 |
| `MAVEN_SETTINGS_XML` org secret | 缺失 fail-fast；临时文件校验后以 0600 原子替换 | 本地测试不证明 GitHub 组织 secret 当前可用 |
