# ddd4j-boot ↔ ddd4j-quarkus 能力对齐矩阵

> 对齐基线：`ddd4j-boot` 3.4.x.20260630（Spring Boot 3.4.13 / Java 17 / ddd4j 2.0.x）
> 对照轨：`ddd4j-quarkus` feature/3.3.x（Quarkus 3.37.4，ddd4j 2.0.x）与 feature/4.0.x（Quarkus 3.38.2，ddd4j 3.0.x）
> 更新日期：2026-09-02

## 1. 模块映射总表

| ddd4j-boot 模块 | ddd4j-quarkus 对应 | 状态 | 说明 |
|---|---|---|---|
| ddd4j-boot-core | （收敛至主仓 `io.ddd4j:ddd4j-runtime-quarkus`） | ✅ 架构等价 | Quarkus 轨核心运行时收敛主仓，本仓不做平行实现 |
| ddd4j-boot-cache | ddd4j-quarkus-cache | ✅ 已对齐 | Boot=AutoConfiguration+`CacheProperties`；Quarkus=`@BuildStep`+`CacheRecorder`（全仓唯一真 build-time 扩展） |
| ddd4j-boot-data-crypto / -datascope / -external / -logs | ddd4j-quarkus-data-{crypto,datascope,external,logs} | ✅ 已对齐 | 纯 CDI Producer 桥接主仓纯 Java 实现 |
| ddd4j-boot-data-mybatis | ddd4j-quarkus-data-panache + data-jpa | ✅ 架构等价 | Quarkus 轨 ORM 选型 Panache（多租户实体基类/雪花ID/查询辅助），不复刻 MyBatis 形态 |
| （无对应） | ddd4j-quarkus-data-event-store-panache | ⭐ Quarkus 独有 | EventStore SPI 关系型落地（镜像主仓 panache 实现） |
| ddd4j-boot-auth-license / -satoken / -security / -shiro | ddd4j-quarkus-auth-{license,satoken,security,shiro} | ✅ 已对齐 | security 已 `@Deprecated(forRemoval)`（推荐 satoken） |
| （无对应） | ddd4j-quarkus-auth-jwt | ⭐ Quarkus 独有 | SmallRye JWT 无状态验证（Quarkus 原生生态） |
| ddd4j-boot-mq-core + 13 broker | ddd4j-quarkus-mq-core + 13 broker + mq-testcontainers | ✅ 已对齐 | broker 全部薄 CDI Producer 复用主仓 `ddd4j-mq-*`；Quarkus 独有集中式 testcontainers fixture 模块 |
| ddd4j-boot-extension-{akka,cola,dubbo,excel,jackson,monitor,qlexpress,qrcode} | ddd4j-quarkus-extension-{同 8 个} | ✅ 已对齐 | 逐一对应；jackson 为自包含 NullTolerant 实现（上游 easy4j/hiwepy 不跟发） |
| ddd4j-boot-extension-pf4j | （缺） | ⚠️ 双侧均未实现 | **boot 侧为空壳目录（无 pom 无 src）**；主仓各线无 `ddd4j-extension-pf4j` 源码；pf4j 插件能力由 `easy4j-pf4j-extension` 承载。quarkus 无落后项 |
| ddd4j-boot-auth-datascope（位于 extensions 聚合下） | ddd4j-quarkus-data-datascope（位于 data 聚合下） | ✅ 归类差异 | 功能等价，归类遵循各自领域边界 |
| （无对应） | ddd4j-quarkus-extension-validation | ⭐ Quarkus 独有 | Bean Validation 文件上传约束校验（替代 boot 生态对应能力） |
| ddd4j-boot-web-{webmvc,webflux,javalin,vertx} | ddd4j-quarkus-web | ✅ 架构等价 | Quarkus 单一 REST 栈（RESTEasy Reactive），无多 Web 栈分包的架构前提；过滤器/异常映射/健康检查语义对齐 boot-web-webmvc |

## 2. 架构性不适用项（非缺失）

