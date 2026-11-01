<script setup>
import {computed,onBeforeUnmount,onMounted,ref,watch} from 'vue'
import {echarts} from '../../config/echarts'
import {makeOrderStructure} from '../../utils/chartTransforms'
const props=defineProps({overview:{type:Object,default:()=>({})}})
const el=ref();let chart
const rows=computed(()=>makeOrderStructure(props.overview))
const ordered=computed(()=>Number(props.overview?.orderCount||0))
const ratio=computed(()=>ordered.value?`${(Number(props.overview.paidOrderCount||0)/ordered.value*100).toFixed(1)}%`:'—')
const option=computed(()=>({animation:false,color:['#8e4ec6','#3b9eff'],tooltip:{trigger:'item',appendToBody:true,formatter:p=>p.seriesName==='订单结构'?`${p.name}<br/>数量：${Number(p.value).toLocaleString()}<br/>占比：${p.percent}%`:''},series:[{name:'周期轨道',type:'pie',silent:true,radius:['80%','84%'],center:['33%','52%'],label:{show:false},data:[{value:1,itemStyle:{color:'rgba(59,158,255,.16)'}}]},{name:'订单结构',type:'pie',radius:['54%','74%'],center:['33%','52%'],startAngle:92,minAngle:2,padAngle:3,label:{show:false},itemStyle:{borderRadius:7,borderColor:'#0b1020',borderWidth:2},emphasis:{scale:true,scaleSize:5},data:rows.value}]}))
const render=()=>chart?.setOption(option.value,true)
const toggle=index=>chart?.dispatchAction({type:'pieToggleSelect',seriesIndex:1,dataIndex:index})
onMounted(()=>{chart=echarts.init(el.value,'warehouse');render();addEventListener('resize',render)});watch(option,render,{deep:true});onBeforeUnmount(()=>{removeEventListener('resize',render);chart?.dispose()})
</script>
<template><div class="order-cycle"><div ref="el" class="chart" role="img" aria-label="支付订单与下单计数差额周期环"></div><div class="cycle-center"><strong>{{ratio}}</strong><span>支付 / 下单</span></div><div class="cycle-legend"><button v-for="(item,index) in rows" :key="item.name" @click="toggle(index)"><i :class="`cycle-color-${index}`"></i><span>{{item.name}}</span><b>{{Number(item.value).toLocaleString()}}</b></button><small>窗口累计下单 {{ordered.toLocaleString()}}</small></div></div></template>
