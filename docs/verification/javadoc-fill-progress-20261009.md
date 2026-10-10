# 中文 javadoc 补齐进度（feature/3.3.x，2026-10-09）

## 口径

- 扫描范围：全仓 `src/main/java` 与 `src/test/java` 中 public 类/接口/枚举/record、public 方法与 public 字段。
- 判定：public 声明前无 `/**` 即缺失；`@Override` 方法不计入（与 feature/4.0.x 已完成基线一致，4.0.x 同样保留 @Override 不写 javadoc）。
- 基线对照：`git archive feature/4.0.x` 全树解包后用同一扫描器比对，目标为「3.3.x 独有缺失 = 0」。
- 分批：每批 ≤30 文件。

## 扫描器口径的初始缺口

| 范围 | 3.3.x 初始 | 4.0.x 基线 | 说明 |
| --- | --- | --- | --- |
| MAIN | 56 文件 / 194 处声明（1 类 + 193 方法/字段） | 40 文件 / 128 方法（全部 @Override） | 3.3.x 独有缺失 56 文件 |
| TEST | 32 文件 / 87 处声明 | 32 文件 / 87 处声明 | 两分支完全一致（only33=0, only40=0） |

## 分批执行记录

### 批次 1（30 文件）

- 范围：`javadoc_plan.json` 中排序后前 29 个主源码文件 + 1 个误报文件。
- 结果：`files: 29, inserted: 83, unmatched: 1`。
- unmatched 1 例：`PanacheStoredEventEntity.java`（类声明）——复查为**误报**：3.3.x HEAD 已带完整类级 javadoc（第 16-46 行），扫描器向上回溯时被跨 4 行的 `@Table(...)` 注解截断。文件未修改，合规。

### 批次 2（26 文件）

- 范围：`javadoc_plan.json` 剩余 26 个主源码文件。
- 结果：`files: 24, inserted: 108, unmatched: 2, write-needed: 0`。
- unmatched 2 例：`ddd4j-quarkus-sample-mq-disruptor`、`ddd4j-quarkus-sample-mq-kafka` 各 1 处 `OrderCreatedEvent` 构造器——4.0.x 侧签名无 `@JsonProperty` 注解导致签名匹配失败，4.0.x 实际有对应 javadoc，手工对齐插入（含 `@param` × 3）。
- 附带补齐：rabbitmq `OrderResource` 内 `MqConstants.TOPIC`/`MqConstants.TAG` 2 个 public 字段的类级 javadoc（4.0.x 有、3.3.x 缺）。

### 批次 3（测试源码，11 文件 26 处声明）

- 测试缺口两分支完全一致，4.0.x 未补；但任务要求 public 声明清零，故 3.3.x 一并补齐 26 处（计划中 5 处 `port40` 经核实 4.0.x 实际无文档，全部为新写）。
- 覆盖：`JwtSubjectTestResource`、`PanacheEventStoreH2IT`、`ExcelQuarkusTest`、`FileValidationTestResource`、`TestMQClient`、`TestMqListener`、`RocketMqPortLeaseProcessProbe`、`Ddd4jQuarkusWebContractResource`、`DisruptorQuarkusIntegrationTest`、`KafkaQuarkusIntegrationTest`、`QuarkusTestResourceLifecycleManagerWrapper`。
- 全部含规范中文说明及 `@param`/`@return`/`@throws`。

## 补齐后复扫结果（同一扫描器）

```
4.0.x MAIN: files 40 types 1 methods 128
3.3.x MAIN: files 39 types 1 methods 127
MAIN missing only in 3.3.x: 0        <- 达标
4.0.x TEST: files 32 types 1 methods 87
3.3.x TEST: files 26 types 0 methods 61
```

- MAIN：**3.3.x 独有缺失 = 0**，残余 39 文件全部为两分支共有的 @Override 声明（与 4.0.x 基线一致）。
- TEST：残余 61 处全部为 @Override（计划口径中 61 处 @Override），非 @Override 的 26 处已全部补齐；types 由 1 降为 0。

## 计数汇总

| 批次 | 文件数 | 补齐声明数 |
| --- | --- | --- |
| 批次 1（主源码） | 29 | 83 |
| 批次 2（主源码，含 2 手工构造器与 rabbitmq 2 字段） | 26 | 112 |
| 批次 3（测试源码） | 11 | 26 |
| 合计 | 66 个 java 文件 | 221 处 |

（另 4 个 pom.xml 为门禁配置改动，见 `javadoc-gate-evidence-20261009.txt`。）

## 未完成清单

- 无。残余 @Override 声明为两分支共同基线口径，不补。
