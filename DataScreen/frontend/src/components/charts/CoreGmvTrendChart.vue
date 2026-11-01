<script setup>
import {computed,onBeforeUnmount,onMounted,ref,watch} from 'vue'
import {echarts} from '../../config/echarts'
import {formatCurrency,formatCurrencyCompact,summarizeTrend} from '../../utils/chartTransforms'
const props=defineProps({data:{type:Array,default:()=>[]}})
const el=ref();let chart
const summary=computed(()=>summarizeTrend(props.data))
const option=computed(()=>({animationDuration:750,tooltip:{trigger:'axis',appendToBody:true,backgroundColor:'rgba(13,21,32,.96)',borderColor:'#3b9eff',textStyle:{color:'#edeef0'},formatter:p=>`${props.data[p[0].dataIndex]?.time}<br/>支付 GMV：${formatCurrency(p[0].value)}`},grid:{left:68,right:24,top:48,bottom:36},xAxis:{type:'category',boundaryGap:false,data:props.data.map(x=>x.time.slice(5,16).replace('T',' ')),axisLine:{lineStyle:{color:'rgba(59,158,255,.35)'}},axisTick:{show:false},axisLabel:{color:'#8b98b8'}},yAxis:{type:'value',axisLabel:{formatter:formatCurrencyCompact},splitNumber:4,splitLine:{lineStyle:{color:'rgba(59,158,255,.1)'}}},dataZoom:props.data.length>20?[{type:'inside',start:60,end:100}]:[],series:[{name:'支付 GMV',type:'line',smooth:.35,showSymbol:props.data.length<12,symbol:'circle',symbolSize:7,lineStyle:{width:3,color:'#8e7dff',shadowBlur:8,shadowColor:'rgba(110,86,207,.45)'},itemStyle:{color:'#2ec5ce',borderColor:'#edeef0',borderWidth:1},emphasis:{scale:1.6,focus:'series'},areaStyle:{opacity:1,color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(110,86,207,.55)'},{offset:.55,color:'rgba(59,158,255,.20)'},{offset:1,color:'rgba(46,197,206,0)'}])},markPoint:{symbol:'pin',symbolSize:38,label:{fontSize:9,formatter:'峰值'},itemStyle:{color:'#d864d8'},data:[{type:'max',name:'峰值'}]},data:props.data.map(x=>x.paidAmount)}]}))
const render=()=>chart?.setOption(option.value,true)
onMounted(()=>{chart=echarts.init(el.value,'warehouse');render();addEventListener('resize',render)});watch(option,render,{deep:true});onBeforeUnmount(()=>{removeEventListener('resize',render);chart?.dispose()})
</script>
<template><div class="core-trend"><div class="trend-summary"><div><span>最新窗口</span><strong>{{formatCurrencyCompact(summary.latest)}}</strong></div><div><span>区间峰值</span><strong>{{formatCurrencyCompact(summary.peak)}}</strong></div></div><div ref="el" class="chart" role="img" aria-label="支付 GMV 核心面积趋势图"></div></div></template>
