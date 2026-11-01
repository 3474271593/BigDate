# 测试与构建记录

验证日期：2026-11-01。

## 自动化测试

| 模块 | 测试结果 | 覆盖方向 |
|---|---|---|
| DWD | 39 项通过 | 订单及明细清洗、序列化、脏数据、维度补全与 Redis 重连、Checkpoint 配置 |
| DWS | 144 项通过 | 主题增量计算、历史基线、状态预热、商品关联、窗口聚合、迟到数据和序列化 |
| Flask API | 31 项通过 | 接口参数、时间处理、指标聚合、数据质量与主题分析 |
| 前端 | 14 项通过 | 图表数据转换、历史图表、请求竞态、3D 店铺图及用户排行逻辑 |

DWS 逻辑测试使用明确的测试类名单；Kafka 分区诊断和手工消息发送工具按集成测试场景执行。命令见 [部署与运行](deployment.md)。

## 构建结果

| 对象 | 结果 |
|---|---|
| realtime-dw | Maven 编译打包成功 |
| realtime-flink-kafka-dwd | Maven 编译打包成功 |
| realtime-flink-kafka-dws | Maven 编译打包成功 |
| 前端依赖 | pnpm install --frozen-lockfile 成功 |
| 前端生产构建 | pnpm run build 成功 |

后端测试命令：

```powershell
cd DataScreen/backend
python -m pytest -p no:cacheprovider tests -q
```

前端测试与构建：

```powershell
cd DataScreen/frontend
pnpm test
pnpm run build
```
