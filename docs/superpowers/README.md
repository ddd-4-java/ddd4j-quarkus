# ddd4j-quarkus 超级规范（Superpowers）

本目录是 ddd4j-quarkus 项目的**设计与实施规范体系**，遵循 superpowers 协作框架：

- **spec** = 设计（Why / What）— 由架构师评审，回答"为何这么做、模块/接口长什么样"
- **plan** = 实施步骤（How）— 由 AI 代理按 checkbox 逐步执行

## 与历史计划的关系

- 本目录**取代** `.zcode/plans/plan-sess_5963fbf4-*.md`（旧 ZCode session plan，不在 git 控制内）
- 总览 spec 的核心内容已**吸收**旧 plan 的"对齐 ddd4j-boot 框架集成"核心内容并标注来源
- 旧 plan 的 10 周详细工作量估算**已过时**（实际按多智能体并行协作完成）

## 索引

| 类型 | 文件 | 说明 |
|---|---|---|
| 总览 spec | [specs/2026-08-05-quarkus-alignment-overview-design.md](specs/2026-08-05-quarkus-alignment-overview-design.md) | ddd4j-quarkus 整体设计、阶段路线图、验收标准、计划偏差 |
| P0 spec | [specs/2026-08-05-p0-baseline-infrastructure-design.md](specs/2026-08-05-p0-baseline-infrastructure-design.md) | Cache / Web / MQ 三件套 + parent 链重构 |
| P0 plan | [plans/2026-08-05-p0-baseline.md](plans/2026-08-05-p0-baseline.md) | P0 实施步骤（已完成 ✅） |
| P1 spec | [specs/2026-08-06-p1-extensions-design.md](specs/2026-08-06-p1-extensions-design.md) | 9 个扩展 Producer 矩阵（含 dubbo/validation/qrcode 新增） |
| P1 plan | [plans/2026-08-06-p1-extensions.md](plans/2026-08-06-p1-extensions.md) | P1 实施步骤（已完成 ✅） |
| P2 spec | [specs/2026-08-07-p2-mq-testcontainers-design.md](specs/2026-08-07-p2-mq-testcontainers-design.md) | 14 broker testcontainers 设计 |
| P2 plan | [plans/2026-08-07-p2-mq-testcontainers.md](plans/2026-08-07-p2-mq-testcontainers.md) | P2 实施步骤（已完成 ✅） |
| P3 spec | [specs/2026-08-08-p3-auth-samples-ci-design.md](specs/2026-08-08-p3-auth-samples-ci-design.md) | Auth + samples + CI/CD |
| P3 plan | [plans/2026-08-08-p3-auth-samples-ci.md](plans/2026-08-08-p3-auth-samples-ci.md) | P3 实施步骤（已完成 ✅） |

## 规范骨架

### spec 模板

```markdown
# <主题> 设计

- 日期: YYYY-MM-DD
- 作者:
- 状态: 待评审 | 设计已确认 | 待实施 | 实施已对齐
- 范围:
- 涉及模块:

## 1. 背景与问题
## 2. 目标
## 3. 总体架构
## 4. 核心抽象
## 5. 关键文件
## 6. 测试策略
## 7. 风险与缓解
## 8. 验收标准
## 9. 相关文档
```

### plan 模板

```markdown
# <主题> 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 一句话目标
**Architecture:** 关键设计决策摘要
**Tech Stack:** 已有/新增依赖
**Related Design Doc:** docs/superpowers/specs/...

## 全局约定
## 实施阶段总览
## Stage 1 — <标题>
- [ ] **Step 1.1: <动词>**（文件/操作/验证/提交四元组）
## Stage N — ...
## Self-Review / 完成校验
```

## 命名规范

- **文件名**：`YYYY-MM-DD-slug[-design].md`
- **日期**：推断的实际阶段代表日（liteflow 惯例使用未来日期标注已完成阶段）
- **slug**：kebab-case 英文短语（中文主题用对应英文）
- **spec 后缀**：固定 `-design.md`
- **plan 后缀**：无
- **spec 与 plan**：同 stem 一一对应

## 状态字段（受控词表）

| 状态 | 含义 |
|---|---|
| `待评审` | spec 草案，等待架构师评审 |
| `设计已确认` | spec 已通过评审，等待实施 |
| `实施已对齐` | 实际代码与 plan 步骤一致 |
| `进行中` | 部分 Step 完成 |
| `已搁置` | 暂不实施，登记原因 |

## 项目链接

- 主 README: [../../README.md](../../README.md)
- 贡献指南: [../../CONTRIBUTING.md](../../CONTRIBUTING.md)
- CI 工作流: [../../.github/workflows/ci.yml](../../.github/workflows/ci.yml)
- 父 POM（parent）: [../../ddd4j-quarkus-parent/pom.xml](../../ddd4j-quarkus-parent/pom.xml)
- 版本 BOM（dependencies）: [../../ddd4j-quarkus-dependencies/pom.xml](../../ddd4j-quarkus-dependencies/pom.xml)
- 扁平 BOM: [../../ddd4j-quarkus-bom/pom.xml](../../ddd4j-quarkus-bom/pom.xml)

## 分支与版本线

| 分支 | ddd4j 底座 | 构建工具 | 说明 |
|---|---|---|---|
| `feature/3.3.x` | `ddd4j 2.0.x`（Maven 3 / POM 4.0.0） | Maven 3.9+ | 当前稳定线（288 测试） |
| `feature/4.0.x` | `ddd4j 3.0.x`（目标 Maven 4 / POM 4.1.0） | `./mvnw`（4.0.0-rc-6 暂未启用） | **占位分支**（与 Quarkus 3.x 集成测试暂不兼容）——实测发现 Quarkus 3.37 的 BootstrapAppModelFactory 仍按 Maven 3 关键字（`project` / `<module>`）解析，4.0.0-rc-6 引入的 `managingProject` / `<subproject>` 重命名会令 Quarkus 拿到 `appArtifact=null` → surefire fork 报 "Failed to create the application model for null"。本分支当前保持 POM 4.0.0 + Maven 3 wrapper（实测 232 测试全绿）。**升级到 Maven 4 必须等 Quarkus 4.x（或 PR 修复 BootstrapAppModelFactory 兼容）**——待办登记于本表 |

## 当前快照

- **阶段状态**：P0 / P1 / P2 / P3 全部完成
- **测试总数**：318 个（0 失败）
- **集成测试**：14 broker testcontainers 真实运行通过
- **CI**：3 阶段（workflow-lint + JDK 17/21 build matrix + infrastructure-integration）
- **生产用户**：cloud-das（com.bmgw），版本管理已整体移交 ddd4j-quarkus

## 已登记的待办（不在 P0-P3 范围）

- [x] 删除 `ddd4j-quarkus-extension-pf4j`（空壳）— 已完成（2026-08-16，随 data-panache 缺陷修复一并提交）
- [x] 修复 data-panache 缺陷（TenantAwareEntity strategy 类名 / 空 filters HQL / IdGeneratorProducer CDI 通配符违规）— 已完成（2026-08-16，新增 2 个 CDI 装配测试）
- [ ] 新增 `ddd4j-quarkus-auth-testcontainers` 共享 fixture + 4 个 auth 子模块集成测试
- [ ] 补齐 `ddd4j-quarkus-data-jpa` main src（当前仅 test）
- [ ] 补齐 `ddd4j-quarkus-data-external` WeatherQuarkusAdapter + QuarkusExternalCdiProducer
- [ ] 完整化 `sample-rich-model` / `sample-auth-*` / `sample-mq-*`
