# P1 — 扩展 Producer 矩阵实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 9 个扩展模块的 CDI Producer 化（qlexpress/akka/cola/excel/jackson/monitor/dubbo/qrcode/validation），每个扩展新增 `@QuarkusTest`，新增 ddd4j-quarkus-extension-dubbo
**Architecture:** Spring `@ConfigurationProperties` → Quarkus `@ConfigMapping`；Spring `@ConditionalOnProperty` → `@IfBuildProperty`；Spring `@AutoConfiguration` → `META-INF/quarkus-extension.yaml`
**Tech Stack:** Quarkus 3.37.0 + ddd4j 2.0.x.20260630-SNAPSHOT + JDK 17 + smallrye-config
**Related Design Doc:** [../specs/2026-08-06-p1-extensions-design.md](../specs/2026-08-06-p1-extensions-design.md)

## 全局约定

- 所有 Producer 命名：`XxxCdiProducer` 或 `Ddd4jXxxCdiProducer`
- 所有 ConfigMapping：`XxxConfig` 接口 + `@ConfigMapping(prefix = "ddd4j.<module>")`
- 所有 QuarkusTest：`XxxQuarkusTest` + `@QuarkusTest` + 默认开启条件装配
- 库模块（继承 ddd4j-quarkus-dependencies）必须自声明编译依赖（jakarta.ws.rs-api 等）

## 实施阶段总览

```
Stage 1 — qlexpress（11 main + 1 test，最大）
Stage 2 — akka（5 main + 1 test，ActorSystem CDI 适配）
Stage 3 — cola（4 main + 1 test，ExceptionMapper）
Stage 4 — excel（4 main + 1 test，ServerResponseFilter）
Stage 5 — jackson（2 main + 1 test，ObjectMapperCustomizer）
Stage 6 — monitor（2 main + 1 test，robot webhook）
Stage 7 — dubbo（3 main + 1 test，★ 新增）
Stage 8 — qrcode（4 main + 1 test，★ 新增）
Stage 9 — validation（8 main + 2 test，★ 新增）
Stage 10 — 全量验证 + 提交
```

## Stage 1 — qlexpress 扩展

- [x] **Step 1.1: QLExpressConfig + QLExpressCdiProducer**
  - 文件: `ddd4j-quarkus-extension-qlexpress/src/main/java/io/ddd4j/quarkus/qlexpress/{QLExpressConfig,QLExpressCdiProducer,QLExpressEngine,RuleService,RuleRepository,...}.java`
  - 操作: 11 个 main 类（QLExpressEngine + RuleService + RuleRepository + Redis 缓存 + ...）
  - 验证: 编译通过

- [x] **Step 1.2: QLExpressQuarkusTest**
  - 文件: `src/test/java/io/ddd4j/quarkus/qlexpress/QLExpressQuarkusTest.java`
  - 操作: `@QuarkusTest` + `@Inject` RuleService + 表达式求值验证
  - 验证: 3 个测试方法通过

## Stage 2 — akka 扩展

- [x] **Step 2.1: AkkaConfig + AkkaCdiProducer + AkkaCdiActorProducer**
  - 文件: `ddd4j-quarkus-extension-akka/src/main/java/io/ddd4j/quarkus/akka/{AkkaConfig,AkkaCdiProducer,AkkaCdiActorProducer,AkkaExtensionRegistry}.java`
  - 操作: 5 个 main 类（AkkaProperties + ActorSystem CDI Producer + AkkaExtensionRegistry）
  - 验证: 编译通过（不依赖 Spring ActorProducer）

- [x] **Step 2.2: AkkaQuarkusTest**
  - 文件: `src/test/java/io/ddd4j/quarkus/akka/AkkaQuarkusTest.java`
  - 验证: 2 个测试方法通过

## Stage 3 — cola 扩展

- [x] **Step 3.1: ColaConfig + ColaCdiProducer + 2 ExceptionHandler**
  - 文件: `ddd4j-quarkus-extension-cola/src/main/java/io/ddd4j/quarkus/cola/{ColaConfig,ColaCdiProducer,ColaExceptionHandler,ColaSysExceptionHandler}.java`
  - 操作: 4 个 main 类（含 BizException/SysException → HTTP 200/500）
  - 验证: 编译通过

- [x] **Step 3.2: ColaQuarkusTest**
  - 验证: 2 个测试方法通过

- [x] **Step 3.3: pom 显式声明 jakarta.ws.rs-api**
  - 文件: `pom.xml`
  - 操作: 因父链重构后不再隐式继承默认 jakarta 依赖，库模块必须自声明
  - 验证: clean compile 通过

## Stage 4 — excel 扩展

- [x] **Step 4.1: ExcelConfig + Ddd4jExcelCdiProducer + ExcelHttpKit**
  - 文件: `ddd4j-quarkus-extension-excel/src/main/java/io/ddd4j/quarkus/excel/*.java`
  - 操作: 4 个 main 类（ExcelConfig + CDI Producer + ServerResponseFilter 适配）
  - 验证: 编译通过

- [x] **Step 4.2: ExcelQuarkusTest**
  - 验证: 3 个测试方法通过

## Stage 5 — jackson 扩展

