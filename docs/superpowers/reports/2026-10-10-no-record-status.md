# 禁止 record 分支验收记录

- 分支：`master`
- 原始提交：`c4007405dfbce71ea3f692f5bc67117b30947db0`
- 变更范围：0 个包含 record 的源码/测试/示例文件，普通不可变类替换；AGENTS 规则与扫描器覆盖全分支。
- 本分支门禁：全部 Git 跟踪 Java 文件 record 声明为 0；扫描器 5 项自测通过；`git diff --check` 通过；转换目标 JavaParser AST 解析通过。
- 原始源码门禁：原提交本就没有 record，仅增加规范与防回归门禁。
- 独立实际源码编译及值语义测试：0 个类型，NO_STANDALONE_TYPES；验证构造校验、组件访问、equals/hashCode/toString、Jackson JSON roundtrip。内容指纹：`无独立值对象`。
- 当前工作分支 Maven 目标验证（仅适用于具体验证分支）：feature/4.0.x: MQ core、QLExpress、client 及 app reactor 34 tests PASS。
- 历史 formatter 倒置实现修复：0 个文件；仅采用与当前源码有效 token 多重集完全相同的历史有效语法位置，不丢弃已有实现。

## 验证边界

全分支未执行完整 reactor/发布/CI。跨分支复用仅限内容指纹一致的实际源文件编译和独立值语义测试，不代表该分支所有依赖和集成环境均通过。

未修改 Maven 父 POM、版本及发布 shell；不存在通过 skip Docker 参数宣称发布链路通过的情况。
