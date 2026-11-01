# 部署与运行准备

源码目标Java8，Flink1.14.6/Scala2.11；初始化模块MySQL JDBC8.0.33、Kafka客户端3.6.1。历史记录提到MySQL5.7.44、Canal1.1.8、Kafka服务端3.9.0、Druid24.0.1 nano；服务端版本未在本轮重新连接确认。

## 前置条件

1. 独立演示库shop导入sql/shop-schema.sql，仅含CREATE TABLE，不含数据库创建或原始行。不要对已有业务库执行。
2. 补充合成维度与事实种子SQL。examples仅说明消息契约，不能替代数据库种子。
3. 导出并脱敏Canal server/instance配置、binlog过滤、ODS路由及Redis维度同步映射。匹配DWD的table/type/data契约。
4. 导出四主题Druid supervisor，包含字段类型、timestampSpec、rollup和retention。必须核对现有伪UTC约定，不能猜测后覆盖正式配置。
5. 导出Kafka Topic分区、副本与retention。主要Topic见data-model.md，原代码多处假设3分区。
6. 配置master/slave1/slave2解析，核对源码192.168.13.*地址；历史文档和源码的slave2地址并不一致。
7. 提供Flink集群配置、连接器lib清单、HDFS默认文件系统与Checkpoint路径。POM中provided的Flink依赖由运行集群提供。

## 凭据

Java初始化模块使用MYSQL_PASSWORD，Redis Java调用使用REDIS_PASSWORD；认证调用保留，必须给有效值，不提供免密适配。远端Flink TaskManager也需要这些环境变量，仅设置提交终端未必能传入远端环境。用户名与地址部分仍硬编码，需适配目标环境。

Flask在backend/.env设置DRUID_BASE_URL、REDIS_SHOP_HOST/REDIS_SHOP_PASSWORD等；本地文件不提交。模拟器使用MYSQL_PASSWORD/MYSQL_HOST/MYSQL_USER/MYSQL_DATABASE/MYSQL_CLIENT。MySQL模拟器普通模式与--once会真实写入；--preview/--dry-run仍只读查询数据库，不是完全离线生成。

## 建议运行顺序

基础组件和Topic就绪 → 验证CDC路由 → 初始化维度 → DWD开始监听 → 一次性历史事实导入 → 冻结一致边界 → DWS历史Bootstrap与状态预热 → 从已验证Checkpoint接实时增量 → Druid摄取 → Flask/Vue → 小批模拟数据及逐窗口对账。

DWD使用group offsets，无已提交offset时回退latest；先灌历史再首次启动可能跳过已有数据。不要调整正式消费组或重复执行Bootstrap。Goods WARMUP/REALTIME状态恢复过程需要单独验证；Trade/Shop/User必须使用对应历史边界。

Checkpoint配置只负责保存状态，普通flink run不会自动找旧状态。恢复明确指定已验证URI，保持算子UID/哈希；不要忽略未恢复状态掩盖问题。原DWS Kafka Sink没有显式事务语义，因此不能据Checkpoint EXACTLY_ONCE声称端到端Exactly-Once。

## 本轮可复现的离线测试命令

仓库根目录PowerShell：

```powershell
mvn -f realtime-dw/pom.xml package
mvn -f realtime-flink-kafka-dwd/pom.xml package
$tests = Get-ChildItem realtime-flink-kafka-dws/src/test -Recurse -Filter '*Test.java' | Where-Object { $_.Name -notin @('KafkaPartitionPositionTest.java','DwsKafkaSinkIntegrationTest.java') } | ForEach-Object { $_.BaseName }
mvn -f realtime-flink-kafka-dws/pom.xml package ("-Dtest=" + ($tests -join ','))
```

KafkaPartitionPositionTest带@Test，会访问原Kafka；DwsKafkaSinkIntegrationTest是手工main入口，会发消息。离线验证不执行这两项。旧Surefire2.12.4使用明确名单，避免依赖通配排除语法。

后端建议Python3.11，全新环境安装requirements。原README的Python3.8与Flask>=3要求不一致。前端按pnpm-lock.yaml frozen-lockfile安装，保留锁定版本，避免重新解析latest。
