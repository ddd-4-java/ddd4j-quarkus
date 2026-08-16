# Contributing to ddd4j-quarkus

ddd4j-quarkus 是 ddd4j 的 Quarkus 适配仓库，能力基线对齐 ddd4j-boot。
欢迎提交 PR、issue 与测试用例。参与前请阅读本指南。

## 仓库结构

| 模块 | 说明 |
| --- | --- |
| `ddd4j-quarkus-bom` / `ddd4j-quarkus-dependencies` | 版本与依赖统一管理（quarkus-bom 3.37.x） |
| `ddd4j-quarkus-ddd` / `ddd4j-quarkus-cache` / `ddd4j-quarkus-web` | 核心深度适配（BuildStep + Recorder / CDI Producer） |
| `ddd4j-quarkus-data` | 数据访问（panache / jpa / external） |
| `ddd4j-quarkus-mq` | MQ 适配（core + 13 broker + testcontainers fixtures） |
| `ddd4j-quarkus-auth` | 认证授权（jwt / satoken / shiro / security / license） |
| `ddd4j-quarkus-extensions` | 业务扩展（akka / cola / excel / jackson / monitor / qlexpress / dubbo） |
| `ddd4j-quarkus-parent` | 业务项目父 POM（默认依赖 + profiles） |
| `ddd4j-quarkus-samples` | 分层架构示例（domain / app / infrastructure / adapter / client / common / layered 等） |

## 开发环境

- JDK 17（编译基线）或 21（CI 矩阵）
- Maven 3.9+（仓库自带 `./mvnw`）
- Docker（运行 `@QuarkusTest` 中的 Testcontainers 集成测试）

## 构建与测试

```bash
# 编译
./mvnw -B compile

# 编译测试（含 @QuarkusTest 用例的编译验证，不启动容器）
./mvnw -B test-compile -DskipTests

# 全量单元测试（@QuarkusTest；涉及 Testcontainers 的用例需要 Docker）
./mvnw -B verify

# 仅跑 testcontainers 集成测试（CI 的 infrastructure-integration job 使用）
./mvnw -B verify -Pintegration -pl <mq 模块列表> -am
```

### 容器复用

- 仓库内 `testcontainers.properties`（classpath）与 CI 中 `~/.testcontainers.properties`
  均开启 `testcontainers.reuse.enable=true`，本地与 CI 都会复用已启动的 broker 容器。
- 需要临时关闭复用：`-Dtestcontainers.reuse.enable=false`。
- 清理残留容器：`docker rm -f $(docker ps -aq --filter "label=testcontainers")`。

## 编码约定

1. **不复制主仓实现**：底层能力一律复用 `io.ddd4j:*`（ddd4j-core / cache / mq / auth / data / web-core），
   本仓库只写 Quarkus 适配层（CDI Producer / ConfigMapping / BuildStep / ExceptionMapper）。
2. **Quarkus 风格**：配置用 `@ConfigMapping`（替代 Spring `@ConfigurationProperties`），
   装配用 `@Produces @Singleton` + `@IfBuildProperty`（替代 Spring `@AutoConfiguration`），
   异常处理用 JAX-RS `ExceptionMapper`（替代 `@ControllerAdvice`）。
3. **日志**：使用 `org.jboss.logging.Logger`，占位符风格为 `infof("...%s", arg)`。
4. **测试**：每个 starter 至少 1 个 `@QuarkusTest`；涉及外部组件的用例走 Testcontainers
   fixture（复用 `ddd4j-quarkus-mq-testcontainers` / samples 中的模式）。
5. **版本**：新依赖必须进入 `ddd4j-quarkus-dependencies` 的 dependencyManagement，
   禁止在子模块写裸版本号；Quarkus 组件跟随 quarkus-bom，禁止覆盖版本。

## 提交规范

- 分支：基于 `feature/3.3.x` 创建 `feature/<topic>` 分支，PR 合入 `feature/3.3.x`。
- 提交信息：参考 [Conventional Commits](https://www.conventionalcommits.org/)，
  如 `feat(mq): align QuarkusMQListenerRegistrar with MQClient.init contract`。
- 单 PR 控制改动量（≤ 500 行），便于审查。

## CI

`.github/workflows/ci.yml` 三阶段：

1. `workflow-lint` — actionlint 校验 workflow 语法；
2. `build` — JDK 17 / 21 矩阵执行 `./mvnw -B verify`；
3. `infrastructure-integration` — 启用 Docker 复用后执行
   `./mvnw -B verify -Pintegration`（13 broker testcontainers 集成测试）。
