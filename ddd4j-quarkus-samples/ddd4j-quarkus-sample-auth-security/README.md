# ddd4j-quarkus-sample-auth-security

> ⚠️ **DEPRECATED since 4.1.0** — This sample demonstrates the deprecated
> `ddd4j-quarkus-auth-security` module. New projects should use
> [`ddd4j-quarkus-sample-auth-satoken`](../ddd4j-quarkus-sample-auth-satoken/) instead.
>
> See [MIGRATION-auth-security-to-satoken.md](../../docs/MIGRATION-auth-security-to-satoken.md) for migration guide.
>
> **Removal scheduled for 5.0.0.**

## Why is this deprecated?

Spring Security requires Spring ecosystem; Quarkus recommends sa-token.

See module JavaDoc in `Ddd4jSecurityQuarkusConfig.java` for details.

## What does this sample do?

演示 `ddd4j-quarkus-auth-security` 模块的统一鉴权入口（`SubjectKit`，底层为
Quarkus Security / Spring Security 风格集成），通过 `AuthResource`（`/auth`）暴露：

- `POST /auth/login` — 登录并返回 token 与 principal
- `POST /auth/logout` — 登出
- `GET /auth/me` — 查看当前登录用户信息
- `GET /auth/check/permission?permission=xxx` — 权限校验
- `GET /auth/check/role?role=xxx` — 角色校验
- `GET /auth/status` — 登录状态查询

## Migration

Run the satoken sample instead:

```bash
./mvnw quarkus:dev -pl ddd4j-quarkus-samples/ddd4j-quarkus-sample-auth-satoken -am
```
