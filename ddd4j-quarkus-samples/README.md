# ddd4j-quarkus 示例工程

本目录放 Quarkus 运行时关注的示例：CDI、JAX-RS、Panache/Quarkus 数据适配和 Quarkus 原生构建约束。通用领域模型优先复用 `io.ddd4j:ddd4j-sample-*`。

## 重点示例

| 示例                                  | 方向        | 说明 |
|-------------------------------------|-----------|------|
| `ddd4j-quarkus-sample-layered`      | 分层骨架      | 对齐 Boot 分层样例的 Quarkus 骨架，后续可替换为 Panache 持久化 |
| `ddd4j-quarkus-sample-auth-*`       | Auth      | 三种鉴权实现接入示例 |

> TODO: 普通充血模型（rich-model）与 CQRS/ES（cqrs-person）示例已下线，待重新设计后回归。

验证命令：

```bash
mvn -pl ddd4j-quarkus-samples/ddd4j-quarkus-sample-layered -am compile -DskipTests
```
