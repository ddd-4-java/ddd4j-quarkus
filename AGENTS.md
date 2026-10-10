# 框架工程协作规范

## Java 类型统一规范（所有分支）

- 本框架所有版本分支严禁声明 Java `record` 类型，范围包含生产源码、测试、示例、局部类型与嵌套类型。
- 值对象和 DTO 使用普通类；需要不可变语义时使用 `final` 类与 `private final` 字段，保持各 JDK 分支的类型布局一致。
- 从 record 迁移时保留构造器校验、组件访问方法、值相等性、哈希值、字符串表示、泛型、接口及序列化契约；嵌套类型显式保留 `static` 语义。
- JSON 类型显式声明属性及构造器映射；字段校验和接口文档注解按适用位置保留。禁止依赖 Record 反射或 record pattern。
- 提交前扫描当前分支所有受 Git 管理的 Java 文件并验证不存在 record 声明，执行受影响类型及 JSON/校验链路测试；报告构建和测试的实际限制。

执行门禁：`python3 scripts/check_no_records.py`；扫描器自测：`python3 -m unittest discover -s scripts -p test_check_no_records.py`。
