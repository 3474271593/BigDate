<script setup>
import {computed,onBeforeUnmount,onMounted,ref,watch} from 'vue'
import {echarts} from '../../config/echarts.js'
import {selectHeatmapDay} from '../../utils/historicalCharts.js'

const props=defineProps({data:{type:Object,required:true}})
const el=ref(null),selectedDate=ref(''),metric=ref('paidOrders')
let chart,observer
const dates=computed(()=>props.data?.dates||[])
watch(dates,items=>{if(!items.some(item=>item.date===selectedDate.value))selectedDate.value=[...items].sort((a,b)=>b.windows-a.windows)[0]?.date||''},{immediate:true})
const view=computed(()=>selectHeatmapDay(props.data,selectedDate.value,metric.value))
const money=value=>`¥${Number(value).toLocaleString('zh-CN',{minimumFractionDigits:2,maximumFractionDigits:2})}`
const escapeHtml=value=>String(value??'').replace(/[&<>"']/g,char=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]))
const clock=value=>String(value||'').replace('T',' ').replace('+08:00','')
const option=computed(()=>{
  const {shops,windows,cells,max}=view.value
  const start=Math.max(0,100-Math.round(40/Math.max(1,windows.length)*100))
  return {
    animationDuration:250,
    grid:{left:76,right:46,top:16,bottom:78,containLabel:false},
    tooltip:{trigger:'item',appendToBody:true,confine:false,backgroundColor:'#111927',borderColor:'#1d3e56',textStyle:{color:'#edeef0',fontSize:11},extraCssText:'max-width:245px;white-space:normal;line-height:1.5',formatter:params=>{
      const row=params.data?.raw
      if(!row)return ''
      return `<strong>${escapeHtml(row.shopName||'店铺 '+row.shopId)}</strong> · ID ${escapeHtml(row.shopId)}<br/>开始：${escapeHtml(clock(row.time))}<br/>结束：${escapeHtml(clock(row.windowEnd)||'—')}<br/>支付订单：${Number(row.paidOrders).toLocaleString('zh-CN')} 单<br/>支付金额：${money(row.paidAmount)}`
    }},
    xAxis:{type:'category',data:windows.map(window=>window.time),name:'分钟窗口',nameLocation:'middle',nameGap:38,nameTextStyle:{color:'#8fa4c4',fontSize:10},axisLabel:{color:'#8fa4c4',fontSize:10,formatter:value=>value.slice(11,16),interval:'auto'},axisLine:{lineStyle:{color:'#335678'}},axisTick:{show:false},splitArea:{show:false}},
    yAxis:{type:'category',data:shops.map(shop=>shop.name||`店铺 ${shop.shopId}`),axisLabel:{color:'#8fa4c4',fontSize:10,width:69,overflow:'truncate'},axisLine:{lineStyle:{color:'#335678'}},axisTick:{show:false},splitArea:{show:false}},
    visualMap:{min:0,max:Math.max(1,max),calculable:true,orient:'vertical',right:0,top:32,bottom:80,itemWidth:8,itemHeight:110,text:['高','低'],textStyle:{color:'#8fa4c4',fontSize:10},inRange:{color:['#172436','#235485','#357fbc','#2baac7','#66d2da']}},
    dataZoom:[{type:'inside',xAxisIndex:0,start,end:100,filterMode:'none',zoomOnMouseWheel:true},{type:'slider',xAxisIndex:0,start,end:100,height:10,bottom:35,showDetail:false,borderColor:'#1d3e56',backgroundColor:'rgba(17,25,39,.55)',fillerColor:'rgba(46,197,206,.28)',handleSize:8,textStyle:{color:'#8fa4c4'}}],
    series:[{type:'heatmap',data:cells,selectedMode:'single',itemStyle:{borderColor:'rgba(7,10,18,.72)',borderWidth:1},select:{itemStyle:{borderColor:'#dce7fa',borderWidth:2}},emphasis:{itemStyle:{borderColor:'#dce7fa',borderWidth:1.5,shadowBlur:7,shadowColor:'rgba(46,197,206,.45)'}}}]
  }
})
const render=()=>chart?.setOption(option.value,true)
onMounted(()=>{chart=echarts.init(el.value,'warehouse');render();observer=new ResizeObserver(()=>chart?.resize());observer.observe(el.value)})
watch(option,render)
onBeforeUnmount(()=>{observer?.disconnect();chart?.dispose()})
</script>

<template>
  <div class="historical-chart">
    <div class="historical-chart-toolbar" aria-label="热力图日期和指标">
      <button v-for="item in dates" :key="item.date" :class="{active:selectedDate===item.date}" @click="selectedDate=item.date">{{item.date.slice(5)}} <span>({{item.windows}})</span></button>
      <button :class="{active:metric==='paidOrders'}" @click="metric='paidOrders'">支付订单数</button>
      <button :class="{active:metric==='paidAmount'}" @click="metric='paidAmount'">支付金额</button>
    </div>
    <div class="historical-chart-note">空白=无记录 · 深色=真实零值 · 拖动底部时间轴查看全部窗口</div>
    <div ref="el" class="historical-chart-canvas" role="img" :aria-label="`店铺交易时序热力矩阵，${selectedDate}，${view.cells.length}个实际单元`"></div>
  </div>
</template>
