# P1 — 扩展 Producer 矩阵设计（9 个跨领域扩展）

- 日期: 2026-08-06
- 作者: ddd-4-java（多智能体并行）
- 状态: 设计已确认 / 实施已对齐
- 范围: ddd4j-quarkus-extensions/{qlexpress,akka,cola,excel,jackson,monitor,dubbo,qrcode,validation}
- 涉及模块: 9 个扩展模块

## 1. 背景与问题

历史实现中，9 个扩展模块大量空壳（pom 仅有依赖），业务项目继承时无 CDI Bean 可注入。Spring 轨 ddd4j-boot 通过 `@AutoConfiguration` + `@EnableConfigurationProperties` 实现，Quarkus 轨需要转换为 `@BuildStep` + `@ConfigMapping` + `@Produces` 原生范式。

## 2. 目标

1. **9 个扩展全部 CDI 化**：Producer 暴露 `@ConfigMapping` 配置 + `@IfBuildProperty` 条件装配
2. **`@QuarkusTest` 覆盖每个扩展**：startup + bean 注入 + 配置覆盖
3. **新增 ddd4j-quarkus-extension-dubbo**：对齐 ddd4j-boot 的 3 文件实现
4. **修复历史隐患**：cola/dubbo 的 `ExceptionMapper` 因父链重构丢失 jakarta.ws.rs-api 依赖

## 3. 总体架构

### 3.1 转换模式

```
Spring 轨                                              Quarkus 轨
─────────────────────────────────────────────────────────────────────
@ConfigurationProperties                                @ConfigMapping(prefix = "ddd4j.ext.xxx")
@Configuration + @Bean                                 @ApplicationScoped + @Produces @Singleton
@ConditionalOnProperty                                 @IfBuildProperty(name = "ddd4j.ext.xxx.enabled", enableIfMissing = true)
@AutoConfiguration                                     META-INF/quarkus-extension.yaml
```

### 3.2 扩展清单

| 扩展 | Producer 类 | ConfigMapping | 测试类 |
|---|---|---|---|
| qlexpress | `QLExpressCdiProducer` | `QLExpressConfig` | `QLExpressQuarkusTest` |
| akka | `AkkaCdiProducer` + `AkkaCdiActorProducer` | `AkkaConfig` | `AkkaQuarkusTest` |
| cola | `ColaCdiProducer` | `ColaConfig` | `ColaQuarkusTest` |
| excel | `Ddd4jExcelCdiProducer` | `ExcelConfig` | `ExcelQuarkusTest` |
| jackson | `JacksonConfig` + `DefaultJacksonObjectMapperCustomizer` | `JacksonConfig` | `JacksonQuarkusTest` |
| monitor | `Ddd4jMonitorCdiProducer` | `MonitorConfig` | `MonitorQuarkusTest` |
| dubbo | `DubboCdiProducer` | `DubboConfig` + `MonitorConfig` | `DubboQuarkusTest` |
| qrcode | `QrCodeProducer` | `QrCodeConfig` | `QrCodeQuarkusTest` |
| validation | `QuarkusFileValidationProducer` | `FileValidationConfig` | `FileValidationQuarkusTest` |

**计划外新增**：dubbo（旧 plan 未列，对齐 ddd4j-boot 3 文件）；validation（计划未列，业务侧需求）；qrcode（计划未列，对齐 ddd4j-boot）。

**未实现删除**：`ddd4j-quarkus-extension-pf4j`（旧 plan W7 要求删除，实际仍存在但为空壳，登记为 P4 待办）。

## 4. 核心抽象

### 4.1 典型 Producer 模板（以 cola 为例）

```java
@ApplicationScoped
public class ColaCdiProducer {
    @Inject ColaConfig config;
    
    @Produces
    @Singleton
    public ColaResponseHandler responseHandler() {
        return new ColaResponseHandler(config.isBizFailFast());
    }
    
    @Produces
    @Singleton
    @IfBuildProperty(name = "ddd4j.cola.exception-handler", stringValue = "true", enableIfMissing = true)
    public ColaExceptionHandler exceptionHandler() {
        return new ColaExceptionHandler();
    }
}
```

### 4.2 ConfigMapping 模板

```java
@ConfigMapping(prefix = "ddd4j.cola")
public interface ColaConfig {
    @WithName("biz-fail-fast") @WithDefault("true") boolean isBizFailFast();
    @WithName("sys-fail-fast") @WithDefault("true") boolean isSysFailFast();
}
```

