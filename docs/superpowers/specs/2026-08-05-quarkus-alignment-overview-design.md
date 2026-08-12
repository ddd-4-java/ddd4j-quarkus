# ddd4j-quarkus 对齐 ddd4j-boot 框架集成 — 总体设计

- 日期: 2026-08-05
- 作者: ddd-4-java（基于多智能体协作）
- 状态: 设计已确认 / 实施已对齐
- 范围: ddd4j-quarkus 整体
- 涉及模块: ddd4j-quarkus-{bom,dependencies,parent,ddd,cache,web,data,mq,auth,extensions,samples}
- 来源: 本文档核心内容吸收自 `.zcode/plans/plan-sess_5963fbf4-6b1b-4107-b46d-531bc7c75443.md`（旧 ZCode session plan，已被本规范体系取代）

## 1. 背景与定位

Quarkus 轨道的 ddd4j 平台，与 `ddd4j-boot`（Spring Boot 轨道）对称，**不继承 Spring Boot**。

本仓定位为 Quarkus 的**深度适配聚合层**：

- `io.ddd4j:ddd4j-runtime-quarkus` 负责主仓中的通用 Quarkus 运行时绑定底座
- `ddd4j-quarkus-ddd/...` 保留 Quarkus 轨道的聚合入口、依赖编排与深度整合语义，核心运行时已收敛到 `io.ddd4j:ddd4j-runtime-quarkus`
- 不再按"逐步删空模块"的方式演进，而是对齐 `ddd4j-boot` 的整体结构

## 2. 目标与非目标

### 2.1 目标

- 能力基线对齐 ddd4j-boot（|ddd4j-quarkus 模块| ≥ |ddd4j-boot 模块| × 80%）
- 保留 ddd4j-quarkus 现有 87 个 Java 基线文件
- 补齐所有空 starter（新增 web / data-jpa / data-external / auth-testcontainers 等）
- 每个模块新增 testcontainers 集成测试
- 提供统一的 CI 工作流（lint + JDK 矩阵 build + testcontainers integration job）

### 2.2 非目标（v3.3.x 不做）

- 不替代 ddd4j-boot（Spring 轨道仍由 ddd4j-boot 维护）
- 不引入 Spring 生态依赖
- 不拆分 ddd4j-cloud（独立 plan）
- 不引入 Quarkus Plugin 体系替代 pf4j（仅对齐 ddd4j-boot 的扩展思路）
- 不对齐 ddd4j-javalin（javalin 轨道独立维护）

## 3. 总体架构

### 3.1 Maven 模块结构

```
ddd4j-quarkus/
├── ddd4j-quarkus-bom                          # 1 POM（仅依赖清单，扁平 BOM）
├── ddd4j-quarkus-dependencies                 # 1 POM（import quarkus-bom 3.37.x + Quarkiverse BOMs）
├── ddd4j-quarkus-parent                       # 业务项目 parent（按域 profiles，无默认全家桶）
├── ddd4j-quarkus-ddd                          # DDD/CQRS/EventStore 整合
├── ddd4j-quarkus-cache                        # Caffeine/Guava/Hutool 缓存
├── ddd4j-quarkus-data/{crypto,datascope,logs,panache,jpa,external}  # 数据访问
├── ddd4j-quarkus-mq/{core + testcontainers + 14 broker}            # MQ 适配
├── ddd4j-quarkus-auth/{jwt,satoken,shiro,security,license}        # 认证授权
├── ddd4j-quarkus-extensions/{9 modules}       # 跨领域扩展
└── ddd4j-quarkus-samples/{16 modules}         # 示例与集成测试
```

合计：10 个根模块 + 53 个叶子模块 = **63 个 Maven 模块**。

### 3.2 版本基线

| 维度 | 版本 |
|---|---|
| `quarkus-bom` | **3.37.x**（3.37.0/3.37.3，由 quarkus-bom 自身管理） |
| `ddd4j.version` | **2.0.x.20260630-SNAPSHOT** |
| `ddd4j-quarkus.revision` | **3.3.x.20260630-SNAPSHOT** |
| `testcontainers-bom` | **1.20.4** |
| JDK | **17（编译基线）/ 21（CI 矩阵）** |
| Maven | **3.9+** |

### 3.3 Parent 链重构

