# ddd4j-quarkus

Quarkus 轨道的 ddd4j 平台，与 `ddd4j-boot` 对称，**不继承 Spring Boot**。

## 模块

| 模块 | 说明 |
|------|------|
| `ddd4j-quarkus-dependencies` | `quarkus-bom` + `ddd4j-platform-dependencies` |
| `ddd4j-quarkus-bom` | Quarkus 模块版本 |
| `ddd4j-quarkus-parent` | 业务 parent（插件、Jakarta 基础依赖） |
| `ddd4j-quarkus-annotation` | CDI 构造型 |
| `ddd4j-quarkus-core` | 请求上下文等 |
| `ddd4j-quarkus-ddd` | fuinorg DDD/CQRS |
| `ddd4j-quarkus-data/web/mq/monitor` | Phase 2+ 桩模块 |

## 构建顺序

```bash
cd ../ddd4j-platform && mvn install -DskipTests
cd ../ddd4j-quarkus
mvn -pl ddd4j-quarkus-samples/sample-api -am clean package -DskipTests
```

## 业务项目 parent

```xml
<parent>
  <groupId>io.ddd4j.quarkus</groupId>
  <artifactId>ddd4j-quarkus-parent</artifactId>
  <version>1.0.x.20260625-SNAPSHOT</version>
</parent>
```

宝港湾 Quarkus 服务：`bmgw-quarkus-parent` → `ddd4j-quarkus-parent`。
