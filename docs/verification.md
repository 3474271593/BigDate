# 本轮验证记录

2026-11-01，Asia/Shanghai。对象为发布准备副本。

| 检查 | 结果 |
|---|---|
| realtime-dw Maven test/package | 1项通过，BUILD SUCCESS（测试本身是模板断言，主要证明编译打包） |
| DWD Maven离线test/package | 39项通过，BUILD SUCCESS |
| DWS Maven离线test/package | 144项通过，BUILD SUCCESS |
| Flask pytest | 31项通过，1项预装环境依赖弃用提示 |
| 前端node --test | 14项通过 |
| pnpm install --frozen-lockfile | 成功 |
| pnpm run build | 成功，978模块；主JS约1.89MB，gzip约600KB，有包体积提示 |

DWS排除连接实际Kafka的KafkaPartitionPositionTest，未执行手工main发消息工具DwsKafkaSinkIntegrationTest。Maven shade存在LICENSE/NOTICE/MANIFEST资源重叠提示，构建通过不等于完成第三方许可证分发审查。

Python来自已有Anaconda3.8.5环境并预装依赖，本次结果不证明requirements能在全新Python3.8安装；Flask>=3需要较新Python，新环境建议Python3.11。

本轮未连接或修改MySQL/Canal/Kafka/Flink/Redis/Druid，未做在线E2E、压力测试、故障注入或浏览器复验。截图为用户提供的既有成果。

原Word报告记录2026-09-23消息追踪和2026-10-09Druid查询，并区分原始实测与独立审计/项目方确认。报告里的“生产级容量/恢复力完成”未附本轮可复核压力或故障数据，不能直接当作生产性能承诺。历史179个分钟窗口对账也不是本轮结果。
