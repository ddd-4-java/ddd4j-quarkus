# Contributing to ddd4j-quarkus

ddd4j-quarkus 是 ddd4j 的 Quarkus 适配仓库，能力基线对齐 ddd4j-boot。
欢迎提交 PR、issue 与测试用例。参与前请阅读本指南。

## 仓库结构

| 模块 | 说明 |
| --- | --- |
| `ddd4j-quarkus-bom` / `ddd4j-quarkus-dependencies` | 版本与依赖统一管理（quarkus-bom 3.38.2） |
| `ddd4j-quarkus-ddd` / `ddd4j-quarkus-cache` / `ddd4j-quarkus-web` | 核心适配；标准 runtime/deployment/IT 产品化属于 P5-B |
| `ddd4j-quarkus-data` | 数据访问（panache / jpa / external） |
| `ddd4j-quarkus-mq` | MQ 适配（core + 13 broker + testcontainers fixtures） |
| `ddd4j-quarkus-auth` | 认证授权（jwt / satoken / shiro / **[security (deprecated, use satoken)]** / license） |
| `ddd4j-quarkus-extensions` | 业务扩展（akka / cola / excel / jackson / monitor / qlexpress / dubbo） |
| `ddd4j-quarkus-parent` | 业务项目父 POM（默认依赖 + profiles） |
| `ddd4j-quarkus-samples` | 分层架构示例（domain / app / infrastructure / adapter / client / common / layered 等） |

## 开发环境

- JDK 21（编译基线 + CI）
- Maven 4.0.0-rc-6（仓库自带 `./mvnw`）
- Docker（运行 `@QuarkusTest` 中的 Testcontainers 集成测试）

## 构建与测试

先按 [README 的空缓存命令](README.md#p5-a-发布依赖与本地构建) 验证已发布的
ddd4j `3.0.x.20260630-SNAPSHOT`。该步骤只依赖配置好的远端 Maven settings，
无需检出或修改 ddd4j 源码。独立消费夹具使用 Model 4.0.0，主 reactor 保持 Model 4.1.0。

```bash
# 编译
./mvnw -B compile

# 编译测试（含 @QuarkusTest 用例的编译验证，不启动容器）
./mvnw -B test-compile -DskipTests

# 完整 JVM reactor（包含 @QuarkusTest 与需要 Docker 的 Testcontainers 用例）
./mvnw -B clean verify -Denforcer.skip=true

# Web 与 License 关键消费契约，分别执行
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-web -am test
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-auth/ddd4j-quarkus-auth-license -am test

# 仅跑指定 Testcontainers 集成测试（CI 的 broker matrix 使用）
./mvnw -B verify -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-<broker> -am
```

`-Denforcer.skip=true` 的成功结果不代表 Enforcer 通过。测试统计必须来自
`target/surefire-reports/TEST-*.xml` 与 `target/failsafe-reports/TEST-*.xml`，
分别列出 tests/failures/errors/skipped，并保留 skip 类与原因。TDMQ 的测试内存 fallback
不计为腾讯云服务验收；Native、Dev Mode 与 3.3.x 维护线需要各自的运行证据。

2026-09-09 P5-A 已观察：空仓远端解析成功、Web 4 tests、License 3 tests；完整
`clean verify` 为 62/62 模块成功，最终 47 Surefire suites、141 tests、0 failures、
0 errors、3 skipped，0 Failsafe suites。跳过类、原因及剩余警告统一记录在
[P5-A 完成证据](docs/superpowers/specs/2026-09-08-quarkus-production-extension-convergence-design.md#9-p5-a-本地完成证据2026-09-09)。

### P5-B0 本地门禁（2026-09-10）

`fce0e1e` 的 Java 21 / Maven 4 全量通过 61/61 模块、61 suites / 228 tests /
0 failures / 0 errors / 3 skips；3.3.x `91ceb9b` 在 Java 17/21 下各为 61 suites /
224 tests / 0 failures / 0 errors / 3 skips。完整源码依赖树使用
`org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree -Dincludes=org.testcontainers -Dverbose`，
全部 Testcontainers 为 2.0.5；Jackson/ZXing 叶子无显式版本，实际从 ddd4j 3.0.x BOM
解析 Easy4J 扩展。逐项 skip、13 broker 对比和限制见 [本地验证证据](docs/P5-B0-LOCAL-VERIFICATION.md)。
该证据还包含 BOM 52 个叶子、auth runner、NATS、严格 Javadoc（52 个叶子、53 个归档）、
security 代码门禁和 Quarkus 配置 warning=0；远端 Actions、发布及空缓存消费仍是独立门禁。
CI、Enforcer、发布与发布后空缓存消费仍须各自取得证据。

### 容器生命周期

- 共享 fixture 启动并关闭自己拥有的容器，CI 不启用实验性的 reusable containers。
- 本地如需复用，由开发者在 Testcontainers 用户配置中显式开启；测试代码不得强制
  `withReuse(true)`，也不得依赖上一次执行残留的容器。
- 测试运行目录写入模块的 `target/`，不得在源码目录生成随机持久化目录。

## 编码约定

1. **不复制主仓实现**：底层能力一律复用 `io.ddd4j:*`（ddd4j-core / cache / mq / auth / data / web-core），
   本仓库只写 Quarkus 适配层（CDI Producer / ConfigMapping / BuildStep / ExceptionMapper）。
2. **Quarkus 风格**：配置用 `@ConfigMapping`（替代 Spring `@ConfigurationProperties`），
   装配用 `@Produces @Singleton` + `@IfBuildProperty`（替代 Spring `@AutoConfiguration`），
   异常处理用 JAX-RS `ExceptionMapper`（替代 `@ControllerAdvice`）。
3. **日志**：使用 `org.jboss.logging.Logger`，占位符风格为 `infof("...%s", arg)`。
4. **测试**：每个 starter 至少 1 个 `@QuarkusTest`；涉及外部组件的用例走 Testcontainers 2.0.5
   fixture（复用 `ddd4j-quarkus-mq-testcontainers` / samples 中的模式）。
5. **版本**：新依赖必须进入 `ddd4j-quarkus-dependencies` 的 dependencyManagement，
   禁止在子模块写裸版本号；Quarkus 组件跟随 quarkus-bom，禁止覆盖版本。

## 提交规范

- 分支：基于 `feature/4.0.x` 创建 `feature/<topic>` 分支，PR 合入 `feature/4.0.x`。
- 提交信息：参考 [Conventional Commits](https://www.conventionalcommits.org/)，
  如 `feat(mq): align QuarkusMQListenerRegistrar with MQClient.init contract`。
- 单 PR 控制改动量（≤ 500 行），便于审查。

## CI

`.github/workflows/ci.yml` 三阶段：

1. `workflow-lint` — actionlint 校验 workflow 语法；
2. `unit-and-contract` — 配置 Maven、空缓存解析已发布 ddd4j，然后 JDK 21 执行 `./mvnw -B verify -Denforcer.skip=true`；
3. `broker-integration` — 按 broker matrix 执行指定 MQ 模块的 Testcontainers 集成测试，
   不启用容器复用；RocketMQ 与 Pulsar 均为阻塞性任务。

两个 Maven job 均复用 `configure-maven`，缺失 `MAVEN_SETTINGS_XML` 时 fail-fast；
settings 经临时文件验证、`0600` 权限及原子替换后使用。broker matrix 保留 13 个条目、
`fail-fast: false` 和每个 broker 的报告上传。CI 不再安装或改写 ddd4j 检出源码。
本地 actionlint 和 Maven 结果不等于 GitHub Actions 运行成功；push、deploy 与远端验证需分别授权和记录。
