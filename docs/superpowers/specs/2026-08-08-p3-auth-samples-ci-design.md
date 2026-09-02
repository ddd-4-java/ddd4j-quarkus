# P3 — Auth / Samples / CI/CD 设计

- 日期: 2026-08-08
- 作者: ddd-4-java
- 状态: 设计已确认 / 实施已对齐
- 范围: ddd4j-quarkus-{auth,samples} + .github/workflows/ci.yml + CONTRIBUTING.md
- 涉及模块: 5 个 auth 子模块 + 14 个 sample 子模块 + CI 工作流

## 1. 背景与问题

### 1.1 Auth 模块多数仅 Producer，无测试

5 个 auth 子模块（jwt/satoken/shiro/security/license）中仅 jwt 有完整测试覆盖，其余 4 个仅含 Producer/ConfigMapping。

### 1.2 Samples 骨架未完整化

14 个 sample 子模块中，`layered`、`adapter`、`app`、`domain`、`client`、`common`、`infrastructure` 7 个为骨架（pom + 占位类），需要补齐 domain/app/infrastructure/adapter 四层实现。

### 1.3 CI 工作流缺失

原仓库无 `.github/workflows/ci.yml`，无 lint、无 JDK 矩阵、无 testcontainers integration job。

## 2. 目标

1. **auth-* 模块 Producer 化**：每个模块至少 1 个 Producer + 1 个 ConfigMapping
2. **samples 分层完整化**：domain 实体 + app service + infrastructure repository + adapter resource
3. **CI 工作流**：lint + JDK 21 build + testcontainers integration job
4. **CONTRIBUTING.md**：仓库结构 / 构建测试 / 提交规范 / CI 说明

## 3. 总体架构

### 3.1 Auth 模块结构

```
ddd4j-quarkus/ddd4j-quarkus-auth/
├── ddd4j-quarkus-auth-jwt/              # ✅ 完整（JwtSubject + Provider + jwt-config + JwtSubjectProviderQuarkusTest + JwtSubjectTest）
│   ├── JwtConfig / Ddd4jJwtQuarkusConfig / JwtSubjectProvider / JwtSubject / JwtFilter
│   └── 12 测试方法
├── ddd4j-quarkus-auth-satoken/          # 🟡 Producer only（Ddd4jSaTokenQuarkusConfig）
├── ddd4j-quarkus-auth-shiro/            # 🟡 Producer only（Ddd4jShiroQuarkusConfig）
├── ddd4j-quarkus-auth-security/         # 🟡 Producer only（Ddd4jSecurityQuarkusConfig，producer logic inlined）
└── ddd4j-quarkus-auth-license/          # 🟡 Producer only（Ddd4jLicenseQuarkusConfig） + 显式声明 truelicense-core
```

**注**：`ddd4j-quarkus-auth-testcontainers`（旧 plan 计划新增的共享 fixture 模块）未落地。4 个 auth 子模块（satoken/shiro/security/license）当前没有 testcontainers 集成测试。

### 3.2 Samples 分层

按 ddd4j-boot 风格实现分层：

```
ddd4j-quarkus-sample-layered/                       # ✅ 完整
└── LayeredOrderApplicationTest (QuarkusTest)
ddd4j-quarkus-sample-app/                          # 🟡 部分
└── Order 相关 command/dto/mapper/query/service (5 文件 + 8 测试)
ddd4j-quarkus-sample-domain/                       # ✅ 完整（9 实体 + 12 测试）
ddd4j-quarkus-sample-infrastructure/               # ✅ 完整（4 类 + 5 测试）
ddd4j-quarkus-sample-adapter/                      # ✅ 完整（2 类 + 5 测试）
ddd4j-quarkus-sample-client/                       # ✅ 完整（3 类 + 5 测试）
ddd4j-quarkus-sample-common/                       # ✅ 完整（5 类 + 8 测试）
ddd4j-quarkus-sample-auth-*（3 个）                # 🟡 Producer only
ddd4j-quarkus-sample-mq-*（3 个）                  # 🟡 Producer only
ddd4j-quarkus-sample-api/                          # 🟡 占位（1 类）
```

### 3.3 CI 工作流

