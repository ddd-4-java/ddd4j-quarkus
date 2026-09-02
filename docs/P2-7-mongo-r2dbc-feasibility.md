# P2-7 评估报告：Mongo / R2DBC EventStore for ddd4j-quarkus

**评估日期**：2026-09-02
**评估者**：Agent-7 (architecture review)
**结论**：**本期不实施**，等待真实业务需求出现。

---

## 1. 需求评估

### 1.1 现有证据（均已实测核实）

| 证据 | 命中 |
|---|---|
| ddd4j-quarkus samples 是否使用 Mongo | 无（grep 零结果）|
| ddd4j-quarkus samples 是否使用 R2DBC | 无（grep 零结果）|
| ddd4j-quarkus docs 是否提及 Mongo | 无 |
| ddd4j-quarkus 是否声明 Mongo / R2DBC 运行时依赖 | 无（`ddd4j-quarkus-dependencies` 与 `ddd4j-quarkus-bom` 均零命中）|
| ddd4j 主仓 ADR 是否规划 Mongo EventStore | 无（`docs/adr/0005-event-store-spi.md` 列了 JPA／Panache／JDBI／R2DBC 四套实现，未列 Mongo）|
| 业务项目 bmgw-quarkus-parent 等的真实 Mongo 需求 | 未发现明确文档证据 |
| ddd4j-javalin 是否有 Mongo 集成 | 未发现（全仓 grep 零命中；主仓唯一 mongo 代码级引用是 `ddd4j-kit` `IdKit` 中关于 MongoDB ObjectId 的 javadoc 注释）|
| 主仓依赖 BOM 是否具备 Mongo 基础设施 | 是（`ddd4j-dependencies` 已声明 `mongo-java-driver 3.12.14`、`mongodb-driver-bom 5.6.1`，以及 flowable/flyway/log4j/pac4j/shedlock 等第三方 mongo 集成——但均为第三方桥接，非 ddd4j 自有模块）|

### 1.2 推断

- 当前 ddd4j 生态的 EventStore 实现集中在 **关系型数据库**（JPA/Panache/JDBI/R2DBC），ADR-0005 明确"单轨响应式"由 `R2dbcEventStore` 承担
- **Mongo EventStore 模块在主仓零存在**，意味着主仓维护方未投入 Mongo 资源；依赖 BOM 中的 mongo 版本声明只是为第三方集成兜底
- Quarkus 官方 Mongo 扩展（`quarkus-mongodb-client` / `quarkus-mongodb-panache`，随 quarkus-bom 平台提供）已成熟，但 ddd4j 框架未做桥接
- 业务方若需要 Mongo 集成，目前最直接的路径是 **业务方自行实现 `EventStore` / `AsyncEventStore` SPI 接口**（SPI 表面积仅 4 方法，见 ADR-0005）

---

## 2. 技术可行性

### 2.1 Mongo EventStore 实施方案

**路径 A**：主仓新建 `ddd4j-data-event-store-mongo`
- 工作量：1 个新模块（pom + ~3 Java 类 + IT）
- 风险：Mongo 文档 schema 设计（event collection 字段布局、聚合版本唯一性索引）需要领域专家
- 依赖：`mongodb-driver-bom 5.6.1`（主仓 `ddd4j-dependencies` 已声明，推荐）或 `mongo-java-driver 3.12.14`
- Quarkus 桥接：新建 `ddd4j-quarkus-data-event-store-mongo` Producer（参考现有 `ddd4j-quarkus-data-event-store-panache` 桥接模式）

**路径 B'**：依赖 R2DBC 抽象 + MongoDB R2DBC 驱动
- 工作量：理论上可复用 R2DBC 模块的 SQL schema 语义，但 MongoDB R2DBC 驱动（`io.r2dbc:r2dbc-mongodb`）截至 2026-09 **无官方稳定版**，社区生态薄弱
- 风险：**不推荐**——R2DBC 事件表 schema（TEXT payload、乐观锁列、全局 position 序列）无法无损映射到文档模型

### 2.2 R2DBC EventStore Quarkus 桥接方案

