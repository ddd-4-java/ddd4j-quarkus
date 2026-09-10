# ddd4j-quarkus

Quarkus 轨道的 ddd4j 平台，与 `ddd4j-boot` 对称，**不继承 Spring Boot**。

本仓定位为 Quarkus 的**深度适配聚合层**：

- `io.ddd4j:ddd4j-runtime-quarkus` 负责主仓中的通用 Quarkus 运行时绑定底座
- `ddd4j-quarkus-ddd/...` 保留 Quarkus 轨道的聚合入口、依赖编排与深度整合语义，核心运行时已收敛到 `io.ddd4j:ddd4j-runtime-quarkus`
- 不再按“逐步删空模块”的方式演进，而是对齐 `ddd4j-boot` 的整体结构

## 模块

| 模块                                         | 说明                                             |
|--------------------------------------------|------------------------------------------------|
| `ddd4j-quarkus-dependencies`               | `quarkus-bom` + `ddd4j-platform-dependencies`  |
| `ddd4j-quarkus-bom`                        | Quarkus 模块版本                                   |
| `ddd4j-quarkus-parent`                     | 业务 parent（插件、Jakarta 基础依赖）                     |
| `io.ddd4j:ddd4j-runtime-quarkus`           | 主仓通用 Quarkus 运行时绑定底座（注解、CDI、CQRS、EventStore 通用能力） |
| `ddd4j-quarkus-ddd`                        | Quarkus DDD 深度适配聚合                             |
| `ddd4j-quarkus-data/cache/auth/mq`         | Quarkus 轨业务域深度适配聚合，对齐 boot 的领域模块边界                |
| `ddd4j-quarkus-extensions`                 | Quarkus 轨跨领域扩展聚合，对齐 boot extensions 中已有底座的扩展模块       |

## P5-A 发布依赖与本地构建

`feature/4.0.x` 使用已发布的 ddd4j snapshot。先配置本机 Maven settings 中的
`2624322-snapshot-3EoOv3` 服务器凭据，再验证一个全新的 Maven 仓库：

```bash
P5A_REMOTE_REPO="$(mktemp -d /tmp/ddd4j-p5a-remote.XXXXXX)"
./mvnw -B -U -f .github/maven/ddd4j-remote-consumer/pom.xml \
  -Dmaven.repo.local="$P5A_REMOTE_REPO" \
  -Dmaven.resolver.transport=wagon dependency:go-offline
./mvnw -B clean verify -Denforcer.skip=true
```

第一条命令独立验证远端依赖闭包；第二条使用本机配置的 Maven 仓库运行完整 reactor。
无需先检出或安装主仓 ddd4j。`-Denforcer.skip=true` 表示本次结果不包含 Enforcer 验收。

| P5-A 项目 | 已核对值 |
|---|---|
| ddd4j-quarkus revision | `4.0.x.20260630-SNAPSHOT` |
| ddd4j | `3.0.x.20260630-SNAPSHOT` |
| Quarkus BOM/plugin / Testcontainers | `3.38.2` / `2.0.5` |
| Java / Maven | `21.0.12.1` / `4.0.0-rc-6` |
| Maven Model / 聚合 | `4.1.0` / `modules/module` |

2026-09-09 P5-A 本地门禁：新空仓远端 `dependency:go-offline` 成功；Web 4 tests、
License 3 tests 均零失败/错误/跳过；`clean verify` 的 62/62 模块成功。
最终 XML 为 47 个 Surefire suites、141 tests、0 failures、0 errors、3 skipped，
没有 Failsafe suite。跳过项分别为 MQTT-Mica 的 macOS arm64 限制、ONS 商业协议缺少镜像、
Security 模块废弃，具体类与原因见 [P5-A 完成证据](docs/superpowers/specs/2026-09-08-quarkus-production-extension-convergence-design.md#9-p5-a-本地完成证据2026-09-09)。

上述为历史 P5-A 本地证据；P5-B–E 待实施。本轮未执行 push/deploy，也没有本轮 GitHub Actions
运行证据；上游模型、settings 解析和测试配置警告仍存在。该结果不等于生产扩展、Native、
Dev Mode、3.3.x 或云服务已验收。

## P5-B0 Testcontainers 本地验证（2026-09-10）

`fce0e1e` 的 Java 21 / Maven 4 完整本地门禁通过：61/61 模块、61 suites /
228 tests / 0 failures / 0 errors / 3 skips；3.3.x `91ceb9b` 在 Java 17/21 下
各为 61 suites / 224 tests / 0 failures / 0 errors / 3 skips。
源码 reactor 与测试 classpath 的 Testcontainers 全为 `2.0.5`；Easy4J Jackson/ZXing
均从 ddd4j 3.0.x BOM 取得版本。双分支 13 broker 比较、skip 原因和原始证据见
[P5-B0 本地验证](docs/P5-B0-LOCAL-VERIFICATION.md)。本地还验证了 BOM 52 个叶子、
auth runner、NATS、严格 Javadoc、security 代码门禁与 Quarkus 配置 warning=0。
目标 HEAD 的 CI、发布和发布后空缓存消费仍待执行；不能将本地通过视为 P5-B0
整体完成或生产就绪。

## 构建约定：为何用 `<modules>` 而非 Maven 4 的 `<subprojects>`

feature/4.0.x 使用 Maven 4（modelVersion 4.1.0），但聚合语法刻意保留 `<modules>`：
Quarkus 3.38.x 的 WorkspaceLoader 内嵌 Maven 3.9.9 的 `MavenXpp3Reader`，解析
`<subprojects>` 时报 `Unrecognised tag`，导致**所有 `@QuarkusTest` 无法引导**。
`<modules>` 是 Maven 3 / Maven 4 / Quarkus 测试引导的兼容交集（Maven 4 仍完整支持）。
Maven 4 `<subprojects>` 仍为上游阻塞，必须等 WorkspaceLoader 支持并重新通过测试后迁移；
不能仅凭 Quarkus 版本号推定兼容。历史复现包括 2026-09-07 的 `999-SNAPSHOT`，
实证记录见 commit `274ee01`。

feature/3.3.x 对应 ddd4j 2.0.x 线，本就是 Maven 3 + modelVersion 4.0.0 + `<modules>`，不受影响。

## CI 前置条件：`MAVEN_SETTINGS_XML` 组织 secret

GitHub Actions 依赖 **ddd-4-java 组织级 secret** 解析 Aliyun 私有仓 SNAPSHOT：

1. org → Settings → Secrets and variables → Actions → New organization secret
2. Name：`MAVEN_SETTINGS_XML`；Value：**base64 编码**的 settings.xml（须含 `2624322-snapshot-3EoOv3` 服务器凭据）
3. 本地生成：`base64 -i ~/.m2/settings.xml | pbcopy`（macOS）

secret 缺失时 CI **直接失败**（fail-fast）并输出修复指引，不再降级为警告继续跑。

当前 `configure-maven` action 先在临时文件验证 settings，再以 `0600` 权限原子替换；
两个 Maven job 都从新空仓解析已发布 ddd4j。CI 已移除 ddd4j 源码安装和源码改写步骤，
workflow-lint 实际调用 actionlint。这里描述的是已提交的 CI 设计，远端运行结果需另行验证。

## 业务项目 parent

```xml
<parent>
  <groupId>io.ddd4j.quarkus</groupId>
  <artifactId>ddd4j-quarkus-parent</artifactId>
  <version>4.0.x.20260630-SNAPSHOT</version>
</parent>
```

宝港湾 Quarkus 服务：`bmgw-quarkus-parent` → `ddd4j-quarkus-parent`。