## 5. 关键文件

```
ddd4j-quarkus/ddd4j-quarkus-extensions/
├── pom.xml                                                                   # 聚合 POM
├── ddd4j-quarkus-extension-qlexpress/
│   ├── pom.xml
│   ├── src/main/java/io/ddd4j/quarkus/qlexpress/{QLExpressConfig,QLExpressCdiProducer,QLExpressEngine,RuleService,RuleRepository,...}.java
│   └── src/test/java/io/ddd4j/quarkus/qlexpress/QLExpressQuarkusTest.java
├── ddd4j-quarkus-extension-akka/{AkkaConfig,AkkaCdiProducer,AkkaCdiActorProducer,AkkaExtensionRegistry}.java + AkkaQuarkusTest
├── ddd4j-quarkus-extension-cola/{ColaConfig,ColaCdiProducer,ColaExceptionHandler,ColaSysExceptionHandler}.java + ColaQuarkusTest
├── ddd4j-quarkus-extension-excel/{ExcelConfig,Ddd4jExcelCdiProducer,ExcelHttpKit}.java + ExcelQuarkusTest
├── ddd4j-quarkus-extension-jackson/{JacksonConfig,DefaultJacksonObjectMapperCustomizer}.java + JacksonQuarkusTest
├── ddd4j-quarkus-extension-monitor/{MonitorConfig,Ddd4jMonitorCdiProducer}.java + MonitorQuarkusTest
├── ddd4j-quarkus-extension-dubbo/{DubboConfig,MonitorConfig,DubboCdiProducer,DubboExceptionMapper}.java + DubboQuarkusTest
├── ddd4j-quarkus-extension-qrcode/{QrCodeConfig,QrCodeProducer}.java + QrCodeQuarkusTest
└── ddd4j-quarkus-extension-validation/{FileValidationConfig,QuarkusFileValidationProducer}.java + FileValidationQuarkusTest
```

## 6. 测试策略

| 测试类型 | 验证目标 | 测试方法 |
|---|---|---|
| `@QuarkusTest` startup | Quarkus 启动 + CDI 注入 + ConfigMapping 解析 | `@Inject` 注入 Producer 暴露的 Bean |
| ConfigMapping 校验 | 默认值 + 自定义值覆盖 | `@QuarkusTestResource` 或 `application.properties` |
| 条件装配 | `@IfBuildProperty` 生效 | 通过 `@ConfigOverride` 切换属性 |
| 异常映射 | `ExceptionMapper` 转换正确 | REST 调用 + 断言响应 |

## 7. 风险与缓解

| 风险 | 缓解 |
|---|---|
| cola/dubbo `ExceptionMapper` 编译失败（jakarta.ws.rs-api 缺失） | 框架模块显式声明 `jakarta.ws.rs-api` 依赖（库模块自包含） |
| SmallRye Config `@WithDefault("")` 空字符串触发 SRCFG00040 | 改为 `Optional<String>` |
| ConfigMapping 注册时序问题（ObjectMapper STATIC_INIT 早于 mapping 注册 → SRCFG00027） | jackson customizer 改用 MicroProfile `ConfigProvider` 直接读 |
| dubbo `@WithDefault("")` → SRCFG00040 | 改为 `Optional<String>` + `ifPresent/orElse("")` |
| smallrye-jwt 4.6.x 键名变更（`key-location` → `sign.key.location` 点分隔） | 文档化键名约定 |
| quarkus-bom 3.37 + hibernate 7.4 ABI 兼容 | 主仓 ddd4j-data-jpa 仅用 jakarta.persistence 稳定 API，运行时验证通过 |

## 8. 验收标准

- [x] 9 个扩展模块（含 plan 外 dubbo/validation/qrcode）全部 Producer 化
- [x] 每个扩展至少 1 个 `@QuarkusTest`
- [x] cola/dubbo 显式声明 jakarta.ws.rs-api 后编译通过
- [x] jackson customizer 适配 ConfigMapping 时序
- [x] ddd4j-quarkus-extension-dubbo 新增完成

## 9. 相关文档

- 总览 spec: [`./2026-08-05-quarkus-alignment-overview-design.md`](./2026-08-05-quarkus-alignment-overview-design.md)
- 实施计划: [`../plans/2026-08-06-p1-extensions.md`](../plans/2026-08-06-p1-extensions.md)
