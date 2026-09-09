# ddd4j-quarkus

Quarkus 轨道的 ddd4j 平台，与 `ddd4j-boot` 对称，**不继承 Spring Boot**。

本仓定位为 Quarkus 的**深度适配聚合层**：

- `io.ddd4j:ddd4j-runtime-quarkus` 负责主仓中的通用 Quarkus 运行时绑定底座
- `ddd4j-quarkus-ddd/...` 保留 Quarkus 轨道的聚合入口、依赖编排与深度整合语义，核心运行时已收敛到 `io.ddd4j:ddd4j-runtime-quarkus`
- 不再按“逐步删空模块”的方式演进，而是对齐 `ddd4j-boot` 的整体结构

## 模块

| 模块                                         | 说明                                             |
|--------------------------------------------|------------------------------------------------|
| `ddd4j-quarkus-dependencies`               | `quarkus-bom` + `ddd4j-dependencies`  |
| `ddd4j-quarkus-bom`                        | Quarkus 模块版本                                   |
| `ddd4j-quarkus-parent`                     | 业务 parent（插件、Jakarta 基础依赖）                     |
| `io.ddd4j:ddd4j-runtime-quarkus`           | 主仓通用 Quarkus 运行时绑定底座（注解、CDI、CQRS、EventStore 通用能力） |
| `ddd4j-quarkus-ddd`                        | Quarkus DDD 深度适配聚合                             |
| `ddd4j-quarkus-data/cache/auth/mq`         | Quarkus 轨业务域深度适配聚合，对齐 boot 的领域模块边界                |
| `ddd4j-quarkus-extensions`                 | Quarkus 轨跨领域扩展聚合，对齐 boot extensions 中已有底座的扩展模块       |

## feature/3.3.x 构建基线

本线使用 `ddd4j 2.0.x.20260630-SNAPSHOT`、本仓 revision `3.3.x.20260630-SNAPSHOT`、Quarkus runtime/BOM/plugin `3.37.4`。Java 17 为基线、21 为兼容验证；Maven wrapper 为 `3.8.1`，Model `4.0.0`，聚合保持 `<modules>`。

```bash
P5A_CONSUMER_REPO="$(mktemp -d /tmp/ddd4j-quarkus-consumer.XXXXXX)"
./mvnw -B -U -f .github/maven/ddd4j-remote-consumer/pom.xml \
  -Dmaven.repo.local="$P5A_CONSUMER_REPO" \
  -Dmaven.resolver.transport=wagon dependency:go-offline
./mvnw -B clean verify -Denforcer.skip=true
```

Maven settings 必须能访问已发布 ddd4j 快照仓库；消费者直接解析已发布制品。`-Denforcer.skip=true` 表示本阶段未验收 Enforcer。详细运行结果与限制见 [P5-A 能力证据](docs/CAPABILITY-ALIGNMENT.md)。

2026-09-09 已将 QR 依赖切换到 BOM 管理的 `io.github.easy4j:zxing-extension`，修复 Java 17 字节码不兼容。修复后 Java 17/21 全量均为 62/62 模块成功，各 53 suites、185 tests、0 failures/errors、11 skipped；最后坐标清理的补验和历史失败见能力证据。最终复审与 final-HEAD 门禁仍待执行。

自有第三方组件统一使用 `io.github.easy4j`；源码、POM、注释和文档均不得保留退役发布组引用。版本优先沿用 ddd4j BOM 管理，不在叶模块重复固定。

最终审查修正已补强 Web 清理前绑定及 License 部分材料/false 签发清理证据，并更正 CI/BOM 注释。受影响模块的 Java 17/21 clean test 均通过：Web 4 tests、License 5 tests，共 4 suites / 9 tests，failures/errors/skipped 均为 0。上文 185 tests 是修正前全量历史结果；最终复审及 final-HEAD 全量门禁待完成。

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
  <version>3.3.x.20260630-SNAPSHOT</version>
</parent>
```

宝港湾 Quarkus 服务：`bmgw-quarkus-parent` → `ddd4j-quarkus-parent`。
