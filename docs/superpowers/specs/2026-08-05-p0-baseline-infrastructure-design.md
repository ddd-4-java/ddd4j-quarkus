# P0 — 基线基础设施设计（Cache / Web / MQ 三件套 + parent 链重构）

- 日期: 2026-08-05
- 作者: ddd-4-java
- 状态: 设计已确认 / 实施已对齐
- 范围: ddd4j-quarkus-{cache,web,mq-core} + ddd4j-quarkus-parent + ddd4j-quarkus-dependencies + ddd4j-quarkus-bom
- 涉及模块: cache、web、mq-core、parent、dependencies、bom

## 1. 背景与问题

### 1.1 三件套未采用 Quarkus 原生扩展范式

基线代码使用 Spring 风格（`@Configuration` + `@Bean`）实现 cache、web、mq listener，无法被 Quarkus 在编译期增强（@Record / BuildStep）正确识别。需要在 quarkus 启动前由 Arc 处理，且运行时配置无法做到 build-time 优化。

### 1.2 parent 默认依赖污染业务项目

`ddd4j-quarkus-parent` 历史上默认提供 openapi/health/jwt/fault-tolerance/panache/narayana/mysql/quartz/easyexcel 共 8 个业务选型依赖，下游业务项目继承时会**被动**获得这些组件，造成：
- 不必要的 classpath 污染
- 隐式选型（业务不想要也得接受）
- 框架模块继承 parent 时全家桶随之而来（违反库模块自包含原则）

### 1.3 版本基线漂移

`root pom.xml`（`quarkus-bom 3.36.3`）与 `ddd4j-quarkus-dependencies`（`quarkus-bom 3.37.0`）版本不一致，导致 GACT ClassCastException、hibernate 7.4.1/7.3.7 解析失败。

## 2. 目标

1. **三件套 Quarkus 原生化**：cache 用 `@BuildStep + @Recorder`，web 用 `@ContainerRequestFilter + @Provider + HealthCheck`，mq-core 用反射扫描 `@MQEventListener`
2. **parent 按域 profiles**：8 个业务选型依赖改为 5 个 profile（web/data/schedule/excel/full），业务项目按需激活
3. **版本基线统一**：quarkus-bom 3.37.0 + hibernate 7.4.1.Final + agroal 3.2
4. **parent 链重构**：bom（扁平 BOM）→ dependencies（版本管理）→ parent（业务父 POM），三级解耦

## 3. 总体架构

### 3.1 Cache — BuildStep + Recorder

```
静态配置 @ConfigMapping
       ↓
Ddd4jCacheConfig (运行时读 Quarkus config)
       ↓
Ddd4jCacheBuildItemProducer#produceCacheConfig (@BuildStep)
       ↓
CacheRecorder#setDefaultType (@Recorder)
       ↓
RuntimeValue<Void> 触发 CacheKit.setDefaultType(...)
```

**核心要点**：Recorder 在 STATIC_INIT 阶段执行，build-time 配置不进入运行时配置源，配置解析 0 成本。

### 3.2 Web — Filter + HealthCheck + ExceptionMapper

```
RESTEasy Reactive 请求生命周期
       ↓
Ddd4jQuarkusWebFilter (ContainerRequestFilter，租户上下文 + 访问日志)
       ↓
@Path Resource
       ↓
抛 BizException / SysException
       ↓
DefaultExceptionHandler (@Provider，ExceptionMapper → ApiRestResponse)
       ↓
健康检查：Ddd4jQuarkusWebHealthCheck (HealthCheck → /q/health/ready)
```

**核心要点**：基类不带 `@Provider` 注解（避免 RESTEasy 双注册），具体 ExceptionMapper 在子类声明 `@Provider`；`@Readiness` 是 CDI qualifier，注入时必须 `@Inject @Readiness` 具体类。

### 3.3 MQ Listener Registrar — 反射扫描

```
应用启动
       ↓
QuarkusMQListenerRegistrar.scanListeners (@Observes StartupEvent)
       ↓
1. 扫描所有 @ApplicationScoped Bean
2. 反射收集带 @MQEventListener 的方法（仅方法签名匹配）
3. 仅实例化有监听器方法的 Bean
4. 注册到 MQClient.init
```

