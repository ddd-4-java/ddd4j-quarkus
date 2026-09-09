# P5-B0 双分支发布与 Testcontainers 收敛设计

> 状态：规格已确认，待实施计划
>
> 规格事实源：`feature/4.0.x`
>
> 适用分支：`feature/3.3.x`、`feature/4.0.x`

## 1. 背景

ddd4j 三条基础快照已经发布到阿里云私有 Maven 仓库：

- `1.0.x.20260630-SNAPSHOT`
- `2.0.x.20260630-SNAPSHOT`
- `3.0.x.20260630-SNAPSHOT`

ddd4j-quarkus 当前维护两条框架线：

| ddd4j-quarkus 分支 | revision | ddd4j | Quarkus | Java | Maven 模型 |
|---|---|---|---|---|---|
| `feature/3.3.x` | `3.3.x.20260630-SNAPSHOT` | `2.0.x.20260630-SNAPSHOT` | `3.37.4` | 17，兼容验证 21 | Maven 3 / Model 4.0.0 / `<modules>` |
| `feature/4.0.x` | `4.0.x.20260630-SNAPSHOT` | `3.0.x.20260630-SNAPSHOT` | `3.38.2` | 21 | Maven 4 / Model 4.1.0 / 暂存 `<modules>` |

P5-A 已建立空缓存上游消费、Web 请求生命周期、License 真实验签、CI 安全解析与 Snowflake workerId 合约。两个分支的 Snowflake 修复已通过本地测试和 GitHub Actions。

本阶段先关闭 Testcontainers 与发布验证差距，再进入 P5-B 扩展产品化。

## 2. 已确认决策

1. 两个 Quarkus 分支在本阶段完成前暂缓 Maven deploy。
2. `feature/3.3.x` 从 Testcontainers `1.20.4` 升级到 `2.0.5`。
3. 两个分支建立同一套 broker 测试能力矩阵，但保留各自 Java、Maven、Quarkus 和 ddd4j 合约。
4. `feature/4.0.x` 不改为 `<subprojects>`；继续使用 Model 4.1.0 与 `<modules>`。
5. 两个分支重新完成本地验证、GitHub Actions、Maven deploy 和发布后空缓存消费。
6. P5-B 在 P5-B0 发布闭环后开始，重点建设真正的 Quarkus extension productization。

## 3. 非目标

- 不修改 `master`。
- 不将 3.3.x 升级到 Maven 4、Java 21 基线或 ddd4j 3.0.x。
- 不将 4.0.x 降级到 Maven 3、Model 4.0.0 或 ddd4j 2.0.x。
- 不在当前 Quarkus 版本下强制使用 `<subprojects>`。
- 不把没有 Java 专用模块的 broker 伪装成官方专用模块。
- 不将 ONS/TDMQ fallback、协议占位或内存总线称为真实云服务验收。
- 不在 P5-B0 中扩建新的业务能力或重构全部 Quarkus 模块。

## 4. Maven 聚合模型

`feature/4.0.x` 保持：

```xml
<modelVersion>4.1.0</modelVersion>
<modules>
    ...
</modules>
```

原因：Quarkus `3.38.2` 的 WorkspaceLoader 不能可靠解析 Model 4.1.0 的 `<subprojects>` 聚合结构，强制切换会使 `@QuarkusTest` 启动失败。该状态记录为“上游阻塞下的 Maven 4 可执行兼容模式”，不称为 Maven 4 完整模型验收。

P5-B0 的结构门禁必须确认：

- 3.3.x：Model 4.0.0 + `<modules>`。
- 4.0.x：Model 4.1.0 + `<modules>`。
- 两个分支都不存在活动 `<subprojects>` 或 `<subproject>` 元素。

## 5. Testcontainers 版本治理

### 5.1 统一版本

两个分支统一：

```xml
<testcontainers-bom.version>2.0.5</testcontainers-bom.version>
```

