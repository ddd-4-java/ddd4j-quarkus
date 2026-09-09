# feature/3.3.x P5-A 能力同步设计

> 状态：规格确认，待实施计划
>
> 目标分支：`feature/3.3.x`
>
> 参考分支：`feature/4.0.x`

## 1. 背景

`feature/4.0.x` 已完成 P5-A 本地消费者基线：已发布 ddd4j 快照的空缓存消费、Web 请求生命周期、License 真实安装验签，以及不修改上游源码的 CI 解析链路均有本地证据。

`feature/3.3.x` 是独立维护线，仍必须保持自己的平台约束：

- ddd4j：`2.0.x`
- ddd4j-quarkus revision：`3.3.x.20260630-SNAPSHOT`
- Quarkus BOM 与 Maven 插件：`3.37.4`
- Java：17 为基线，CI 同时验证 17 与 21
- Maven Model：`4.0.0`
- 聚合结构：`<modules>`

本变更不是把 `feature/4.0.x` 合并或逐提交照搬到 `feature/3.3.x`，而是把 P5-A 的可观察能力按 3.3.x 合约重新实现和验证。

## 2. 目标

1. 建立 ddd4j 2.0.x 已发布制品的空缓存消费者证据。
2. 让 Web 集成测试证明租户、请求 ID 传播和请求结束后的线程上下文清理。
3. 让 License 集成测试证明真实签发、安装、验签以及安全的临时文件生命周期。
4. 将 CI 从“checkout 并修改 ddd4j 源码”改为“使用组织 Maven settings 消费已发布制品”。
5. 让 actionlint 错误真实阻断后续 CI job。
6. 同步忽略 Paho MQTT 客户端生成的 UUID 锁目录。
7. 用 3.3.x 自己的完整构建、测试总数和 skip 原因更新文档。

## 3. 非目标

- 不修改 `master`。
- 不合并 `feature/4.0.x` 历史。
- 不把 ddd4j 3.0.x、Maven Model 4.1.0 或 `<subprojects>` 引入 3.3.x。
- 不复制 4.0.x 的测试数量、完成状态或 Quarkus 4 前瞻结论。
- 不借此变更补齐 P5-B 至 P5-E。
- 不宣称 Native、Dev Mode、GitHub-hosted Actions、Maven deploy 或云服务已经验收。
- 不将本地 MQTT `.lck` 或 UUID 运行目录提交到 Git。

## 4. 分支同步策略

### 4.1 可直接迁移的通用能力

以下能力与 ddd4j 主版本无关，可以复用 4.0.x 的设计，但仍需在 3.3.x 单独测试：

- Maven settings 经权限为 `0600` 的同目录临时文件解码。
- 在临时文件上校验仓库 server id，成功后原子替换 `settings.xml`。
- 解码或校验失败时保留原 settings，并清除临时凭据文件。
- `reviewdog/action-actionlint` 显式配置 `fail_level: error` 和 `filter_mode: nofilter`。
- `.gitignore` 忽略 `**/ddd4j-mq-*-tcplocalhost*/`。

### 4.2 必须重新适配的能力

以下内容依赖 3.3.x/ddd4j 2.0.x API，禁止机械 cherry-pick 后直接宣称完成：

- 远端消费者的 artifact 坐标和版本。
- BOM/dependencyManagement 中的 ddd4j 上游模块。
- Web filter 的发现、优先级、ThreadContext 字段和清理顺序。
- LicenseKit、LicenseCreator、LicenseVerify 和 TrueLicense 传递依赖。
- Java 17/21 CI 初始化顺序。
- broker skip 条件及其平台理由。

### 4.3 明确排除的 4.0.x 内容

- `3.0.x.20260630-SNAPSHOT` 坐标。
- Maven 4 wrapper、Model 4.1.0 与 `<subprojects>` 阻塞说明。
- 4.0.x 专属 POM 结构、模块差异和测试统计。
- 仅存在于 ddd4j 3.0.x 的接口或实现。

