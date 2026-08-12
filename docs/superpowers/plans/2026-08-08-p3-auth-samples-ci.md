# P3 — Auth / Samples / CI/CD 实施计划

> **For agentic workers:** REQUIRED SUB-KILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 5 个 auth 子模块 Producer 化、7 个 sample 分层完整化、CI 三阶段工作流、CONTRIBUTING.md
**Architecture:** auth-* 模块 Producer/ConfigMapping；samples 按 ddd4j-boot 风格 domain/app/infrastructure/adapter 四层；CI lint + JDK 矩阵 + testcontainers integration
**Tech Stack:** Quarkus 3.37.0 + GitHub Actions + JDK 17/21 + Docker
**Related Design Doc:** [../specs/2026-08-08-p3-auth-samples-ci-design.md](../specs/2026-08-08-p3-auth-samples-ci-design.md)

## 全局约定

- auth Producer：`Ddd4jXxxQuarkusConfig` + `XxxCdiProducer`
- samples 分层：domain（无 Quarkus 依赖）/ application（无 Quarkus 依赖）/ infrastructure（依赖 panache）/ adapter（依赖 web）
- CI 三阶段：workflow-lint + build（matrix JDK 17/21）+ infrastructure-integration

## 实施阶段总览

```
Stage 1 — auth-jwt 完善（已有 12 测试）
Stage 2 — auth-{satoken,shiro,security,license} Producer 化
Stage 3 — samples 完整化（7 个分层模块）
Stage 4 — CI 工作流（.github/workflows/ci.yml）
Stage 5 — CONTRIBUTING.md
Stage 6 — 全量验证 + 提交
```

## Stage 1 — auth-jwt 完善

- [x] **Step 1.1: JwtConfig + Ddd4jJwtQuarkusConfig**
  - 文件: `ddd4j-quarkus-auth/ddd4j-quarkus-auth-jwt/src/main/java/io/ddd4j/quarkus/auth/jwt/{JwtConfig,Ddd4jJwtQuarkusConfig}.java`
  - 操作: ConfigMapping 接口（mp.jwt 公共前缀）
  - 验证: 编译通过

- [x] **Step 1.2: JwtSubjectProvider + JwtSubject**
  - 文件: 同上目录
  - 操作: `Supplier<Subject>` 实现 + smallrye-jwt 4.6.x 适配
  - 验证: 启动正常

- [x] **Step 1.3: JwtSubjectProviderQuarkusTest + JwtSubjectTest**
  - 文件: `src/test/java/io/ddd4j/quarkus/auth/jwt/*.java`
  - 操作: 2 个测试类（QuarkusTest 启动 + 纯 JUnit）
  - 验证: 12 个测试方法通过

- [x] **Step 1.4: smallrye-jwt 键名修正**
  - 文件: CONTRIBUTING.md / application.properties 模板
  - 操作: `key-location` → `smallrye.jwt.sign.key.location`（点分隔，4.6.x 规范）
  - 验证: 文档化

- [x] **Step 1.5: IllegalProductException 处理**
  - 文件: `JwtSubjectProvider.java`
  - 操作: provider 主动触碰 `getPrincipal()` 并捕获（client proxy 惰性创建）
  - 验证: 启动正常

## Stage 2 — auth-{satoken,shiro,security,license} Producer 化

- [x] **Step 2.1: auth-satoken SaTokenCdiProducer + Ddd4jSaTokenQuarkusConfig**
  - 文件: `ddd4j-quarkus-auth-jwt/src/main/java/io/ddd4j/quarkus/auth/satoken/*.java`
  - 操作: Producer 暴露 SaToken 集成
  - 验证: 编译通过

- [x] **Step 2.2: auth-shiro ShiroCdiProducer + Ddd4jShiroQuarkusConfig**
  - 文件: `ddd4j-quarkus-auth-shiro/src/main/java/io/ddd4j/quarkus/auth/shiro/*.java`
  - 操作: Shiro CDI 适配 + 异常映射
  - 验证: 编译通过

- [x] **Step 2.3: auth-security SpringSecurityCdiProducer + Ddd4jSecurityQuarkusConfig**
  - 文件: `ddd4j-quarkus-auth-security/src/main/java/io/ddd4j/quarkus/auth/security/*.java`
  - 操作: Security CDI 适配 + 异常映射
  - 验证: 编译通过

- [x] **Step 2.4: auth-license Ddd4jLicenseQuarkusConfig + truelicense-core 显式声明**
  - 文件: `ddd4j-quarkus-auth/ddd4j-quarkus-auth-license/pom.xml` + src/main
  - 操作: 显式声明 truelicense-core（主仓发布 POM 无效传递依赖恢复）
  - 验证: 编译通过

## Stage 3 — samples 完整化

