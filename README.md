# ddd4j-quarkus

Quarkus 轨道的 ddd4j 平台，与 `ddd4j-boot` 对称，**不继承 Spring Boot**。

本仓定位为 Quarkus 的**深度适配聚合层**：

- `io.ddd4j:ddd4j-quarkus` 负责主仓中的通用 Quarkus 适配底座
- `ddd4j-quarkus-ddd/...` 保留 Quarkus 轨道的聚合入口、依赖编排与深度整合语义，核心运行时已收敛到 `io.ddd4j:ddd4j-quarkus`
- 不再按“逐步删空模块”的方式演进，而是对齐 `ddd4j-boot` 的整体结构

## 模块

| 模块                                         | 说明                                             |
|--------------------------------------------|------------------------------------------------|
| `ddd4j-quarkus-dependencies`               | `quarkus-bom` + `ddd4j-platform-dependencies`  |
| `ddd4j-quarkus-bom`                        | Quarkus 模块版本                                   |
| `ddd4j-quarkus-parent`                     | 业务 parent（插件、Jakarta 基础依赖）                     |
| `io.ddd4j:ddd4j-quarkus`                   | 主仓通用 Quarkus 适配底座（注解、CDI、CQRS、EventStore 通用能力） |
| `ddd4j-quarkus-ddd`                        | Quarkus DDD 深度适配聚合                             |
| `ddd4j-quarkus-data/cache/auth/mq/monitor` | Quarkus 轨业务域深度适配聚合                             |

## 构建顺序

```bash
cd ../ddd4j
mvn install -DskipTests

cd ../ddd4j-quarkus
mvn -pl ddd4j-quarkus-samples/sample-api -am clean package -DskipTests
```

说明：

- 先安装主仓 `ddd4j`，使 `io.ddd4j:ddd4j-quarkus` 等底座模块进入本地仓库
- 再构建本仓 `ddd4j-quarkus`，由各深度适配模块复用主仓通用实现

## 业务项目 parent

```xml
<parent>
  <groupId>io.ddd4j.quarkus</groupId>
  <artifactId>ddd4j-quarkus-parent</artifactId>
  <version>1.0.x.20260625-SNAPSHOT</version>
</parent>
```

宝港湾 Quarkus 服务：`bmgw-quarkus-parent` → `ddd4j-quarkus-parent`。