**核心要点**：先收集注解方法避免触发所有 bean 实例化（Quarkus synthetic injection point 崩溃），按 broker 类型分发到对应 MQEventListener。

### 3.4 Parent 链重构

**原结构**（扁平）：
```
业务项目 → ddd4j-quarkus-parent → ddd4j-parent
```

**新结构**（三级 + 解耦）：
```
业务项目 → ddd4j-quarkus-parent (按域 profiles + quarkus-arc + jakarta + slf4j + lombok + test deps)
                ↓ dependencyManagement
                ddd4j-quarkus-dependencies (version management: ddd4j-dependencies + quarkus-bom + overrides)
                ↓
                ddd4j-quarkus-bom (扁平 BOM：ddd4j + ddd4j-quarkus 自身模块坐标)
                ↓
                ddd4j-parent (顶级 parent)

框架聚合（mq/extensions/auth/data/cache/ddd/web）→ ddd4j-quarkus-dependencies（不继承 parent）
```

**版本管理链**：

```
parent dependencyManagement import ddd4j-quarkus-dependencies
       ↓
dependencies import ddd4j-dependencies (main repo BOM)
       ↓
ddd4j-dependencies 包含 quarkus-bom 3.37.x + ddd4j 全套依赖
       ↓
dependencies 显式管理：hibernate 7.4.1.Final（覆盖 quarkus-bom 默认）、agroal 3.2、ASM 9.x、truelicense、zxing
```

## 4. 核心抽象

### 4.1 ddd4j-quarkus-cache

| 类 | 角色 | 注解 |
|---|---|---|
| `Ddd4jCacheConfig` | 配置接口 | `@ConfigMapping(prefix = "ddd4j.cache")` |
| `CacheRecorder` | 静态初始化执行 | `@Recorder` |
| `Ddd4jCacheBuildItemProducer` | BuildStep 编排 | `@BuildStep` + `@Record(STATIC_INIT)` |
| `quarkus-extension.yaml` | 扩展元数据 | `META-INF/quarkus-extension.yaml` |

### 4.2 ddd4j-quarkus-web

| 类 | 角色 | 注解 |
|---|---|---|
| `Ddd4jQuarkusWebFilter` | 请求过滤器基类 | `ContainerRequestFilter`（不带 `@Provider`） |
| `Ddd4jQuarkusWebHealthCheck` | 健康检查 | `HealthCheck` + `@Readiness`（CDI qualifier） |
| `DefaultExceptionHandler` | 全局异常映射 | `@Provider` + `ExceptionMapper<Throwable>` |
| `Ddd4jQuarkusWebConfiguration` | Web 配置 | `@ConfigMapping(prefix = "ddd4j.web")` |
| `WebUtils` | 工具类 | 无 |

### 4.3 ddd4j-quarkus-mq-core

| 类 | 角色 | 注解 |
|---|---|---|
| `QuarkusMQListenerRegistrar` | 监听器扫描注册 | `@ApplicationScoped` + `@Observes StartupEvent` |
| `Ddd4jMQCdiProducer` | MQ 核心 Bean 生产 | `@Produces` |
| `MQEventSerialization` | 序列化策略 | `@Produces @Singleton` |

## 5. 关键文件

