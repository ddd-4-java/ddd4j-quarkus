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

## 构建顺序

```bash
cd ../ddd4j
mvn install -DskipTests

cd ../ddd4j-quarkus
mvn -pl ddd4j-quarkus-samples/ddd4j-quarkus-sample-api -am clean package -DskipTests
```

说明：

- 先安装主仓 `ddd4j`，使 `io.ddd4j:ddd4j-runtime-quarkus` 等底座模块进入本地仓库
- 再构建本仓 `ddd4j-quarkus`，由各深度适配模块复用主仓通用实现

## 构建约定：为何用 `<modules>` 而非 Maven 4 的 `<subprojects>`

feature/4.0.x 使用 Maven 4（modelVersion 4.1.0），但聚合语法刻意保留 `<modules>`：
Quarkus 3.38.x 的 WorkspaceLoader 内嵌 Maven 3.9.9 的 `MavenXpp3Reader`，解析
`<subprojects>` 时报 `Unrecognised tag`，导致**所有 `@QuarkusTest` 无法引导**。
`<modules>` 是 Maven 3 / Maven 4 / Quarkus 测试引导的兼容交集（Maven 4 仍完整支持）。
计划升级 Quarkus 4 后回归 `<subprojects>`（跟踪 quarkusio/quarkus#52190）。
实证记录见 commit `274ee01`。

feature/3.3.x 对应 ddd4j 2.0.x 线，本就是 Maven 3 + modelVersion 4.0.0 + `<modules>`，不受影响。

## CI 前置条件：`MAVEN_SETTINGS_XML` 组织 secret

GitHub Actions 依赖 **ddd-4-java 组织级 secret** 解析 Aliyun 私有仓 SNAPSHOT：

1. org → Settings → Secrets and variables → Actions → New organization secret
2. Name：`MAVEN_SETTINGS_XML`；Value：**base64 编码**的 settings.xml（须含 `2624322-snapshot-3EoOv3` 服务器凭据）
3. 本地生成：`base64 -i ~/.m2/settings.xml | pbcopy`（macOS）

secret 缺失时 CI **直接失败**（fail-fast）并输出修复指引，不再降级为警告继续跑。

## 业务项目 parent

```xml
<parent>
  <groupId>io.ddd4j.quarkus</groupId>
  <artifactId>ddd4j-quarkus-parent</artifactId>
  <version>4.0.x.20260630-SNAPSHOT</version>
</parent>
```

宝港湾 Quarkus 服务：`bmgw-quarkus-parent` → `ddd4j-quarkus-parent`。
