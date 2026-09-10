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
- 仓库自带 `./mvnw`（Maven 3.8.1 / Model 4.0.0）；不要改成 Maven 4 聚合格式
- Docker（运行 `@QuarkusTest` 中的 Testcontainers 集成测试）
- Maven settings 配置已发布的 `ddd4j 2.0.x.20260630-SNAPSHOT` 仓库；空缓存解析命令见 [README](README.md)

## 构建与测试

```bash
# 编译
./mvnw -B compile

# 编译测试（含 @QuarkusTest 用例的编译验证，不启动容器）
./mvnw -B test-compile -DskipTests

# 全量单元测试（@QuarkusTest；涉及 Testcontainers 的用例需要 Docker）
./mvnw -B clean verify -Denforcer.skip=true

# 仅跑 testcontainers 集成测试（CI 的 broker-integration job 使用）
./mvnw -B verify -Pintegration -Denforcer.skip=true -pl <mq 模块列表> -am
```

P5-A 门禁使用 `-Denforcer.skip=true`，因此不代表 Enforcer 通过。Java 17、21 各自执行一次 `clean verify`，下一次 clean 前应保存 Surefire/Failsafe XML、日志、实际 JDK 版本和退出码。按 XML 原值累计 skipped，逐项列出原始原因。Web/License 定向门禁为：

```bash
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-web -am test
./mvnw -B -Denforcer.skip=true -pl ddd4j-quarkus-auth/ddd4j-quarkus-auth-license -am test
actionlint .github/workflows/ci.yml
```

本次版本、能力和测试证据统一记录于 [CAPABILITY-ALIGNMENT](docs/CAPABILITY-ALIGNMENT.md)，不能引用其他维护线的统计代替本线验证。

### 容器生命周期

- 共享 fixture 幂等启动并关闭自己拥有的容器，失败启动也执行清理；失败清理保留所有权，重试成功后才创建替代容器。
- 测试资源配置关闭复用，fixture 不得强制 `withReuse(true)`。不要清理其他任务拥有的容器。
- MQTT 的 Paho 持久化目录由现有 `.gitignore` 规则忽略；本线未采用 4.0.x 的 `target/mqtt-persistence` 目录覆盖。

### P5-B0 本地门禁（2026-09-10）

`91ceb9b` 的 Java 17/21 最终本地门禁各通过 61/61 模块、61 suites /
224 tests / 0 failures / 0 errors / 3 skips；4.0.x `fce0e1e` 为 61 suites /
228 tests / 0 failures / 0 errors / 3 skips。源码 reactor 的版本检查使用
`org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree -Dincludes=org.testcontainers -Dverbose`；
旧版 2.8 standalone tree 会读到陈旧制品描述，不作为源码版本证据。运行记录与逐项 skip 见
[P5-B0 本地验证](docs/P5-B0-LOCAL-VERIFICATION.md)。本地还验证了 BOM 52 个叶子、
auth runner、NATS、严格 Javadoc（52 个叶子、53 个归档）、security 代码门禁和
Quarkus 配置 warning=0。最新 HEAD 的 CI、发布与空缓存消费仍须各自验收。

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
6. **自有第三方坐标**：统一使用 `io.github.easy4j`，已跟踪源码、POM、注释和文档中的退役 groupId 必须零引用。优先使用导入的 ddd4j BOM 管理版本；已有管理时删除重复本地条目，不新增无必要的运行时依赖。

2026-09-09 `feature/3.3.x P5-A 本地完成`：已验证代码提交 `597a5b7` 的 Java 17/21 `clean verify -Denforcer.skip=true` 均为 62/62 模块、53 suites / 186 tests / 0 failures / 0 errors / 11 skipped。ZXing 采用 BOM 管理的 easy4j 坐标；Jackson 继续使用自包含 Jackson 2 实现。原始日志和各次补验边界见能力证据。

## 提交规范

审查修正补验：Web 与 License 在 Java 17/21 下各执行 `-am clean test`，均为 4 suites / 9 tests / 0 failures/errors/skipped（Web 4、License 5）。新增 License 用例分别覆盖部分材料准备异常与真实签发 false 返回，Web 先验证资源方法内绑定，再断言同线程清理；已验证提交的全量验证已包含这些修正。

- 分支：维护目标为 `feature/3.3.x`；按新版 AGENTS.md，创建或切换分支须先获授权，禁止 Git worktree。本次在普通独立克隆 `ddd4j-quarkus-33x-sync` 执行。
- 提交信息：参考 [Conventional Commits](https://www.conventionalcommits.org/)，
  如 `feat(mq): align QuarkusMQListenerRegistrar with MQClient.init contract`。
- 单 PR 控制改动量（≤ 500 行），便于审查。

## CI

`.github/workflows/ci.yml` 三阶段：

1. `workflow-lint` — `reviewdog/action-actionlint@v1` 设置 `fail_level: error`、`filter_mode: nofilter`；
2. `unit-and-contract` — 依赖 lint，JDK 17 / 21 矩阵执行 `./mvnw -B verify -Denforcer.skip=true`；
3. `broker-integration` — 依赖 lint 和 unit/contract，以 Java 17、13 个独立 broker job 执行 `verify -Pintegration -Denforcer.skip=true`，并上传各自报告。

两个 Maven job 都先 setup-java，再调用 `configure-maven`：组织 secret 解码到同目录临时文件，校验 server id 后以 `0600` 原子替换 settings；随后使用独立空 Maven 仓库验证发布制品。失败保留旧 settings 并清除临时凭据文件。

这是工作流静态设计。GitHub-hosted Actions 需推送并实际运行后另行验收；本地 P5-A 不包含 Native、Dev Mode、Enforcer、deploy、云服务、P5-B–E 或 master 验收。push 和 deploy 均须分别授权。
