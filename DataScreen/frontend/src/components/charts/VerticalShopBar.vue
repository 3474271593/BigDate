<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { echarts } from '../../config/echarts'
import { formatCurrency, formatCurrencyCompact } from '../../utils/chartTransforms'

const props = defineProps({ data: { type: Array, default: () => [] } })
const el = ref()
let chart
let observer
const rows = computed(() => [...props.data].slice(0, 10))
const amounts = computed(() => rows.value.map(row => row.paidAmount))

const option = computed(() => ({
  animation: true,
  animationDuration: 650,
  tooltip: {
    trigger: 'axis',
    appendToBody: true,
    axisPointer: { type: 'shadow' },
    formatter: params => {
      const item = params.find(entry => entry.seriesName === '支付金额')
      if (!item) return ''
      const row = rows.value[item.dataIndex]
      return (row.name || '店铺 ' + row.shopId) +
        '<br/>店铺 ID：' + row.shopId +
        '<br/>支付金额：' + formatCurrency(row.paidAmount)
    }
  },
  grid: { left: 62, right: 16, top: 20, bottom: 66 },
  xAxis: {
    type: 'category',
    data: rows.value.map(row => row.name || '店铺 ' + row.shopId),
    axisTick: { show: false },
    axisLine: { lineStyle: { color: 'rgba(74, 132, 198, .35)' } },
    axisLabel: {
      interval: 0, fontSize: 10, color: '#97a9c6', rotate: 32, margin: 12,
      formatter: value => value.length > 6 ? value.slice(0, 6) + '…' : value
    }
  },
  yAxis: {
    type: 'value',
    min: 0,

    axisLabel: { formatter: formatCurrencyCompact, color: '#94a5c1', fontSize: 10 },
    axisLine: { show: false },
    axisTick: { show: false },
    splitNumber: 4,
    splitLine: { lineStyle: { color: 'rgba(59, 158, 255, .11)' } }
  },
  series: [
    {
      name: '底部光圈',
      type: 'pictorialBar',
      symbol: 'circle',
      symbolSize: [29, 8],
      symbolPosition: 'start',
      symbolOffset: [0, 1],
      silent: true,
      tooltip: { show: false },
      z: 1,
      itemStyle: { color: 'rgba(48, 156, 239, .34)', shadowColor: 'rgba(46, 197, 206, .42)', shadowBlur: 8 },
      data: amounts.value
    },
    {
      name: '支付金额',
      type: 'bar',
      barMaxWidth: 22,
      z: 2,
      data: rows.value.map(row => ({ value: row.paidAmount, shopId: row.shopId, shopName: row.name || '店铺 ' + row.shopId })),
      itemStyle: {
        borderColor: 'rgba(113, 221, 255, .46)',
        borderWidth: 1,
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: 'rgba(45, 96, 193, .9)' },
          { offset: .52, color: 'rgba(58, 157, 232, .78)' },
          { offset: 1, color: 'rgba(31, 94, 188, .86)' }
        ]),
        shadowColor: 'rgba(43, 154, 238, .27)',
        shadowBlur: 10
      },
      emphasis: { itemStyle: { borderColor: '#a1efff', shadowBlur: 18, shadowColor: 'rgba(65, 213, 255, .65)' } }
    },
    {
      name: '顶部椭圆',
      type: 'pictorialBar',
      symbol: 'circle',
      symbolSize: [22, 9],
      symbolPosition: 'end',
      symbolOffset: [0, -1],
      silent: true,
      tooltip: { show: false },
      z: 3,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#a2eeff' },
          { offset: 1, color: '#4baae8' }
        ]),
        shadowColor: 'rgba(93, 211, 255, .7)',
        shadowBlur: 10
      },
      data: amounts.value
    }
  ]
}))

const render = () => chart?.setOption(option.value, true)
onMounted(() => {
  chart = echarts.init(el.value, 'warehouse')
  render()
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(el.value)
})
watch(option, render)
onBeforeUnmount(() => { observer?.disconnect(); chart?.dispose() })
</script>
<template><div ref="el" class="chart" role="img" aria-label="店铺支付金额 TOP10 立体柱状图"></div></template>
