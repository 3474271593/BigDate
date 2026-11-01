# 电商实时数仓运营数据大屏

只读数据链路：Apache Druid → Flask REST API → Vue 3 / ECharts。店铺名称由 Flask 只读查询 Redis `dim:shops:{shopId}` 的 `shopName` 字段；未匹配或 Redis 不可用时显示店铺 ID。项目不会修改 MySQL、Canal、Kafka、Flink、Redis 或 Druid。

## 后端启动

```powershell
cd DataScreen/backend
conda create -n datascreen python=3.11 -y
conda activate datascreen
pip install -r requirements.txt
Copy-Item .env.example .env
python run.py
```

测试：`python -m pytest -p no:cacheprovider tests -q`。默认监听 `http://127.0.0.1:5000`。

## 前端启动

```powershell
cd DataScreen/frontend
pnpm install --frozen-lockfile
pnpm run dev
```

生产构建：`npm run build`。Vite 会把 `/api` 代理到 `http://127.0.0.1:5000`。

## V2 双模式驾驶舱

- 默认进入“历史基线分析”：展示真实活跃日、商品支付件数构成、店铺贡献和用户价值分布。历史数据实际覆盖 `2019-06-22 11:11` 至 `2019-09-05 20:28`，共 3 个活跃日。
- “实时链路监控”仅查询 2026 测试数据，每 60 秒刷新，突出 GMV 面积趋势和下单/支付混合趋势。
- 切换数据集会取消旧请求并清空旧视图；迟到响应不能覆盖新数据，2019 与 2026 不会拼接成连续时间线。
- ECharts 使用按需引入；当前实际使用面积折线、混合柱线、Donut、Treemap、Scatter、PictorialBar 与排行条形图。

新增只读接口：

- `GET /api/dashboard/dataset-meta`：真实数据范围和活跃日数。
- `GET /api/trade/active-days`：真实活跃日期及交易指标。
- `GET /api/goods/paid-volume-share`：商品 TOP5 支付件数与真实“其他”。
- `GET /api/shop/contribution`：店铺支付金额及支付订单贡献。
- `GET /api/user/value-distribution`：用户支付订单与支付金额分布。

## 数据口径

- 交易：支付 GMV 使用 `dws_trade_summary.paidAmount`；订单数使用 `orderCount/paidOrderCount`。
- 商品：所有金额与数量均为商品明细口径，不解释为完整平台 GMV 拆解。
- 店铺/用户：窗口用户数、窗口店铺数跨窗口求和不等于全周期独立计数。
- 时间：API 接收 Asia/Shanghai，查询 Druid 时保留业务钟面数字并按当前伪 UTC 规则附加 `Z`。
- 重复：自然键与指标完全相同只保留一条；同键指标冲突整组不参与聚合并通过 warning 报告。
