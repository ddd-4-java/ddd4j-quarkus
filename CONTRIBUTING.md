# Contributing to ddd4j-quarkus

ddd4j-quarkus 是 ddd4j 的 Quarkus 适配仓库，能力基线对齐 ddd4j-boot。
欢迎提交 PR、issue 与测试用例。参与前请阅读本指南。

## 仓库结构

| 模块 | 说明 |
| --- | --- |
| `ddd4j-quarkus-bom` / `ddd4j-quarkus-dependencies` | 版本与依赖统一管理（quarkus-bom 3.38.2） |
| `ddd4j-quarkus-ddd` / `ddd4j-quarkus-cache` / `ddd4j-quarkus-web` | 核心深度适配（BuildStep + Recorder / CDI Producer） |
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

```bash
# 编译
./mvnw -B compile

# 编译测试（含 @QuarkusTest 用例的编译验证，不启动容器）
./mvnw -B test-compile -DskipTests

# 全量单元测试（@QuarkusTest；涉及 Testcontainers 的用例需要 Docker）
./mvnw -B verify

# 仅跑指定 Testcontainers 集成测试（CI 的 broker matrix 使用）
./mvnw -B verify -Denforcer.skip=true -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-<broker> -am
```

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
2. `build` — JDK 21 执行 `./mvnw -B verify`；
3. `broker-integration` — 按 broker matrix 执行指定 MQ 模块的 Testcontainers 集成测试，
   不启用容器复用；RocketMQ 与 Pulsar 均为阻塞性任务。
