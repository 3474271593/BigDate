<script setup>
import {computed,onMounted} from 'vue'
import DashboardHeader from '../components/dashboard/DashboardHeader.vue'
import BackgroundLines3D from '../components/dashboard/BackgroundLines3D.vue'
import KpiCard from '../components/dashboard/KpiCard.vue'
import PanelFrame from '../components/dashboard/PanelFrame.vue'
import CoreGmvTrendChart from '../components/charts/CoreGmvTrendChart.vue'
import DonutChart from '../components/charts/DonutChart.vue'
import VerticalShopBar from '../components/charts/VerticalShopBar.vue'
import ValueScatterChart from '../components/charts/ValueScatterChart.vue'
import GoodsQuadrantChart from '../components/charts/GoodsQuadrantChart.vue'
import HistoricalShopHeatmap from '../components/charts/HistoricalShopHeatmap.vue'
import HistoricalVolumePriceBubble from '../components/charts/HistoricalVolumePriceBubble.vue'
import MixedTrendChart from '../components/charts/MixedTrendChart.vue'
import PictorialRankChart from '../components/charts/PictorialRankChart.vue'
import ShopPaymentTicker from '../components/dashboard/ShopPaymentTicker.vue'
import GoodsPaymentTicker from '../components/dashboard/GoodsPaymentTicker.vue'
import ShopTimeAmount3D from '../components/charts/ShopTimeAmount3D.vue'
import UserAmountDepthRank from '../components/charts/UserAmountDepthRank.vue'
import {useDashboard} from '../composables/useDashboard'

const {dataset,data,warnings,loading,error,moduleErrors,dataAsOf,refresh,switchDataset,start}=useDashboard()
const historical=computed(()=>dataset.value==='historical')
const money=v=>v==null?'—':`¥${Number(v).toLocaleString('zh-CN',{maximumFractionDigits:2})}`
const number=v=>v==null?'—':Number(v).toLocaleString('zh-CN')
const rate=v=>v==null?'—':`${v}%`
const userAmountById=computed(()=>(data.value.userAmount||[]).map(row=>({...row,name:'userId '+(row.id??String(row.name).replace(/\D/g,''))})))
const spark=computed(()=>historical.value?[]:(data.value.gmv||[]).slice(-12).map(x=>x.paidAmount))
const warningLabels={DUPLICATE_ROWS_DETECTED:'检测到并逻辑去除了完全重复窗口',DATA_CONFLICT:'检测到同自然键指标冲突，相关记录未参与聚合',EMPTY_RESULT:'当前范围暂无数据',MISSING_GOODS_NAME:'部分商品名称缺失，已显示商品 ID',SHOP_NAME_MISSING:'部分店铺未匹配到 Redis 名称，已显示店铺 ID',SHOP_DIMENSION_UNAVAILABLE:'Redis 店铺维度暂不可用，已显示店铺 ID',GOODS_DETAIL_COVERAGE_WARNING:'商品主题为商品明细口径，不代表完整平台 GMV 拆解',DATA_SKEW_WARNING:'历史金额高度集中于单个活跃日，请按事件分布解读'}
const notes=computed(()=>warnings.value.map(w=>warningLabels[w.code]||w.message||w.code))
onMounted(start)
</script>

