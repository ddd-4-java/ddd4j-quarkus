# ddd4j-quarkus 示例工程

本目录放 Quarkus 运行时关注的示例：CDI、JAX-RS、Panache/Quarkus 数据适配和 Quarkus 原生构建约束。通用领域模型优先复用 `io.ddd4j:ddd4j-sample-*`。

## 重点示例

| 示例 | 方向 | 说明 |
|---|---|---|
| `ddd4j-quarkus-sample-layered` | 分层骨架 | 对齐 Boot 分层样例的 Quarkus 骨架，后续可替换为 Panache 持久化 |
| `ddd4j-quarkus-sample-auth-*` | Auth | Sa-Token 与 Shiro 两种鉴权实现的 HTTP 会话示例 |

> TODO: 普通充血模型（rich-model）与 CQRS/ES（cqrs-person）示例已下线，待重新设计后回归。

分层示例编译验证：

```bash
./mvnw -pl ddd4j-quarkus-samples/ddd4j-quarkus-sample-layered -am compile -DskipTests
```

## 认证示例

认证示例使用真实 Quarkus HTTP 服务和后端会话，通过 `SubjectKit` 登录、查询身份、授权与注销：

| 示例 | 令牌传递 | 授权数据 |
|---|---|---|
| `ddd4j-quarkus-sample-auth-satoken` | 请求头 `satoken: <token>` | `SubjectDataProvider` 提供 `user` 角色、`profile:read` 权限 |
| `ddd4j-quarkus-sample-auth-shiro` | 请求头 `X-Session-Id: <token>`，token 为 Shiro 原生 Session ID | Shiro Realm 提供 `user` 角色、`profile:read` 权限 |

两例的 `POST /auth/login` 均接收 `text/plain` 的 userId，如 `alice`。
Sa-Token 演示已完成上游身份校验后的会话创建；Shiro 使用固定演示账号
`alice`/`bob` 和空凭证。它们是本地会话集成教学示例；真实业务必须接入
账号凭证校验与权限数据源，不能把仅提交 userId 当作生产认证。

登录响应包含非空 `token`。后续请求携带对应请求头：

- `GET /auth/status`：`login`。
- `GET /auth/me`：`authenticated`、`loginId`、`userId`；匿名时只有 `authenticated=false`。
- `GET /auth/check/role?role=user`：`has=true`；`admin` 返回 false。
- `GET /auth/check/permission?permission=profile:read`：`has=true`；`admin:write` 返回 false。
- `POST /auth/logout`：撤销当前会话；原 token 后续不能恢复身份。

Sa-Token 上下文存储绑定真实 Vert.x 请求；Shiro 在每个资源方法调用内恢复会话，
通过 Shiro `Subject.execute` 在正常/异常出口恢复线程状态。

在仓库根目录使用 Java 17 或 Java 21 和 Maven 3.9.16（3.3.x 维护线）：

```bash
mvn -B -ntp -pl ddd4j-quarkus-samples/ddd4j-quarkus-sample-auth-satoken test
mvn -B -ntp -pl ddd4j-quarkus-samples/ddd4j-quarkus-sample-auth-shiro test
mvn -B -ntp -DskipTests=false -pl ddd4j-quarkus-samples -am verify
```

上面的 HTTP 测试使用动态端口，并覆盖匿名、有效 token、伪造 token、
授权正反例和注销后的状态。最后一条命令显式覆盖上游依赖 BOM 的跳测默认值，
包含其他 samples；MQ 的业务端到端完成度另由 P3 Task 3 跟踪。

## 已移除的 Security 示例

`ddd4j-quarkus-sample-auth-security` 随 P3 Task 2 从源码和 reactor 移除；
仓库 BOM 原本未管理该 sample，无需删除不存在的坐标。其底层
`ddd4j-quarkus-auth-security` 仍保留废弃标记，以维护既有依赖兼容性。
新示例使用上面的 Sa-Token 或 Shiro；迁移说明见
[auth-security 迁移指南](../docs/MIGRATION-auth-security-to-satoken.md)。