```
ddd4j-parent  (io.ddd4j:ddd4j-parent:2.0.x.20260630-SNAPSHOT)
    └── ddd4j-quarkus-dependencies  (import ddd4j-dependencies + quarkus-bom)
            ├── ddd4j-quarkus-bom  (扁平 BOM)
            ├── ddd4j-quarkus-parent  (业务 parent，按域 profiles)
            │       └── 业务项目（继承 parent，按需激活 profile）
            └── 框架聚合 pom（mq/extensions/auth/data/cache/ddd/web）
```

**关键决策**：parent 不默认提供业务选型依赖（openapi/health/jwt/fault-tolerance/panache/narayana/mysql/quartz/easyexcel），改按域 profiles 显式激活：

- `-Pddd4j-web`（API 文档 + JWT + 容错 + 健康检查）
- `-Pddd4j-data`（Panache ORM + 事务 + MySQL 驱动）
- `-Pddd4j-schedule`（Quartz）
- `-Pddd4j-excel`（EasyExcel）
- `-Pddd4j-full` = 上述全部 + ddd4j-runtime-quarkus/cache/data-panache/ddd

**原因**：parent 不应替下游项目确定技术选型。框架模块（mq/extensions/auth/data/cache/ddd/web）继承 dependencies 而非 parent，自声明编译依赖（库模块必须自包含）。

## 4. 阶段路线图

| 阶段 | 日期 | 模块范围 | 状态 | Spec |
|---|---|---|---|---|
| **P0** 基线基础设施 | 2026-08-05 | cache / web / mq-core / parent 重构 | ✅ 完成 | [p0-baseline-infrastructure](./2026-08-05-p0-baseline-infrastructure-design.md) |
| **P1** 扩展 Producer 矩阵 | 2026-08-06 | akka/cola/excel/jackson/monitor/qlexpress/dubbo/qrcode/validation | ✅ 完成 | [p1-extensions](./2026-08-06-p1-extensions-design.md) |
| **P2** MQ testcontainers | 2026-08-07 | ddd4j-quarkus-mq-testcontainers + 14 broker | ✅ 完成 | [p2-mq-testcontainers](./2026-08-07-p2-mq-testcontainers-design.md) |
| **P3** Auth + Samples + CI/CD | 2026-08-08 | auth Producer / samples 完整化 / .github/workflows | ✅ 完成 | [p3-auth-samples-ci](./2026-08-08-p3-auth-samples-ci-design.md) |

## 5. 关键复用原则（不重写）

| 来源 | 目标 | 差异 |
|---|---|---|
| `io.ddd4j:ddd4j-core`（所有接口） | 全部模块 | 仅包名 `io.ddd4j.core` 保持不变 |
| `io.ddd4j:ddd4j-cache`（CacheKit + 7 个实现） | cache / 数据 | 不变 |
| `io.ddd4j:ddd4j-web-core` | web / samples | 不变 |
| `io.ddd4j:ddd4j-web-quarkus`（7 文件） | `ddd4j-quarkus-web` | 包名 `io.ddd4j.web.quarkus` → `io.ddd4j.quarkus.web` |
| `io.ddd4j:ddd4j-auth-{satoken,shiro,security}` | auth-* | 不变 |
| `io.ddd4j:ddd4j-mq-core`（MQClient / MQEvent / MQProperties 等 11 个 SPI） | mq-core | 不变 |
| `io.ddd4j:ddd4j-mq-{13 brokers}` | mq-{broker} | 不变 |
| `io.ddd4j:ddd4j-data-jpa`（`JpaAggregateRepository` 抽象） | `ddd4j-quarkus-data-jpa` | 包名调整 |

**转换模式**：

| 来源（Spring） | 目标（Quarkus） | 转换 |
|---|---|---|
| `Ddd4jCacheAutoConfiguration` (`@Configuration`) | `Ddd4jCacheBuildItemProducer` (`@BuildStep`) | `@ConfigurationProperties` → `@ConfigMapping`；`setter` → `@Recorder` + `RuntimeValue` |
| `Ddd4jWebMvcAutoConfiguration` | `Ddd4jWebBuildItemProducer` | 同上 |
| `SaTokenEnhanceAutoConfiguration` (`InitializingBean`) | `Ddd4jSaTokenQuarkusConfig` (`@Observes StartupEvent`) | lifecycle 钩子转换 |
| `MybatisExceptionHandler` (`@ControllerAdvice`) | `MybatisExceptionMapper` (`@Provider`) | Spring 拦截 → JAX-RS ExceptionMapper |
| `*MQEventPublisher` (`@Bean`) | `*MQEventPublisher` (`@Produces @Singleton`) | `@Bean` → `@Produces` |
| `*ConsumerEndpointRegistrar` (`RabbitListenerEndpointRegistry`) | Quarkus 自定义 Registrar（`QuarkusMQListenerRegistrar`） | 反射扫描注解方法 |