**现状（已核实）**：主仓 `ddd4j-data/ddd4j-data-event-store-r2dbc/` 模块完整存在（`ddd4j-data/pom.xml` 已声明该 module）：
- `io.ddd4j.data.event.store.r2dbc.R2dbcAsyncEventStore`：`AsyncEventStore` SPI 的 Reactor 实现，纯 `io.r2dbc.spi` Connection API（零 Spring，beginTransaction/commit/rollback 真响应式事务），TEXT payload schema、事务内乐观锁校验
- `io.ddd4j.data.event.store.r2dbc.R2dbcEventStore`：同步 `EventStore` 边界适配器（内部委托异步实现，同步边界 block()），两轨道共用同一张表与唯一性规则
- 测试：`R2dbcEventStoreTest`、`R2dbcAsyncEventStoreTest`（r2dbc-h2）+ `R2dbcEventStorePostgresIT`（testcontainers-postgresql）
- 版本管理：`r2dbc-spi 1.0.0.RELEASE`、`reactor-bom 2025.0.6`、各数据库 r2dbc 驱动版本均在主仓 `ddd4j-dependencies` 统一声明

（勘误备注：主仓顶层存在一个同名游离目录 `ddd4j-data-event-store-r2dbc/`，仅含旧布局遗留的 `.flattened-pom.xml` 构建残留，真实模块位于 `ddd4j-data/` 之下，后续可清理。）

**桥接路径**：
- Quarkus Reactive：`quarkus-rest`（Quarkus 3.x 后由 `quarkus-resteasy-reactive` 更名）+ `quarkus-r2dbc-postgresql`/`mysql`（均随 quarkus-bom 平台提供）
- 新建 `ddd4j-quarkus-data-event-store-r2dbc` 模块：
  - 依赖 `io.ddd4j:ddd4j-data-event-store-r2dbc`（主仓）
  - CDI Producer 桥接 `ConnectionFactory` → `R2dbcEventStore` / `R2dbcAsyncEventStore`
  - @BuildStep + @Record 处理 `R2dbcConnectionFactoryBuildItem`（可选，最简形态仅 Producer 即可）
- 工作量：~80 行 Java + 1 个新 pom + 2 个 IT（H2 + Postgres Reactive）
- Quarkus Reactive 生态成熟，BUILD SUCCESS 概率高

**业务需求缺口**：samples 零引用，docs 零提及，无明确业务驱动。

---

## 3. 三种实施路径的工作量对比

| 路径 | 模块数 | Java 类数 | 测试用例 | 文档 | 总工作量估算 | 业务价值 |
|---|---|---|---|---|---|---|
| **A: Mongo EventStore 全栈** | 主仓 1 + Quarkus 1 | 8~10 | 4~6 IT | 2 篇 | ~5 人天 | **未知**（无业务证据）|
| **B: R2DBC Quarkus 桥接** | Quarkus 1 | 3~4 | 2~4 IT | 1 篇 | ~2 人天 | **响应式场景扩展**（潜在）|
| **C: 推迟到后续 Phase** | 0 | 0 | 0 | 1 篇（本报告）| 0.5 人天 | 0（等待需求）|

---

## 4. 推荐

**强烈推荐路径 C（推迟）**，理由：
1. **无业务证据**：samples 零引用，docs 零提及，ADR-0005 未规划 Mongo
2. **投入产出比低**：~5 人天投入但短期零业务回报
3. **资源稀缺**：当前 P0/P1 任务占用了所有开发资源
4. **可恢复性**：Mongo EventStore 是 SPI 层面的扩展（`EventStore` 4 方法 + `AsyncEventStore` 4 方法），业务方在需求出现时可在不影响框架稳定的前提下独立实现

### 触发重新评估的条件

满足以下任一条件时重新启动 P2-7 实施：
1. 出现明确 Mongo 业务需求（具体业务方提供 PRD）
2. ddd4j 主仓 roadmap 出现 `ddd4j-data-event-store-mongo` 模块
3. Quarkus Reactive 生态需要 ddd4j 全栈响应式支持（路径 B 启动）
4. 业务方愿意投入 Mongo / R2DBC 适配工作

### 启动条件下的最小可行路径

若业务需求出现，建议实施 **路径 B（R2DBC 桥接）**：
- 优先于路径 A（Mongo）因为：
  - 主仓 R2DBC EventStore 模块已完整存在（含同步/异步双实现与 H2/Postgres 测试），无需新建
  - Quarkus Reactive 生态成熟（`quarkus-r2dbc-*` 随平台 BOM 提供，无需新增 BOM 导入）
  - 可同时支持 PG/MySQL/MariaDB 等响应式数据库
- 预计工作量 ~2 人天，可在 1 周内交付

---

## 5. 跟踪

本报告生成于 2026-09-02，作为 ddd4j-quarkus 4.0.x 版本线生产就绪化的一部分。

后续 Phase 评审时重读本文档，检查触发条件是否满足。
