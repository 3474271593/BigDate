<script setup>
import {onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {echarts} from '../../config/echarts'
import 'echarts-gl/lib/component/grid3D'
import 'echarts-gl/lib/chart/bar3D'
import {buildShopTime3DOption} from '../../utils/shopTime3D'

const props = defineProps({data: {type: Object, default: () => ({times: [], shops: [], data: []})}})
const el = ref(null)
let chart
let observer
let hovered = false
const render = () => chart?.setOption(buildShopTime3DOption(props.data, !hovered), true)
const setHovered = value => {
  hovered = value
  chart?.setOption({grid3D: {viewControl: {autoRotate: !hovered}}})
}

onMounted(() => {
  chart = echarts.init(el.value, 'warehouse')
  render()
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(el.value)
})
watch(() => props.data, render, {deep: true})
onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
})
</script>

<template><div ref="el" class="chart shop-time-3d" role="img" aria-label="店铺分时支付金额三维柱状图；时间、店铺和支付金额" @mouseenter="setHovered(true)" @mouseleave="setHovered(false)"></div></template>