## 6. 验收标准

1. **能力覆盖率**：`|ddd4j-quarkus 模块| ≥ |ddd4j-boot 模块| × 80%`，缺失项书面说明原因
2. **测试覆盖率**：每个 Quarkus starter 至少 1 个 `@QuarkusTest` + 1 个 testcontainers 集成测试
3. **版本一致性**：所有模块锁定 `quarkus-bom 3.37.x`、`ddd4j.version 2.0.x.20260630-SNAPSHOT`
4. **依赖一致性**：复用主仓 ddd4j-core/cache/mq/auth/data，不复制实现
5. **API 一致性**：所有 starter 在 `META-INF/quarkus-extension.yaml` 注册，业务可通过 `quarkus extension list` 查看
6. **CI 通过**：`mvn verify` 在 JDK 17 + 21 矩阵 100% 通过；`mvn verify -Pintegration` 跑 testcontainers 通过
7. **文档完整**：每个 starter 有 `README.md`（参考 ddd4j-boot 的 starter 文档）

## 7. 风险与缓解（已记录）

| 风险 | 影响 | 缓解 |
|---|---|---|
| Quarkus 编译时增强（BuildStep）开销大，14 broker 编译慢 | CI 耗时 | 拆分为 `quarkus-mq-{core,broker}` 两层；broker 模块用 `@BuildStep` 缓存 |
| Quarkus 没有 servlet 容器，shiro/satoken-web 部分能力受限 | shiro 集成受阻 | 用 `ContainerRequestFilter` 替代 servlet filter；shiro 走 Vert.x Route 适配 |
| Hibernate 6.6 vs Quarkus 3.37 需 7.4 | 升级复杂 | 在 `ddd4j-quarkus-dependencies` 统一升级 Hibernate 到 7.4.1.Final（仅引用 jakarta.persistence 稳定 API） |
| Testcontainers Docker 镜像在 CI 拉取慢 | CI 超时 | 用 `withReuse(true)` 启用 Docker reuse + 阿里云镜像加速 |
| ddd4j-quarkus 与 ddd4j-boot 在 application.properties 命名差异 | 用户困惑 | 在 README 明确"Quarkus 风格 vs Spring 风格"对照表 |
| 框架模块隐式继承 parent 默认依赖（被全家桶污染） | 不必要依赖污染 | parent 链重构 + 按域 profiles；框架模块继承 dependencies 而非 parent，自声明依赖 |

## 8. 计划偏差记录（实施过程中发现 vs 原始 plan）

| 项 | 旧 plan 描述 | 实际落地 | 偏差原因 |
|---|---|---|---|
| `ddd4j-quarkus-data-jpa` | 补 JpaAggregateRepository/JpaCdiProducer/JpaRepositoryBuildItem | 仅 test（`JpaCdiProducerTest`），main src 留空 | 数据访问抽象统一由主仓 `io.ddd4j:ddd4j-data-jpa` 提供，Quarkus 模块仅适配测试 |
| `ddd4j-quarkus-data-external` | IpRegion + Weather + Producer | 仅 IpRegion（无 Weather/Producer） | 同上，外置模块由主仓提供 |
| `ddd4j-quarkus-extension-pf4j` | 删除（改用 Quarkus Plugin） | 仍存在但空壳 | 计划未实施删除；建议 P4 清理 |
| `ddd4j-quarkus-auth-testcontainers` | 新增 | 未落地 | 4 个 auth 子模块当前只有 Producer，无 testcontainers 集成测试 |
| 版本基线 | quarkus-bom 3.36.3 | quarkus-bom 3.37.x | 升级对齐 3.37.x 系（hibernate 7.4.1 + agroal 3.2） |
| 工期 | 10 周 1 人全职 | 实际多智能体并行约 4 个工作日 | 多智能体协作效率显著高于单人估算 |

## 9. 相关文档

- 主 README: [`../../README.md`](../../README.md)
- 贡献指南: [`../../CONTRIBUTING.md`](../../CONTRIBUTING.md)
- CI 工作流: [`../../.github/workflows/ci.yml`](../../.github/workflows/ci.yml)
- 测试坐标说明（quarkus-junit）：见 [p0 spec § 6 测试策略](./2026-08-05-p0-baseline-infrastructure-design.md#6-测试策略)
