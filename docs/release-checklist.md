# 发布清单：除了代码还需上传什么

| 材料 | 本次准备 | 仍需补齐 |
|---|---|---|
| README、系统架构、逻辑ER图 | 已有 | 可再补个人职责、业务背景 |
| 数据库建表SQL和数据字典 | 84表、901字段 | 独立合成种子SQL、数据来源及授权说明 |
| 分层/表粒度/指标字典 | 四主题说明 | 更细字段映射、变更与退款边界 |
| 依赖与环境 | POM、requirements、pnpm锁文件 | Python锁文件、服务端实际版本清单 |
| Canal/Redis同步 | 源码契约已说明 | 正式配置脱敏导出、表过滤和映射 |
| Kafka | 主要Topic已列 | 分区/副本/retention、初始化命令 |
| Druid | 四主题字段在查询代码中 | 实际supervisor JSON、时区/rollup/保留策略 |
| Flink部署与恢复 | 已说明顺序、边界和风险 | 集群配置、lib清单、可复用提交/恢复脚本 |
| 演示截图 | 两张既有效果图 | 可选短视频或GIF |
| 测试与对账 | 本轮测试记录、合成消息预期值 | 同批源库→DWS→Druid→API脱敏对账SQL/结果 |
| 许可证和素材来源 | 保留远端Apache-2.0 | 确认教学数据、报告与图像来源可公开 |

现有examples是合成消息，不能当完整数据库种子；缺少基础设施配置时不能宣传一键复现。

不进入Git历史：原始shop.sql全量行、.env、密钥、IDE数据库连接、target/JAR、node_modules/dist、日志、浏览器目录、Checkpoint/Savepoint、Kafka日志、RedisRDB/AOF、Druid段和元数据库、聊天原文。JAR可验证后放Release附件；大数据集在确认授权后另附下载及校验值。运行态资产另作私有备份。

原始Word报告未直接公开，避免把项目方确认与本轮独立实测混为一谈；可提供脱敏Markdown测试报告。

## 发布步骤

首次版本从远端main准备，保留原历史与LICENSE。后续更新可参考以下检查与提交步骤。

```powershell
git status --short
git diff --check
git add .
git diff --cached --stat
git diff --cached
# 完成文件审查后
git commit -m "docs: publish ecommerce realtime warehouse project"
git push origin main
```

首次发布前再次检查暂存内容和截图。不能从原目录直接git add .。