- [x] **Step 5.1: JacksonConfig + DefaultJacksonObjectMapperCustomizer**
  - 文件: `ddd4j-quarkus-extension-jackson/src/main/java/io/ddd4j/quarkus/jackson/*.java`
  - 操作: 2 个 main 类（ObjectMapperCustomizer + Module CDI Producer）
  - 验证: 编译通过

- [x] **Step 5.2: DefaultJacksonObjectMapperCustomizer 改用 ConfigProvider**
  - 文件: `DefaultJacksonObjectMapperCustomizer.java`
  - 操作: 用 MicroProfile `ConfigProvider` 直接读（避免 ConfigMapping STATIC_INIT 时序问题 SRCFG00027）
  - 验证: 启动正常

- [x] **Step 5.3: JacksonQuarkusTest**
  - 验证: 4 个测试方法通过

## Stage 6 — monitor 扩展

- [x] **Step 6.1: MonitorConfig + Ddd4jMonitorCdiProducer**
  - 文件: `ddd4j-quarkus-extension-monitor/src/main/java/io/ddd4j/quarkus/monitor/*.java`
  - 操作: 2 个 main 类（DingDingRobotSender + QiWeiRobotSender + Logback Appender CDI Producer）
  - 验证: 编译通过

- [x] **Step 6.2: MonitorConfig @WithDefault("") → Optional<String>**
  - 文件: `MonitorConfig.java`
  - 操作: 避免 SRCFG00040 空字符串解析错误
  - 验证: 启动正常

- [x] **Step 6.3: MonitorQuarkusTest**
  - 验证: 2 个测试方法通过

## Stage 7 — dubbo 扩展（★ 新增）

- [x] **Step 7.1: DubboConfig + MonitorConfig + DubboCdiProducer**
  - 文件: `ddd4j-quarkus-extension-dubbo/src/main/java/io/ddd4j/quarkus/dubbo/*.java`
  - 操作: 3 个 main 类（ApplicationConfig/RegistryConfig/ProtocolConfig CDI 暴露）
  - 验证: 编译通过

- [x] **Step 7.2: DubboExceptionMapper**
  - 文件: 同上
  - 操作: `@Provider` + `ExceptionMapper<RpcException>` → HTTP 200/500
  - 验证: 编译通过

- [x] **Step 7.3: DubboConfig @WithDefault("") → Optional<String> + producers 适配**
  - 文件: `DubboCdiProducer.java`
  - 操作: `ifPresent/orElse("")` 避免 SRCFG00040
  - 验证: 启动正常

- [x] **Step 7.4: pom 显式声明 jakarta.ws.rs-api**
  - 文件: `pom.xml`
  - 验证: clean compile 通过

- [x] **Step 7.5: DubboQuarkusTest**
  - 验证: 3 个测试方法通过

## Stage 8 — qrcode 扩展（★ 新增）

- [x] **Step 8.1: QrCodeConfig + QrCodeProducer**
  - 文件: `ddd4j-quarkus-extension-qrcode/src/main/java/io/ddd4j/quarkus/qrcode/*.java`
  - 操作: 4 个 main 类（DefaultQrCodeService + concurrency/maxBatchSize）
  - 验证: 编译通过

- [x] **Step 8.2: pom 增加 zxing-extension 依赖**
  - 文件: `pom.xml`
  - 操作: 依赖由 ddd4j-quarkus-dependencies 管理版本
  - 验证: clean compile 通过

- [x] **Step 8.3: QrCodeQuarkusTest**
  - 验证: 2 个测试方法通过

## Stage 9 — validation 扩展（★ 新增）

- [x] **Step 9.1: FileValidationConfig + QuarkusFileValidationProducer**
  - 文件: `ddd4j-quarkus-extension-validation/src/main/java/io/ddd4j/quarkus/validation/*.java`
  - 操作: 8 个 main 类（配置 + Producer + 校验器）
  - 验证: 编译通过

- [x] **Step 9.2: FileValidationQuarkusTest**
  - 验证: 3 个测试方法通过

## Stage 10 — 全量验证

- [x] **Step 10.1: clean test-compile**
  - 验证: `mvn clean test-compile` 全量通过

- [x] **Step 10.2: verify -Pintegration**
  - 验证: 318 个测试 0 失败

- [x] **Step 10.3: 提交**
  - 提交: `feat(extensions): 完成 9 个扩展模块 Producer 化 + @QuarkusTest 覆盖`（commit 1f62eba）

## Self-Review / 完成校验

- [x] 9 个扩展模块（含 plan 外 dubbo/validation/qrcode）全部 Producer 化
- [x] 每个扩展至少 1 个 `@QuarkusTest`
- [x] cola/dubbo 显式声明 jakarta.ws.rs-api 后编译通过
- [x] jackson customizer 适配 ConfigMapping 时序
- [x] ddd4j-quarkus-extension-dubbo 新增完成
- [x] 整体 mvn verify -Pintegration 通过（318 测试 0 失败）

## 后续待办（非本次范围）

- [ ] 删除 `ddd4j-quarkus-extension-pf4j`（空壳）
- [ ] 补充 ddd4j-quarkus-data-jpa 与 ddd4j-quarkus-data-external 的 main src