| boot 能力 | Quarkus 处置 | 原因 |
|---|---|---|
| web 四栈分包（webmvc/webflux/javalin/vertx） | 单一 `ddd4j-quarkus-web` | Quarkus HTTP 层单一实现（RESTEasy Reactive + Vert.x），多容器分包无意义 |
| data-mybatis（MyBatis 适配） | data-panache / data-jpa | Quarkus 生态 ORM 主线为 Hibernate ORM + Panache |
| sample-starter-druid-* / hikaricp-*（13 个连接池 starter） | `ddd4j-quarkus-parent` 的 `ddd4j-data` profile（quarkus-agroal + jdbc-mysql） | Quarkus 连接池统一 Agroal，无多池选型场景 |
| sample-starter-r2dbc-webflux | 无（P2-7 评估推迟） | 无业务需求证据，见 `docs/P2-7-mongo-r2dbc-feasibility.md` |
| `AutoConfiguration.imports` 自动发现 | `META-INF/quarkus-extension.yaml` + Jandex 索引 + `@IfBuildProperty`/`@DefaultBean` | Spring SPI → Quarkus build-time augmentation 的标准转换 |
| ApplicationContextRunner 契约测试 | `@QuarkusTest` + CDI 注入断言 | 容器模型不同，契约语义等价 |

## 3. ddd4j-quarkus 独有能力（boot 无对应）

| 模块/机制 | 说明 |
|---|---|
| ddd4j-quarkus-data-panache | 多租户实体基类（tenantId+雪花ID 复合主键）、IdGenerationStrategy 构建期三选一（snowflake/auto-increment/uuid） |
| ddd4j-quarkus-data-event-store-panache | EventStore SPI 生产实现（3.3.x/4.0.x 双线一致） |
| ddd4j-quarkus-data-jpa | EntityManager CDI 桥接 + RepositoryRegistry 启动注册 |
| ddd4j-quarkus-auth-jwt | SmallRye JWT（无状态验证、非请求线程兜底） |
| ddd4j-quarkus-extension-validation | 文件上传 Bean Validation 约束校验器 |
| ddd4j-quarkus-ddd | EntityId 白名单安全注册（Class.forName 加固：类名校验+前缀白名单+禁 setAccessible） |
| ddd4j-quarkus-mq-testcontainers | 13 broker 集中式 testcontainers fixture + QuarkusTestResource 桥接 |
| cache BuildStep/Recorder | 全生态唯一真 Quarkus build-time 扩展范式 |

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

### 4.2 端到端 round-trip 覆盖

- 对齐 boot 的 10 broker `*ClientIntegrationTest` 与 javalin 的 `AbstractMqIntegrationTest.shouldPublishAndConsumeRoundTrip` 模式
- 13 broker：11 个真实往返 + ons/tdmq 按 javalin 先例 `@Disabled`（商业协议无 Testcontainers 镜像）+ mqtt-mica `@Disabled`（macOS arm64 AIO 缺陷，CI linux 可移除）
- payload 使用 DDD 业务事件（OrderCreated 模式），不再使用空 PingEvent

### 4.3 javalin 独有、quarkus 暂缓的 fixture（记录）

postgres/mysql/mariadb/mongodb/keycloak/wiremock fixture 仅 javalin 有；quarkus 轨数据层走 DevServices（H2）+ 主仓 IT，待业务需求出现再补。

## 5. CI 能力对齐（对照 boot verify.yml）

| boot 能力 | quarkus 现状 | 处置 |
|---|---|---|
| reviewdog/action-actionlint lint | echo 占位 | 可选后续引入 |
| scripts/consistency/*.sh 双分支一致性 | 无 | 可选后续引入（quarkus 双分支结构下高价值） |
| coverage-gate（LINE≥90%） | 无 | 可选后续引入 |
| SBOM/license/CVE 报告 | 无 | 可选后续引入 |
| 13 broker matrix + 无 continue-on-error | ✅ 已领先 boot | — |
| composite action 复用 install | ✅ 已领先 boot | — |
| `MAVEN_SETTINGS_XML` org secret | ✅ 引用正确；secret 缺失 fail-fast 加固中 | 见 Phase E |
