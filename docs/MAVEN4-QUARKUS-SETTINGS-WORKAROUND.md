# Maven 4 / Quarkus settings 兼容说明

`feature/4.0.x` 使用 Maven Model 4.1.0 和 Maven 4 wrapper，同时保留
`<modules>` 聚合器。Quarkus 3.38.2 的 bootstrap resolver 仍通过 Maven 3
settings reader 读取 Maven 4 安装目录下的全局 `conf/settings.xml`，无法识别
Maven 4 新增的顶层 `<repositories>`，因而会为每次 Quarkus 测试启动输出
`Settings problem encountered`。

wrapper 和 CI 中会通过标准 `-gs` 参数显式传入仓库内最小
Maven 3 兼容 global settings 的绝对路径，并用
`-Dddd4j.maven.home` 将它所在的最小 Maven home 传入 Surefire/Failsafe
测试进程。绝对路径是必要的：Quarkus
测试可能从嵌套模块目录启动，相对路径会回退到 Maven 4 内置 settings。
该文件不包含仓库、镜像、服务器或凭据，也不会替代用户级
`~/.m2/settings.xml`。GitHub Actions 仍由
`MAVEN_SETTINGS_XML` 写入用户 settings，并从其中取得阿里云私有仓库配置。

这是 Quarkus 3.38.2 Maven 4 前瞻适配的临时兼容层，不代表上游已完整支持
Maven 4 settings 模型。上游 resolver 能直接解析 Maven 4 global settings 后，
应删除 CI/wrapper 命令中的 `-gs` 参数和本兼容文件，并重新执行空缓存消费、
完整测试和 CI 门禁。