## 5. 版本解析门禁

当前 3.3.x 根 POM 同时出现：

- parent `io.ddd4j:ddd4j-parent:2.0.x.20260630-SNAPSHOT`
- 属性 `ddd4j.version=2.0.x.20260730-SNAPSHOT`

实施不得预设哪一个是正确最终版本。第一任务必须从全新 Maven 本地仓库验证以下制品的远端可消费性：

- `io.ddd4j:ddd4j-parent`
- `io.ddd4j:ddd4j-dependencies`
- `io.ddd4j:ddd4j-runtime-quarkus`
- `io.ddd4j:ddd4j-web-quarkus`
- `io.ddd4j:ddd4j-extension-license`

判定规则：

1. 优先验证当前声明的 `2.0.x.20260730-SNAPSHOT` 完整依赖集合。
2. parent 与依赖集合必须属于同一条可解析版本线；不能只因单个 JAR 存在就通过。
3. 若 `20260730` 不完整，则记录缺失坐标和仓库响应，再验证已发布的 `20260630` 集合。
4. 选定版本必须统一写入 parent、`ddd4j.version`、BOM、dependencies 和远端消费者 fixture。
5. 不允许依赖开发者现有 `~/.m2` 缓存得出结论。

## 6. Web 生命周期契约

### 6.1 消费者行为

测试资源端点必须通过实际发布的 `ddd4j-web-quarkus` filter 处理请求，并验证：

- 请求头中的租户标识进入 `ThreadContext`。
- 响应中存在非空请求 ID。
- Authorization 等请求态不会越过请求边界泄漏。

### 6.2 清理证据

第二个 HTTP 请求可能被调度到不同工作线程，因此不能单独作为清理证据。3.3.x 必须采用测试专用生命周期探针：

- 探针与业务资源在同一服务线程执行。
- 探针的 response-filter 顺序位于上游 ddd4j filter 清理之后。
- 探针读取 tenant、request-id、Authorization 等状态并记录结果。
- 客户端收到响应后断言这些状态已经恢复或清除。

禁止复制 ddd4j 的生产 filter 到 ddd4j-quarkus。

## 7. License 生命周期契约

### 7.1 启动顺序

Quarkus test profile 必须在 CDI 创建 `LicenseVerify` 之前完成：

1. 创建本次 JVM/测试运行唯一的临时目录。
2. 生成私钥库和公钥库。
3. 签发临时许可证。
4. 返回 License 配置覆盖。

测试必须真实断言：

- `LicenseVerify` Bean 可注入。
- `isInstallSuccess()` 为 true。
- `verify()` 为 true。

### 7.2 文件与秘密边界

- 临时目录位于 `java.io.tmpdir` 下并具有唯一名称。
- POSIX 平台目录权限为 `0700`，keystore/license 文件为 `0600`。
- 非 POSIX 平台兼容执行，不伪造权限结论。
- 测试口令只存在于测试常量，不打印到日志。
- 应用关闭后递归清理目录。
- `Files.walk` 必须关闭；删除失败不得静默吞掉。
- 签发失败必须进入同一清理路径，不能遗留系统属性或密钥文件。

### 7.3 依赖治理

`ddd4j-extension-license` 应由 3.3.x BOM 管理。只有空缓存 dependency tree 证明其发布 POM 仍缺少可解析的 TrueLicense 版本时，才保留叶模块的直接 `truelicense-core` 依赖，并在文档中记录原因。

## 8. CI 设计

### 8.1 Maven 配置动作

新的 composite action 只负责：

1. 校验 `MAVEN_SETTINGS_XML` 已配置。
2. 安全安装 Maven settings。
3. 通过临时空 Maven 仓库和 Wagon transport 执行远端消费者 `dependency:go-offline`。

它不得：