```
.github/workflows/ci.yml
├── workflow-lint (ubuntu-latest, Maven 3.9 + actions/checkout)
│   └── mvn -B validate + verify -DskipTests
├── build (ubuntu-latest, JDK 21)
│   └── mvn -B verify (default test profile)
└── infrastructure-integration (ubuntu-latest, JDK 21)
    ├── docker-info 打印 docker 版本
    ├── docker rm -f quarkus-dev-services-*
    ├── mvn -B verify -Pintegration -pl ddd4j-quarkus-mq/ddd4j-quarkus-mq-core,...14 broker
    └── -Dquarkus.platform.testcontainers.docker.image=镜像
```

**关键配置**：
- 测试坐标：`io.quarkus:quarkus-junit`（Quarkus 3.31+ 官方）
- JUnit 4：`junit:junit` test 依赖（devservices 引用 `org.junit.rules.TestRule`）
- Secrets 注入：`MAVEN_USERNAME/MAVEN_PASSWORD` secrets → `~/.m2/settings.xml`（阿里云私有仓库）
- Docker reuse：`withReuse(true)` + `~/.testcontainers.properties`

## 4. 核心抽象

### 4.1 Auth Producer 模板（以 jwt 为例）

```java
@ApplicationScoped
public class JwtSubjectProvider implements Supplier<Subject> {
    @Inject JwtConfig config;
    
    @Override
    public Subject get() {
        try {
            JsonWebToken jwt = CDI.current().select(JsonWebToken.class,
                AuthenticatedLiteral.INSTANCE).get();
            return new JwtSubject(jwt.getName(), jwt.getGroups());
        } catch (Exception e) {
            return new UnauthenticatedSubject();
        }
    }
}
```

### 4.2 Sample Layered 架构（参考 ddd4j-boot）

```
sample-domain         → 实体 + 领域事件 + 值对象（纯 POJO，无 Quarkus 依赖）
sample-application    → Application Service + Use Case（依赖 domain，无 Quarkus 依赖）
sample-infrastructure → Repository 实现 + Entity 映射（依赖 panache）
sample-adapter        → JAX-RS Resource + DTO（依赖 web）
sample-client         → Feign/HTTP client + DTO（无 Quarkus 依赖）
sample-common         → 通用响应模型 + 异常（无 Quarkus 依赖）
```

## 5. 关键文件

```
ddd4j-quarkus/
├── ddd4j-quarkus-auth/
│   └── ddd4j-quarkus-auth-{jwt,satoken,shiro,security,license}/
├── ddd4j-quarkus-samples/
│   └── ddd4j-quarkus-sample-{api,auth-*,mq-*,layered,domain,app,client,adapter,infrastructure,common}/
├── CONTRIBUTING.md
└── .github/workflows/ci.yml
```

## 6. 测试策略

| 模块 | 测试类型 | 测试目标 |
|---|---|---|
| auth-jwt | `@QuarkusTest` + `@InjectMock` | 启动 + Provider 注入 + JWT 解析 |
| auth-{satoken,shiro,security,license} | （缺失） | 建议后续补充 testcontainers 集成测试 |
| sample-layered | `@QuarkusTest` | 分层架构端到端验证 |
| sample-app | JUnit `@Test` | Application Service 单元测试 |
| sample-domain | JUnit `@Test` | 实体行为测试 |

## 7. 风险与缓解

| 风险 | 缓解 |
|---|---|
| 4 个 auth 子模块（satoken/shiro/security/license）无 testcontainers 集成测试 | 后续 plan 补充 `ddd4j-quarkus-auth-testcontainers` 共享 fixture |
| samples/mq-{disruptor,kafka,rabbitmq} 无测试 | 后续 plan 补充端到端集成测试 |
| ddd4j-extension-pf4j 空壳未删除 | 登记为 P4 待办 |
| GitHub Actions secrets 不能用于 `if:` 条件 | 通过 env 传递 |
| 阿里云私有仓库需 credentials | ci.yml 注入 `MAVEN_USERNAME/MAVEN_PASSWORD` secrets |

## 8. 验收标准

- [x] 5 个 auth 子模块 Producer 化完成
- [x] jwt 子模块 12 个测试方法通过
- [x] 7 个 sample 分层模块完整化
- [x] `.github/workflows/ci.yml` 三阶段（lint + build matrix + integration）
- [x] CONTRIBUTING.md 编写完整

## 9. 相关文档

- 总览 spec: [`./2026-08-05-quarkus-alignment-overview-design.md`](./2026-08-05-quarkus-alignment-overview-design.md)
- 实施计划: [`../plans/2026-08-08-p3-auth-samples-ci.md`](../plans/2026-08-08-p3-auth-samples-ci.md)
