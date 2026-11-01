<script setup>
import {computed,onBeforeUnmount,ref} from 'vue'
import DataQualityPopover from './DataQualityPopover.vue'

const props=defineProps({dataset:String,meta:Object,dataAsOf:String,notes:Array,loading:Boolean})
const emit=defineEmits(['change','refresh'])
const now=ref('')
const tick=()=>now.value=new Intl.DateTimeFormat('zh-CN',{dateStyle:'medium',timeStyle:'medium',timeZone:'Asia/Shanghai',hour12:false}).format(new Date())
tick()
const timer=setInterval(tick,1000)
onBeforeUnmount(()=>clearInterval(timer))
const range=computed(()=>props.meta?.actualStart&&props.meta?.actualEnd?`${props.meta.actualStart} — ${props.meta.actualEnd}`:'正在读取真实时间范围')
</script>

<template>
  <header class="dash-header">
    <div class="wing left"><span>ODS / DWD / DWS</span></div>
    <div class="title-block">
      <span class="eyebrow">REALTIME DATA WAREHOUSE · OPERATIONS COCKPIT</span>
      <h1>电商实时数仓运营数据大屏</h1>
      <div class="pulse-track"><b></b></div>
    </div>
    <div class="wing right"><span>DRUID ANALYTICS</span></div>
    <div class="meta-row">
      <div class="switch" role="group" aria-label="数据集切换">
        <button :class="{active:dataset==='historical'}" @click="emit('change','historical')">历史基线分析 · 2019</button>
        <button :class="{active:dataset==='test'}" @click="emit('change','test')">实时链路监控 · 2026</button>
      </div>
      <div class="meta-marquee" aria-label="数据时间信息">
        <div class="meta-marquee-track">
          <div class="meta-marquee-group">
            <span>数据范围 {{range}}</span>
            <span>更新至 {{dataAsOf||'—'}}</span>
            <span>{{now}}</span>
          </div>
          <div class="meta-marquee-group" aria-hidden="true">
            <span>数据范围 {{range}}</span>
            <span>更新至 {{dataAsOf||'—'}}</span>
            <span>{{now}}</span>
          </div>
        </div>
      </div>
      <DataQualityPopover :notes="notes" :loading="loading" :active-days="meta?.activeDays" />
      <button class="refresh-button" aria-label="刷新数据" @click="emit('refresh')">↻</button>
    </div>
  </header>
</template>