Testcontainers BOM 必须在可能管理旧 Testcontainers 版本的 ddd4j BOM 之前导入，保证 Maven first-declaration-wins 下实际解析为 `2.0.5`。

具体模块不声明 Testcontainers 数字版本；版本只来自 BOM。

### 5.2 Java 专用模块

以下 broker 使用 Testcontainers 2.x Java 专用 artifact 与 Container 类：

| 能力 | Maven artifact | Java Container |
|---|---|---|
| ActiveMQ Artemis | `testcontainers-activemq` | `ArtemisContainer` |
| Kafka | `testcontainers-kafka` | `KafkaContainer` |
| AWS/SQS | `testcontainers-localstack` | `LocalStackContainer` |
| Pulsar | `testcontainers-pulsar` | `PulsarContainer` |
| RabbitMQ | `testcontainers-rabbitmq` | `RabbitMQContainer` |

JUnit 5 集成使用：

```xml
<artifactId>testcontainers-junit-jupiter</artifactId>
```

### 5.3 允许 GenericContainer 的场景

以下场景没有满足本项目要求的 Testcontainers 2.x Java 官方专用模块，保留受控 `GenericContainer`：

- RocketMQ
- NATS（Java 目录项是社区模块，不作为本项目强制依赖）
- MQTT/Mosquitto（官方目录没有 Java 专用模块）
- Redis Stream
- MQTT-Mica
- ONS/TDMQ 的协议测试夹具

每个 GenericContainer fixture 必须固定：

- Docker 镜像和版本。
- 暴露端口。
- 启动命令。
- 技术相关的等待策略。
- 启动超时。
- 确定性 stop/cleanup。

不得仅以端口打开作为所有 broker 的统一就绪条件。

## 6. Broker 测试能力矩阵

两个分支使用相同的 13 broker 行，并为每行记录四种证据：

| Broker | Fixture 类型 | 启动证据 | 行为证据 | 允许的边界 |
|---|---|---|---|---|
| ActiveMQ | 专用 | 容器 ready | publish/consume round-trip | Artemis CORE，不称为 Classic OpenWire |
| Kafka | 专用 | bootstrap ready | round-trip | 固定镜像模式 |
| RabbitMQ | 专用 | broker ready | round-trip | 管理端镜像仅用于 readiness |
| Pulsar | 专用 | admin/broker ready | round-trip 或明确 skip | 3.3.x 现有启动竞态需重新验证 |
| SQS | 专用 LocalStack | SQS endpoint ready | queue round-trip | 本地 AWS 模拟，不是 AWS 云验收 |
| RocketMQ | Generic | namesrv + broker ready | round-trip 或明确 skip | 单容器双进程必须有双 readiness |
| NATS | Generic | server ready | round-trip | 不宣称 Java 官方模块 |
| MQTT | Generic | Mosquitto ready | round-trip | Paho UUID 锁目录必须被忽略/清理 |
| MQTT-Mica | Generic | endpoint ready | round-trip 或平台 skip | skip 必须记录 OS/架构原因 |
| Redis Stream | Generic | Redis PING ready | round-trip | 启用所需事件配置 |
| ONS | Generic/占位 | 协议端点占位 | 装配验证，真实 round-trip skip | 商业协议无本地真实服务 |
| TDMQ | Generic/占位 | fixture ready | SPI/内存 fallback | 不称为腾讯云验收 |
| Disruptor | 无容器 | 进程内初始化 | round-trip | 明确标记为内存实现 |

矩阵中的每个 skip 必须包含测试类、测试方法、原始原因和恢复条件，不能只统计数量。

## 7. Testcontainers 共享 Harness

`ddd4j-quarkus-mq-testcontainers` 继续作为唯一共享测试 Harness，负责：

- `AbstractTestContainerFixture` 的幂等 start/stop。
- 失败启动后的部分资源回收。
- Quarkus test resource 配置注入。
- Docker 不可用时的显式 skip。
- 容器复用的所有权边界。
- 镜像、端口、等待策略和连接属性的集中管理。