<template>
<main class="dashboard-shell" :class="`mode-${dataset}`">
  <BackgroundLines3D />
  <DashboardHeader :dataset="dataset" :meta="data.meta" :data-as-of="dataAsOf" :notes="notes" :loading="loading" @change="switchDataset" @refresh="refresh" />
  <div v-if="error" class="error-banner">{{error}}</div>

  <section class="kpi-grid">
    <KpiCard title="支付 GMV" :value="money(data.overview?.paidAmount)" accent="var(--goods-primary)" :spark="spark" :hint="historical?'历史基线总额':''" />
    <KpiCard title="支付订单数" :value="number(data.overview?.paidOrderCount)" unit="单" accent="var(--trade-primary)" :hint="historical?'活跃日累计':''" />
    <KpiCard title="下单订单数" :value="number(data.overview?.orderCount)" unit="单" accent="var(--shop-primary)" :hint="historical?'同范围累计':''" />
    <KpiCard title="窗口支付 / 下单比" :value="rate(data.overview?.paymentRate)" accent="var(--warning)" :hint="''" />
  </section>

  <template v-if="historical">
    <section class="visual-grid historical-primary">
      <PanelFrame title="历史交易播报" subtitle="店铺支付汇总" badge="SHOP" accent="cyan" :level="1" :loading="loading" :empty="!data.shopContribution?.length"><ShopPaymentTicker :data="data.shopContribution" /></PanelFrame>
      <PanelFrame title="商品支付件数构成" subtitle="全部商品 · 支付件数" badge="GOODS" accent="pink" :loading="loading" :empty="!data.goodsShare?.length"><DonutChart :data="data.goodsShare" /></PanelFrame>
      <PanelFrame title="店铺支付金额 TOP10" subtitle="" badge="SHOP" accent="blue" :loading="loading" :empty="!data.shopContribution?.length"><VerticalShopBar :data="data.shopContribution" /></PanelFrame>
    </section>
    <section class="visual-grid historical-detail-grid">
      <PanelFrame class="historical-goods-rank" title="商品支付件数排行" subtitle="全部商品 · 按支付件数排序" badge="RANK" accent="pink" :level="3" :loading="loading" :empty="!data.goodsShare?.length"><GoodsPaymentTicker :data="data.goodsShare" /></PanelFrame>
      <PanelFrame class="historical-heatmap-panel" title="店铺交易时序热力矩阵" subtitle="店铺 × 实际分钟窗口" badge="SHOP" accent="cyan" :level="1" :loading="loading" :empty="!data.shopHeatmap?.data?.length" :error="moduleErrors.shopHeatmap"><HistoricalShopHeatmap :data="data.shopHeatmap" /></PanelFrame>
      <PanelFrame class="historical-bubble-panel" title="历史交易量价关系气泡图" subtitle="订单量 × 均单金额 · 气泡表示窗口 GMV" badge="TRADE" accent="blue" :level="1" :loading="loading" :empty="!data.tradeVolumePrice?.points?.length" :error="moduleErrors.tradeVolumePrice"><HistoricalVolumePriceBubble :data="data.tradeVolumePrice" /></PanelFrame>
      <PanelFrame class="historical-user-rank" title="历史订单 userId 支付金额 TOP10" subtitle="同一 ID 汇总 · 非自然人排行" badge="USER" accent="purple" :level="3" :loading="loading" :empty="!data.userAmount?.length"><UserAmountDepthRank :data="userAmountById" /></PanelFrame>
    </section>
  </template>

  <template v-else>
    <section class="visual-grid realtime-layout">
      <PanelFrame title="实时商品支付排行" subtitle="商品明细支付件数" badge="GOODS" accent="pink" :loading="loading" :empty="!data.goodsPaid?.length"><PictorialRankChart :data="data.goodsPaid" /></PanelFrame>
      <PanelFrame class="focus-panel core-trend-panel" title="支付 GMV 实时趋势" subtitle="支付金额窗口" badge="LIVE" accent="cyan" :level="1" :loading="loading" :empty="!data.gmv?.length"><CoreGmvTrendChart :data="data.gmv" /></PanelFrame>
      <PanelFrame class="goods-quadrant-panel" title="商品销量与成交价值四象限" subtitle="支付件数 × 平均每件成交金额" badge="GOODS" accent="cyan" :level="1" :loading="loading" :empty="!data.goodsQuadrant?.goods?.length" :error="moduleErrors.goodsQuadrant"><GoodsQuadrantChart :data="data.goodsQuadrant" /></PanelFrame>
      <PanelFrame title="商品支付件数构成" subtitle="全部商品 · 支付件数" badge="SHARE" accent="pink" :loading="loading" :empty="!data.goodsShare?.length"><DonutChart :data="data.goodsShare" /></PanelFrame>
      <PanelFrame class="shop-time-panel" title="店铺分时支付金额" subtitle="TOP10 店铺 · 最近 12 个有效分钟窗口" badge="SHOP 3D" accent="cyan" :level="1" :loading="loading" :empty="!data.shopTimeAmount?.data?.length"><ShopTimeAmount3D :data="data.shopTimeAmount" /></PanelFrame>
      <PanelFrame class="realtime-trend-bottom" title="下单与支付实时趋势" subtitle="" badge="TRADE" accent="cyan" :loading="loading" :empty="!data.orders?.length"><MixedTrendChart :data="data.orders" /></PanelFrame>
      <PanelFrame class="user-distribution-bottom" title="订单 userId 支付分布" subtitle="每点一个订单 userId · 非自然人数" badge="USER" accent="purple" :loading="loading" :empty="!data.userDistribution?.length"><ValueScatterChart :data="data.userDistribution" /></PanelFrame>
    </section>
  </template>

</main>
</template>