- [x] **Step 3.1: sample-layered LayeredOrderApplication**
  - 文件: `ddd4j-quarkus-samples/ddd4j-quarkus-sample-layered/src/main/java/io/ddd4j/quarkus/sample/layered/*.java`
  - 操作: 端到端分层示例（domain + app + infra + adapter + client + common）
  - 验证: LayeredOrderApplicationTest 通过

- [x] **Step 3.2: sample-domain 9 实体**
  - 文件: `ddd4j-quarkus-samples/ddd4j-quarkus-sample-domain/src/main/java/io/ddd4j/quarkus/sample/domain/*.java`
  - 操作: 9 个领域实体（纯 POJO，无 Quarkus 依赖）
  - 验证: 12 个测试通过

- [x] **Step 3.3: sample-app Order Application Service**
  - 文件: `sample-app/src/main/java/io/ddd4j/quarkus/sample/app/order/service/OrderApplicationService.java`
  - 操作: Application Service + command/dto/mapper/query
  - 验证: 8 个测试通过

- [x] **Step 3.4: sample-infrastructure Repository 实现**
  - 文件: `sample-infrastructure/src/main/java/io/ddd4j/quarkus/sample/infrastructure/order/persistence/entity/OrderEntity.java`
  - 操作: Repository + Entity + OrderEntityTest（5 测试）
  - 验证: 编译通过

- [x] **Step 3.5: sample-adapter Resource**
  - 文件: `sample-adapter/src/main/java/io/ddd4j/quarkus/sample/adapter/web/OrderResource.java`
  - 操作: JAX-RS Resource + DTO（依赖 web）
  - 验证: OrderResourceQuarkusTest 5 测试通过

- [x] **Step 3.6: sample-client OrderClientDTO + sample-common ApiResponse**
  - 文件: 同上目录
  - 操作: client + common 模块
  - 验证: 5 + 8 测试通过

- [x] **Step 3.7: sample-cqrs-person QuarkusPersonResourceTest**
  - 文件: `sample-cqrs-person/src/test/java/...`
  - 操作: 1 个测试类
  - 验证: 1 测试通过

## Stage 4 — CI 工作流

- [x] **Step 4.1: .github/workflows/ci.yml workflow-lint job**
  - 文件: `.github/workflows/ci.yml`
  - 操作: `workflow-lint` job（ubuntu-latest + actions/checkout + setup-java 17）
  - 验证: workflow 文件 YAML 合法

- [x] **Step 4.2: build job JDK 17/21 矩阵**
  - 文件: 同上
  - 操作: `build` job + matrix: `{ jdk: [17, 21] }` + `./mvnw -B verify`
  - 验证: CI 通过

- [x] **Step 4.3: infrastructure-integration job**
  - 文件: 同上
  - 操作: `infrastructure-integration` job + docker setup + `./mvnw -B verify -Pintegration -pl 14 broker modules -am`
  - 验证: CI 通过

- [x] **Step 4.4: secrets 注入 MAVEN_USERNAME/MAVEN_PASSWORD**
  - 文件: 同上
  - 操作: credentials injection step → `~/.m2/settings.xml`（阿里云私有仓库）
  - 验证: 401 解决

## Stage 5 — CONTRIBUTING.md

- [x] **Step 5.1: CONTRIBUTING.md 编写**
  - 文件: `CONTRIBUTING.md`
  - 内容: 仓库结构表 + 开发环境（JDK 17/21 + Maven 3.9 + Docker）+ 构建测试命令 + 提交规范 + CI 三阶段说明
  - 验证: 文档完整

## Stage 6 — 全量验证

- [x] **Step 6.1: mvn -Pintegration verify**
  - 验证: 318 个测试 0 失败
  - 提交: `feat(samples+ci): complete layered samples with @QuarkusTest coverage and CI/CD`（commit f47413a）

## Self-Review / 完成校验

- [x] 5 个 auth 子模块 Producer 化完成
- [x] jwt 子模块 12 个测试方法通过
- [x] 7 个 sample 分层模块完整化
- [x] `.github/workflows/ci.yml` 三阶段（lint + build matrix + integration）
- [x] CONTRIBUTING.md 编写完整
- [x] mvn -Pintegration verify 全量通过

## 计划偏差记录

| 项 | 旧 plan 描述 | 实际落地 | 偏差原因 |
|---|---|---|---|
| auth-testcontainers 新增 | 计划新增共享 fixture | 未落地 | 优先级让位给 mq-testcontainers |
| sample-rich-model 完整化 | 计划 4 层完整 | 仅骨架 | 业务项目未触发 |
| sample-mq-* 完整化 | 计划完整化 + 集成测试 | 仅骨架 | mq 子模块已有 14 broker 集成测试覆盖 |

## 后续待办（非本次范围）

- [ ] 新增 `ddd4j-quarkus-auth-testcontainers` 模块 + 4 个 auth 子模块集成测试
- [ ] 完整化 sample-rich-model / sample-auth-* / sample-mq-*
- [ ] 删除 ddd4j-quarkus-extension-pf4j（空壳）