```
ddd4j-quarkus/
├── pom.xml                                                              # 版本对齐 3.37.0
├── ddd4j-quarkus-parent/
│   └── pom.xml                                                          # 按域 profiles（web/data/schedule/excel/full）
├── ddd4j-quarkus-dependencies/pom.xml                                   # 版本管理链
├── ddd4j-quarkus-bom/pom.xml                                            # 扁平 BOM
├── ddd4j-quarkus-cache/
│   ├── pom.xml
│   ├── src/main/java/io/ddd4j/quarkus/cache/
│   │   ├── Ddd4jCacheConfig.java
│   │   ├── CacheRecorder.java
│   │   ├── Ddd4jCacheBuildItemProducer.java
│   │   └── Ddd4jCacheCdiProducer.java
│   └── src/test/java/io/ddd4j/quarkus/cache/
│       ├── Ddd4jCacheConfigTest.java                                    # 纯 JUnit
│       └── Ddd4jCacheQuarkusTest.java                                   # @QuarkusTest
├── ddd4j-quarkus-web/
│   ├── pom.xml
│   ├── src/main/java/io/ddd4j/quarkus/web/
│   │   ├── Ddd4jQuarkusWebFilter.java
│   │   ├── Ddd4jQuarkusWebHealthCheck.java
│   │   ├── DefaultExceptionHandler.java
│   │   ├── Ddd4jQuarkusWebConfiguration.java
│   │   └── WebUtils.java
│   └── src/test/java/io/ddd4j/quarkus/web/
│       ├── Ddd4jQuarkusWebFilterTest.java
│       ├── Ddd4jQuarkusWebConfigurationTest.java
│       └── Ddd4jQuarkusWebHealthCheckTest.java
└── ddd4j-quarkus-mq/ddd4j-quarkus-mq-core/
    ├── pom.xml
    └── src/main/java/io/ddd4j/quarkus/mq/core/
        ├── QuarkusMQListenerRegistrar.java                              # 反射扫描
        └── Ddd4jMQCdiProducer.java
```

## 6. 测试策略

| 测试类 | 类型 | 验证 |
|---|---|---|
| `Ddd4jCacheConfigTest` | 纯 JUnit | ConfigMapping 接口契约 |
| `Ddd4jCacheQuarkusTest` | `@QuarkusTest` | Caffeine in-memory 缓存集成 |
| `Ddd4jQuarkusWebFilterTest` | `@QuarkusTest` | 租户上下文 + 访问日志链路 |
| `Ddd4jQuarkusWebConfigurationTest` | 纯 JUnit | ConfigMapping 解析 |
| `Ddd4jQuarkusWebHealthCheckTest` | `@QuarkusTest` | `/q/health/ready` UP |
| `QuarkusMQListenerRegistrarTest` | `@QuarkusTest` + mock MQClient | 监听器扫描注册逻辑 |

**测试坐标**：使用 `io.quarkus:quarkus-junit`（Quarkus 3.31+ 官方坐标，`quarkus-junit5` 已被官方 relocation 到 `quarkus-junit`，版本由 ddd4j-quarkus-parent 版本管理链提供）。

**JUnit 4 依赖**：`quarkus-devservices-*` 处理器加载时引用 `org.junit.rules.TestRule`（junit4），需要显式声明 `junit:junit` test 依赖。

## 7. 风险与缓解

| 风险 | 缓解 |
|---|---|
| Quarkus BuildStep 编译慢 | 拆分 core / 实现两层，broker 模块仅 @BuildStep 缓存 |
| Hibernate 7.4 与 quarkus-bom 3.37 默认版本冲突 | 在 `ddd4j-quarkus-dependencies` 显式覆盖 7.4.1.Final |
| 阿里云 snapshot 仓库 metadata 指向不存在 timestamped 快照 | `mvn install -DskipTests` 主仓依赖 + 离线构建 |
| parent 默认依赖污染 | 按域 profiles + 框架模块继承 dependencies 而非 parent |
| Web Filter 基类注解导致 RESTEasy 双注册 | 基类不带注解，具体子类按需声明 |
| mq-core synthetic bean 崩溃 | scanListeners 先收集注解方法，仅实例化有 listener 的 bean |

## 8. 验收标准

- [x] `mvn -Pintegration verify` 全量通过（318 测试 0 失败）
- [x] 5 个新测试类（Ddd4jCacheConfig/Ddd4jCacheQuarkus/3 个 web/QuarkusMQListenerRegistrar）全部通过
- [x] parent 移除 8 个默认业务选型依赖
- [x] quarkus-bom 统一为 3.37.x
- [x] hibernate 显式管理 7.4.1.Final
- [x] parent 链三级解耦（bom/dependencies/parent）

## 9. 相关文档

- 总览 spec: [`./2026-08-05-quarkus-alignment-overview-design.md`](./2026-08-05-quarkus-alignment-overview-design.md)
- 实施计划: [`../plans/2026-08-05-p0-baseline.md`](../plans/2026-08-05-p0-baseline.md)