专用 Container 类只能替代容器启动细节，不能绕过共享生命周期、配置注入和 round-trip 骨架。

## 8. 双分支验证门禁

### 8.1 feature/3.3.x

- Java 17：完整 `clean verify`。
- Java 21：兼容性 `clean verify`。
- Quarkus runtime/plugin：`3.37.4`。
- Testcontainers effective version：`2.0.5`。
- 13 broker 矩阵执行或按规则 skip。

### 8.2 feature/4.0.x

- Java 21：完整 `clean verify`。
- Quarkus runtime/plugin：`3.38.2`。
- Testcontainers effective version：`2.0.5`。
- Maven 4 Model 4.1.0 + `<modules>`。
- 13 broker 矩阵执行或按规则 skip。

### 8.3 共同门禁

- actionlint 成功。
- 已退役的前序扩展 group 坐标零引用；扩展统一使用 `io.github.easy4j`。
- Surefire/Failsafe XML 汇总 failures/errors 为 0。
- 每个 skip 逐项解释。
- 本地、GitHub、Codeup 分支 SHA 一致。
- GitHub Actions 对目标 HEAD 成功。
- 不用既有 `~/.m2` 或前一分支缓存替代发布消费证据。

## 9. 发布顺序

严格按以下顺序执行：

1. 完成 3.3.x Testcontainers 2.0.5 迁移与本地验证。
2. 核对 4.0.x 与统一 broker 矩阵的一致性。
3. 推送 3.3.x、4.0.x 到 GitHub 与 Codeup。
4. 等待两个目标 HEAD 的 GitHub Actions 成功。
5. deploy `feature/3.3.x`。
6. 从全新 Maven 仓库消费 3.3.x 发布物。
7. deploy `feature/4.0.x`。
8. 从全新 Maven 仓库消费 4.0.x 发布物。

任一步失败即停止后续发布，不把部分上传称为 deploy 成功。

## 10. 发布后空缓存消费

每条分支建立独立 consumer fixture，从全新 Maven 本地仓库解析：

- `ddd4j-quarkus-bom`
- `ddd4j-quarkus-dependencies`
- `ddd4j-quarkus-parent`
- `ddd4j-quarkus-web`
- `ddd4j-quarkus-data-panache`
- `ddd4j-quarkus-auth-license`
- `ddd4j-quarkus-mq-testcontainers`
- 至少一个专用 Container broker 模块
- 至少一个 GenericContainer broker 模块

consumer 必须使用分支对应的 Java/Maven 工具链，不允许 reactor `-am` 或源码相对路径参与解析。

## 11. P5-B 后续边界

P5-B0 发布闭环后，P5-B 参考 ddd4j-boot 建设真正的 Quarkus extension productization：

- runtime 与 deployment artifact 分离。
- build-time processor 与 build item。
- 配置映射、条件装配和扩展元数据。
- Dev Mode、augmentation、Native Image 与消费项目验证。
- 能力由 Quarkus 扩展发现和构建期装配，而不是继续堆叠普通 CDI 适配类。

P5-B 必须另行编写实施计划，不与本阶段发布修复混合。

## 12. 完成定义

P5-B0 完成需要同时满足：

- 3.3.x 和 4.0.x 均实际解析 Testcontainers 2.0.5。
- 专用模块与 GenericContainer 使用符合第 5 节边界。
- 双分支 broker 矩阵有真实运行证据。
- 双分支本地门禁和 GitHub Actions 成功。
- 双分支 deploy 完整成功。
- 双分支发布物通过独立空缓存消费。
- 4.0.x 保持 Model 4.1.0 + `<modules>`，没有虚假 Maven 4 完整验收声明。
- P5-B 产品化仍作为下一阶段，不被本阶段状态覆盖。
