<script setup>
import {computed,onBeforeUnmount,onMounted,ref,watch} from 'vue'
import {echarts} from '../../config/echarts.js'
import {bubbleRadius,groupBubblesByDate,useLogRevenueAxis} from '../../utils/historicalCharts.js'

const props=defineProps({data:{type:Object,required:true}})
const el=ref(null)
let chart,observer
const points=computed(()=>props.data?.points||[])
const groups=computed(()=>groupBubblesByDate(points.value))
const isLog=computed(()=>useLogRevenueAxis(points.value))
const amounts=computed(()=>points.value.map(point=>Number(point.paidAmount)))
const amountMin=computed(()=>Math.min(...amounts.value))
const amountMax=computed(()=>Math.max(...amounts.value))
const revenue=computed(()=>points.value.map(point=>Number(point.avgOrderPaid)))
const colors=['#2ec5ce','#3b9eff','#a56ae8','#cf62c2']
const money=value=>`¥${Number(value).toLocaleString('zh-CN',{minimumFractionDigits:2,maximumFractionDigits:2})}`
const escapeHtml=value=>String(value??'').replace(/[&<>"']/g,char=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]))
const clock=value=>String(value||'').replace('T',' ').replace('+08:00','')
const option=computed(()=>{
  const minimum=Math.min(...revenue.value),maximum=Math.max(...revenue.value)
  return {
    animationDuration:300,
    grid:{left:70,right:21,top:58,bottom:63,containLabel:false},
    legend:{top:5,left:2,itemWidth:9,itemHeight:9,textStyle:{color:'#b0b4ba',fontSize:10},selectedMode:true},
    tooltip:{trigger:'item',appendToBody:true,confine:false,backgroundColor:'#111927',borderColor:'#1d3e56',textStyle:{color:'#edeef0',fontSize:11},extraCssText:'max-width:255px;white-space:normal;line-height:1.5',formatter:params=>{
      const row=params.data?.raw
      if(!row)return ''
      return `<strong>${escapeHtml(row.date)}</strong><br/>开始：${escapeHtml(clock(row.time))}<br/>结束：${escapeHtml(clock(row.windowEnd)||'—')}<br/>支付订单：${Number(row.paidOrders).toLocaleString('zh-CN')} 单<br/>窗口支付金额：${money(row.paidAmount)}<br/>平均每单金额：${money(row.avgOrderPaid)}`
    }},
    xAxis:{type:'value',min:0,name:'支付订单数（单/分钟）',nameLocation:'middle',nameGap:32,nameTextStyle:{color:'#8fa4c4',fontSize:10},axisLabel:{color:'#8fa4c4',fontSize:10},axisLine:{lineStyle:{color:'#335678'}},splitLine:{lineStyle:{color:'rgba(59,158,255,.09)'}}},
    yAxis:{type:isLog.value?'log':'value',min:isLog.value?Math.pow(10,Math.floor(Math.log10(minimum))):0,max:isLog.value?Math.pow(10,Math.ceil(Math.log10(maximum))):undefined,logBase:10,name:'元/单',nameGap:12,nameTextStyle:{color:'#8fa4c4',fontSize:10},axisLabel:{color:'#8fa4c4',fontSize:10,formatter:value=>value>=10000?`¥${Math.round(value/10000)}万`:`¥${value}`},axisLine:{lineStyle:{color:'#335678'}},splitLine:{lineStyle:{color:'rgba(59,158,255,.09)'}}},
    dataZoom:[{type:'inside',xAxisIndex:0,filterMode:'none',zoomOnMouseWheel:'shift',moveOnMouseWheel:false},{type:'slider',xAxisIndex:0,bottom:17,height:9,showDetail:false,borderColor:'#1d3e56',backgroundColor:'rgba(17,25,39,.55)',fillerColor:'rgba(59,158,255,.26)',handleSize:8}],
    series:groups.value.map((group,index)=>({name:group.date,type:'scatter',data:group.points,clip:true,symbolSize:value=>bubbleRadius(value[2],amountMin.value,amountMax.value),itemStyle:{color:colors[index%colors.length],opacity:.66,borderColor:'rgba(220,231,250,.58)',borderWidth:.7},emphasis:{scale:1.25,itemStyle:{opacity:.96,borderColor:'#dce7fa',borderWidth:1.2}}}))
  }
})
const render=()=>chart?.setOption(option.value,true)
onMounted(()=>{chart=echarts.init(el.value,'warehouse');render();observer=new ResizeObserver(()=>chart?.resize());observer.observe(el.value)})
watch(option,render)
onBeforeUnmount(()=>{observer?.disconnect();chart?.dispose()})
</script>

<template>
  <div class="historical-chart">
    <div class="historical-chart-note">气泡大小仅表示窗口 GMV 规模 · 按住 Shift 滚轮缩放{{isLog?' · Y 轴对数刻度':''}}</div>
    <div ref="el" class="historical-chart-canvas" role="img" :aria-label="`历史交易量价关系气泡图，${points.length}个真实支付窗口`"></div>
  </div>
</template>