- checkout ddd4j 源码。
- 使用 `sed` 修改上游 POM。
- 从 settings 中解析或输出用户名、密码。
- 在命令行拼接含凭据的 URL。

### 8.2 Workflow

- `workflow-lint` 必须真实运行 actionlint 并在 error 时失败。
- unit/contract job 保留 Java 17 与 21 矩阵。
- 每个需要私有 Maven 制品的 job 都先执行 Java setup，再执行 Maven 配置动作。
- broker matrix 保留 3.3.x 当前平台 skip 和独立报告策略。
- 两类 Maven job 都使用同一远端解析动作，不保留旧 `install-ddd4j` action。

## 9. 测试与验收

实施按以下门禁串行推进：

1. 空缓存远端消费者解析成功，并记录实际快照版本和时间戳。
2. Web 定向测试与 Web 模块回归成功。
3. License 定向测试与 License 模块回归成功。
4. actionlint 对合法 workflow 返回 0，对临时非法 workflow 返回非 0。
5. Java 17 完整 `clean verify` 成功。
6. Java 21 完整或与 CI 等价的兼容性验证成功。
7. 从最终 Surefire/Failsafe XML 汇总 suites、tests、failures、errors、skipped。
8. 每个 skip 按类、方法和原始理由报告。
9. 扫描确认不存在源码修改式 ddd4j 安装、陈旧坐标或 4.0.x/Maven 4 内容误入。
10. 独立代码审查确认分支契约、测试真实性、CI 安全和文档边界。

`-Denforcer.skip=true` 只表示 Enforcer 未纳入本阶段门禁，文档必须明确保留这一限制。

## 10. 文档与状态

3.3.x 文档只记录本分支实际观察到的：

- 最终 ddd4j 2.0.x 版本矩阵。
- 空缓存消费者命令与结果。
- Web、License 和完整 reactor 结果。
- XML 测试总数及 skip。
- CI 已改为消费发布制品的静态设计。
- GitHub Actions hosted、push、deploy 是否实际执行。

只有本规格第 9 节门禁全部通过，才能将状态更新为“feature/3.3.x P5-A 本地完成”。这不代表 `feature/4.0.x`、`master`、P5-B 至 P5-E 或生产发布验收发生变化。

## 11. Git 与工作区约束

- 本次经用户明确允许使用现有 `ddd4j-quarkus-wt-33x` worktree。
- 不创建额外 worktree。
- 实施前保护现有两个 MQTT UUID 运行目录；它们不得暂存或提交。
- 每个任务限定文件所有权、独立提交、独立审查。
- 不重写已推送历史，不使用 force push。
- GitHub 与 Codeup 推送属于实现完成后的独立发布步骤；执行后必须核对本地及两个远端 SHA。

## 12. 风险与控制

| 风险 | 控制 |
|---|---|
| 2.0.x 快照版本不完整 | 第一门禁使用空缓存验证完整坐标集合 |
| 机械回迁带入 Maven 4 | 对 modelVersion、modules 和 wrapper 做结构检查 |
| Web 清理测试跨线程假阳性 | 使用同线程、清理后 response probe |
| License 测试泄露密钥或并发互删 | 唯一目录、严格权限、失败及关闭后清理 |
| CI 日志泄露 Maven 凭据 | 临时文件、原子替换、禁止解析和输出秘密 |
| actionlint 只报告不阻断 | 显式 fail level 并做负向验证 |
| 复制 4.0.x 验证结论 | 只从 3.3.x 最终 XML 和日志生成文档 |
| master 被错误同步 | 本规格明确排除 master |

## 13. 完成定义

本变更完成必须同时满足：

- 3.3.x 分支合约保持不变。
- P5-A 五类能力均有本分支运行证据。
- 所有 Critical/Important 审查问题完成修正和复审。
- 跟踪工作区干净，MQTT 运行目录被正确忽略。
- 文档不扩大本地证据的含义。
- 如获发布授权，GitHub、Codeup 与本地 HEAD SHA 一致。

