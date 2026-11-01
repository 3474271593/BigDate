<script setup>
import {computed,onBeforeUnmount,onMounted,ref,watch} from 'vue'
import {echarts} from '../../config/echarts'
import {formatCurrency,formatCurrencyCompact} from '../../utils/chartTransforms'

const props=defineProps({data:{type:Array,default:()=>[]},historical:{type:Boolean,default:false}})
const el=ref()
let chart
const point=row=>({name:row.userId||row.name,value:[Number(row.paidOrderCount)||0,Number(row.paidAmount)||0]})
const tooltip={appendToBody:true,formatter:p=>`订单 userId：${String(p.data.name).replace(/^用户\s*/,'')}<br/>支付订单：${Number(p.data.value[0]).toLocaleString()}<br/>支付金额：${formatCurrency(p.data.value[1])}`}
const scatter=(data,xAxisIndex=0,yAxisIndex=0)=>({
  type:'scatter',xAxisIndex,yAxisIndex,clip:false,symbolSize:9,
  itemStyle:{color:'#d864d8',opacity:.84},
  emphasis:{scale:1.5,itemStyle:{opacity:1,borderColor:'#f4f7ff',borderWidth:1}},
  data
})
const option=computed(()=>{
  const points=props.data.map(point)
  if(!props.historical){
    return {animation:false,tooltip,grid:{left:66,right:18,top:24,bottom:42},xAxis:{type:'value',name:'支付订单',nameLocation:'middle',nameGap:28},yAxis:{type:'value',name:'支付金额',axisLabel:{formatter:formatCurrencyCompact}},series:[{...scatter(points),symbolSize:v=>Math.max(7,Math.min(21,7+Math.log10(Number(v[1])+1)*1.8))}]}
  }
  const high=points.filter(p=>p.value[0]>=100)
  const low=points.filter(p=>p.value[0]<100)
  if(!high.length||!low.length){
    return {animation:false,tooltip,grid:{left:68,right:18,top:20,bottom:38},xAxis:{type:'value',name:'支付订单',nameLocation:'middle',nameGap:26,scale:true},yAxis:{type:'value',name:'支付金额',scale:true,axisLabel:{formatter:formatCurrencyCompact}},series:[scatter(points)]}
  }
  const highOrders=high.map(p=>p.value[0]),highAmounts=high.map(p=>p.value[1])
  const minOrders=Math.min(...highOrders),maxOrders=Math.max(...highOrders)
  const minAmount=Math.min(...highAmounts),maxAmount=Math.max(...highAmounts)
  const orderPad=Math.max(12,(maxOrders-minOrders)*.12)
  const amountPad=Math.max(1000000,(maxAmount-minAmount)*.14)
  const lowMaxOrders=Math.max(1,...low.map(p=>p.value[0]))
  const lowMaxAmount=Math.max(1,...low.map(p=>p.value[1]))
  const seen=new Map()
  const oneOrderCount=low.filter(p=>p.value[0]===1).length
  const lowPoints=low.map(p=>{
    const key=p.value.join(':')
    const index=seen.get(key)||0
    seen.set(key,index+1)
    return {...p,symbolOffset:key==='0:0'?[8+(index%5)*9,-7-Math.floor(index/5)*11]:p.value[0]===1?[(low.filter(item=>item.value[0]===1).findIndex(item=>item===p)-(oneOrderCount-1)/2)*7,0]:[0,0]}
  })
  return {
    animation:false,tooltip,
    title:[
      {text:`高订单 userId · ${high.length} 个`,left:'8%',top:0,textStyle:{fontSize:10,fontWeight:'normal',color:'#aab7d3'}},
      {text:`低订单 userId · ${low.length} 个`,left:'73%',top:0,textStyle:{fontSize:10,fontWeight:'normal',color:'#aab7d3'}}
    ],
    grid:[
      {left:64,right:'31%',top:27,bottom:37},
      {left:'75%',right:17,top:27,bottom:37}
    ],
    xAxis:[
      {type:'value',gridIndex:0,min:Math.max(0,minOrders-orderPad),max:maxOrders+orderPad,name:'支付订单',nameLocation:'middle',nameGap:25,splitNumber:4,axisLabel:{fontSize:10}},
      {type:'value',gridIndex:1,min:0,max:lowMaxOrders+0.25,splitNumber:1,axisLabel:{fontSize:10,formatter:v=>Number.isInteger(v)?v:''}}
    ],
    yAxis:[
      {type:'value',gridIndex:0,min:Math.max(0,minAmount-amountPad),max:maxAmount+amountPad,splitNumber:4,axisLabel:{fontSize:10,formatter:formatCurrencyCompact}},
      {type:'value',gridIndex:1,min:0,max:lowMaxAmount*1.15,splitNumber:2,axisLabel:{fontSize:9,formatter:formatCurrencyCompact}}
    ],
    series:[scatter(high),scatter(lowPoints,1,1)]
  }
})
const render=()=>chart?.setOption(option.value,true)
onMounted(()=>{chart=echarts.init(el.value,'warehouse');render();addEventListener('resize',render)})
watch(option,render,{deep:true})
onBeforeUnmount(()=>{removeEventListener('resize',render);chart?.dispose()})
</script>
<template><div ref="el" class="chart" role="img" :aria-label="`订单 userId 支付订单与支付金额分布，共 ${data.length} 个 ID`"></div></template>