<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { echarts } from '../../config/echarts'

const props = defineProps({ data: { type: Array, default: () => [] } })
const el = ref()
const page = ref(0)
const pageSize = 5
let chart
let timer
const pageCount = computed(() => Math.max(1, Math.ceil(props.data.length / pageSize)))
const visibleRows = computed(() => props.data.slice(page.value * pageSize, (page.value + 1) * pageSize))
const pageLabel = computed(() => props.data.length <= pageSize ? '' :
  String(page.value * pageSize + 1).padStart(2, '0') + '–' +
  String(Math.min((page.value + 1) * pageSize, props.data.length)).padStart(2, '0') +
  ' / ' + props.data.length)
const option = computed(() => {
  const rows = [...visibleRows.value].reverse()
  return {
    tooltip: { trigger: 'axis', appendToBody: true, axisPointer: { type: 'shadow' },
      formatter: p => p[0].name + '<br/>支付件数：' + Number(p[0].value).toLocaleString() },
    grid: { left: 12, right: 48, top: 22, bottom: 14, containLabel: true },
    xAxis: { type: 'value', show: false },
    yAxis: { type: 'category', data: rows.map(x => x.name), axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { interval: 0, hideOverlap: false, width: 150, overflow: 'truncate', fontSize: 12, margin: 18 } },
    series: [{ type: 'bar', barMaxWidth: 13, showBackground: true,
      backgroundStyle: { color: 'rgba(142,78,198,.10)', borderRadius: 6 },
      itemStyle: { borderRadius: 6, color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
        { offset: 0, color: '#8e4ec6' }, { offset: 1, color: '#d864d8' }
      ]) },
      emphasis: { itemStyle: { color: '#e38edc' } },
      label: { show: true, position: 'right', distance: 6, fontSize: 11, color: '#f4f7ff' },
      data: rows.map(x => x.value) }]
  }
})
const render = () => chart?.setOption(option.value, true)
const resize = () => chart?.resize()
const stopRotation = () => { if (timer) clearInterval(timer); timer = undefined }
const startRotation = () => {
  stopRotation()
  if (pageCount.value > 1) timer = setInterval(() => { page.value = (page.value + 1) % pageCount.value }, 6000)
}
onMounted(() => {
  chart = echarts.init(el.value, 'warehouse')
  render()
  window.addEventListener('resize', resize)
  startRotation()
})
watch(option, render)
watch(() => props.data.length, () => { page.value = 0; startRotation() })
onBeforeUnmount(() => { stopRotation(); window.removeEventListener('resize', resize); chart?.dispose() })
</script>

<template>
  <div class="product-rank-chart" @mouseenter="stopRotation" @mouseleave="startRotation">
    <div ref="el" class="chart" role="img" :aria-label="'商品支付件数排行，当前显示第 ' + (page + 1) + ' 页，共 ' + pageCount + ' 页'"></div>
    <span v-if="pageLabel" class="product-rank-page">{{ pageLabel }}</span>
  </div>
</template>

<style scoped>
.product-rank-chart { position: relative; width: 100%; height: 100%; min-height: 0; }
.product-rank-chart .chart { min-height: 0; }
.product-rank-page { position: absolute; top: 2px; right: 9px; color: #a883cd; font: 10px Consolas, monospace; letter-spacing: .05em; pointer-events: none; }
</style>
