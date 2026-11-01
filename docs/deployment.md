# 部署与运行

## 环境与依赖

- Java 编译目标：Java 8。
- Flink：1.14.6，Scala 2.11。
- Python：建议 3.11，依赖见 DataScreen/backend/requirements.txt。
- 前端：Node.js 与 pnpm，按 pnpm-lock.yaml 安装依赖。
- 数据服务：MySQL、Canal、Kafka、Redis、Druid；Checkpoint 使用配置的文件系统。

## 配置

| 配置项 | 用途 |
|---|---|
| MYSQL_PASSWORD | Java 初始化程序与订单模拟器的 MySQL 认证 |
| REDIS_PASSWORD | Java 维度初始化与维度查询认证 |
| DRUID_BASE_URL | Flask 查询 Druid 的地址 |
| REDIS_SHOP_HOST / REDIS_SHOP_PASSWORD | Flask 店铺名称查询 |
| flink.checkpoint.path | Flink Checkpoint 路径覆盖 |
| goods.runtime.mode | Goods 状态预热或实时阶段：WARMUP / REALTIME |
| flink.restore.path | Goods REALTIME 的状态恢复路径 |

根据运行环境设置 MySQL、Kafka、Redis 地址及 master/slave1/slave2 主机解析。远端执行时，将相关认证配置提供给对应执行节点与 TaskManager。

Flask 配置示例位于 DataScreen/backend/.env.example。复制为 .env 后填写本地值；认证信息通过环境配置传入。

## 构建 Java 模块

在仓库根目录执行：

```powershell
mvn -f realtime-dw/pom.xml package
mvn -f realtime-flink-kafka-dwd/pom.xml package
$tests = Get-ChildItem realtime-flink-kafka-dws/src/test -Recurse -Filter '*Test.java' | Where-Object { $_.Name -notin @('KafkaPartitionPositionTest.java','DwsKafkaSinkIntegrationTest.java') } | ForEach-Object { $_.BaseName }
mvn -f realtime-flink-kafka-dws/pom.xml package ("-Dtest=" + ($tests -join ','))
```

以上 DWS 命令运行本地逻辑测试。KafkaPartitionPositionTest 用于连接目标 Kafka 的分区诊断；DwsKafkaSinkIntegrationTest 提供手工消息发送入口，按集成验证场景单独执行。

## 数据链路运行顺序

1. 配置业务库、Canal 采集、Kafka Topic、Redis 和 Druid 服务。
2. 在独立 shop 数据库导入 sql/shop-schema.sql，使用目标数据集初始化业务数据。
3. 初始化 Redis 维度，并在 DWD 开始消费后执行一次历史事实导入。
4. 记录一致的 Kafka 分区边界，配置主题 Bootstrap 和状态预热。
5. 完成历史主题计算，使用对应 Checkpoint 衔接实时增量。
6. 启动 Druid 四主题摄取、Flask 服务与前端看板。

DWD 使用消费组位点启动，首次无位点时使用 latest。安排历史导入时，以 DWD 已开始监听为前提。各主题历史边界与数据快照配套配置，保持 Bootstrap 和实时阶段一致。

Flink 恢复时使用目标状态 URI，保持算子 UID 与状态映射一致。Goods REALTIME 通过 flink.restore.path 指向 WARMUP 生成的 Checkpoint。

## 启动看板

后端：

```powershell
cd DataScreen/backend
python -m pip install -r requirements.txt
Copy-Item .env.example .env
python run.py
```

前端另开终端：

```powershell
cd DataScreen/frontend
pnpm install --frozen-lockfile
pnpm run dev
```

后端默认监听 5000 端口，Vite 将 /api 代理到本地后端。GET /api/health 用于检查 Druid 查询连通性。

## 模拟订单

DataScreen/tools/realtime_data_simulator.py 根据业务维度生成订单及同店商品明细。配置 MYSQL_HOST、MYSQL_USER、MYSQL_PASSWORD、MYSQL_DATABASE 和 mysql 客户端路径后，可使用 --preview N 或 --dry-run 检查生成结果。

普通模式持续写入订单，--once 写入一笔完整订单。请在目标演示库运行，使用 Ctrl+C 停止。
