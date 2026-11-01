<script setup>
import {nextTick,onBeforeUnmount,onMounted,ref} from 'vue'
import * as echarts from 'echarts'
import 'echarts-gl'
import {createBackgroundGlobeOption} from '../../config/backgroundGlobe'

const host=ref(null)
let chart
let resizeObserver
let motionPreference
const render=()=>chart?.setOption(createBackgroundGlobeOption(motionPreference?.matches),true)

onMounted(async()=>{
  await nextTick()
  if(!host.value)return
  motionPreference=window.matchMedia('(prefers-reduced-motion: reduce)')
  try{
    chart=echarts.init(host.value,null,{renderer:'canvas',devicePixelRatio:Math.min(window.devicePixelRatio||1,1.5)})
    render()
    resizeObserver=new ResizeObserver(()=>chart?.resize())
    resizeObserver.observe(host.value)
    motionPreference.addEventListener?.('change',render)
  }catch(error){
    console.warn('装饰性 3D 背景不可用，已自动降级。',error)
  }
})

onBeforeUnmount(()=>{
  motionPreference?.removeEventListener?.('change',render)
  resizeObserver?.disconnect()
  chart?.dispose()
  chart=undefined
})
</script>

<template>
  <div class="dashboard-background" aria-hidden="true">
    <div ref="host" class="dashboard-background-canvas"></div>
  </div>
</template>
