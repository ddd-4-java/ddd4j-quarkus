# ddd4j-quarkus 版本线策略

> 本文档定义 ddd4j-quarkus 仓库的版本线命名规则、与 ddd4j 主仓的对齐矩阵、以及 quarkus-bom 版本对齐规则。

---

## 版本线命名规则

ddd4j-quarkus 采用 **语义化版本线 + 日期戳** 的命名规则：

```
{major}.{minor}.x.{yyyymmdd}-SNAPSHOT
```

- `{major}.{minor}`：对齐的 Quarkus 主线版本（如 3.3.x 对齐 Quarkus 3.37.x，4.0.x 对齐 Quarkus 3.38.x）
- `{yyyymmdd}`：版本线冻结日期（如 20260730 表示 2026-07-30 冻结）
- `-SNAPSHOT`：快照版本标识

---

## 与 ddd4j 主仓的版本对齐矩阵

| ddd4j-quarkus 版本线 | revision | quarkus-bom | ddd4j 主仓分支 | ddd4j 主仓 revision | Java |
|---|---|---|---|---|---|
| feature/3.3.x（对应 ddd4j 2.0.x） | 3.3.x.20260630-SNAPSHOT | 3.37.4 | feature/2.0.x | 2.0.x.20260630-SNAPSHOT | 17 |
| feature/4.0.x（当前分支，对应 ddd4j 3.0.x） | 4.0.x.20260630-SNAPSHOT | 3.38.2 | feature/3.0.x | 3.0.x.20260730-SNAPSHOT | 21 |

**注意**：当前分支为 feature/4.0.x（4.0.x 版本线），revision 为 4.0.x.20260630-SNAPSHOT，依赖的 ddd4j 主仓底座为 feature/3.0.x 的 3.0.x.20260730-SNAPSHOT。

---

## quarkus-bom 版本对齐规则

| Quarkus 主线 | quarkus-bom | Hibernate ORM | Agroal | Jackson |
|---|---|---|---|---|
| 3.37.x | 3.37.4 | 7.2.x | 3.1.x | 2.20.x |
| 3.38.x | 3.38.2 | 7.4.1 | 3.2 | 2.22.0 |

**关键约束**：
- ddd4j 主仓 3.0.x 已迁移到 Jackson 3，ddd4j-quarkus 4.0.x 版本线需要显式钉住 jackson-core/databind 2.22.0 覆盖
- ddd4j 主仓 2.0.x 使用 Jackson 2，ddd4j-quarkus 3.3.x 版本线无需覆盖

---

## 分支管理

### 当前活跃分支

| 分支 | 状态 | 说明 |
|---|---|---|
| feature/1.0.x | 维护中 | 旧版本线（已冻结） |
| feature/2.0.x | 维护中 | 旧版本线（已冻结） |
| feature/3.0.x | 维护中 | 历史主线（4.0.x 版本线前身，已由 feature/4.0.x 接替） |
| feature/4.0.x | **活跃（当前分支）** | 4.0.x 版本线，Java 21，对齐 ddd4j 3.0.x |
| master | 稳定 | 发布分支 |

### 待创建分支

| 分支 | 目标 | 说明 |
|---|---|---|
| feature/3.3.x | 待创建 | 对齐 ddd4j 2.0.x + quarkus-bom 3.37.4（Java 17 维护线） |

> feature/4.0.x 已创建并作为当前活跃分支（对齐 ddd4j 3.0.x + quarkus-bom 3.38.2）。

---

## Maven 4 subprojects 规范

Maven 4 使用 `<subprojects>` 替代 `<modules>`：

```xml
<!-- Maven 4 正确写法 -->
<subprojects>
    <subproject>ddd4j-quarkus-data-event-store-panache</subproject>
</subprojects>

<!-- Maven 3 旧写法（不再使用） -->
<modules>
    <module>ddd4j-quarkus-data-event-store-panache</module>
</modules>
```

---

## GitHub Actions 配置

ddd-4-java 组织已配置 `MAVEN_SETTINGS_XML` secret，用于 GitHub Actions 访问阿里云私有 Maven 仓库。

CI 流程：
1. checkout ddd4j 主仓（feature/3.0.x）
2. 分层 install ddd4j 主仓模块到本地 Maven 缓存
3. 运行 ddd4j-quarkus 测试

---

## 参考

- [Quarkus 版本线](https://quarkus.io/blog/)
- [ddd4j 主仓](https://github.com/ddd-4-java/ddd4j)
- [Testcontainers 模块](https://testcontainers.com/modules/)
