<script setup>
import {computed,onBeforeUnmount,onMounted,ref,watch} from 'vue'
import {echarts} from '../../config/echarts'

const props=defineProps({data:{type:Object,required:true}})
const el=ref(null)
let chart
let observer
const categories=[
  {key:'highValueBestSeller',label:'高价值畅销商品',color:'#2ec5ce'},
  {key:'highValueNiche',label:'高价值小众商品',color:'#ab4aba'},
  {key:'lowPriceVolume',label:'低价走量商品',color:'#3b9eff'},
  {key:'lowSalesLowValue',label:'低销量低价值商品',color:'#d9a441'}
]
const count=key=>props.data?.counts?.[key]??0
const money=value=>`¥${Number(value).toLocaleString('zh-CN',{minimumFractionDigits:2,maximumFractionDigits:2})}`
const escapeHtml=value=>String(value??'').replace(/[&<>"']/g,char=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]))
const range=computed(()=>{
  const {start,end}=props.data?.timeRange||{}
  return `${start?.replace('T',' ')||'—'} 至 ${end?.replace('T',' ')||'—'}`
})
const option=computed(()=>{
  const goods=props.data?.goods||[]
  const xSplit=Number(props.data?.thresholds?.salesUnits)||100
  const ySplit=Number(props.data?.thresholds?.avgUnitRevenue)||80
  const maxX=Math.max(xSplit*1.12,...goods.map(item=>Number(item.salesUnits)*1.08))
  const maxY=Math.max(ySplit*1.12,...goods.map(item=>Number(item.avgUnitRevenue)*1.08))
  return {
    animationDuration:350,
    grid:{left:58,right:20,top:22,bottom:52,containLabel:false},
    tooltip:{trigger:'item',appendToBody:true,confine:false,extraCssText:'max-width:320px;white-space:normal;overflow-wrap:anywhere;line-height:1.5',backgroundColor:'#111927',borderColor:'#3b9eff',textStyle:{color:'#edeef0',fontSize:12},formatter:param=>{
      const item=param.data?.raw
      if(!item)return ''
      return `<strong>${escapeHtml(item.goodsName)}</strong><br/>商品 ID：${escapeHtml(item.goodsId)}<br/>所属象限：${escapeHtml(param.seriesName)}<br/>累计支付件数：${Number(item.salesUnits).toLocaleString('zh-CN')} 件<br/>累计支付金额：${money(item.salesAmount)}<br/>平均每件成交金额：${money(item.avgUnitRevenue)} / 件<br/>数据范围：${escapeHtml(range.value)}`
    }},
    xAxis:{type:'value',min:0,max:Math.ceil(maxX/50)*50,name:'累计支付件数（件）',nameLocation:'middle',nameGap:28,axisLabel:{fontSize:10,color:'#9aa9c5'},nameTextStyle:{color:'#9aa9c5',fontSize:11},axisLine:{lineStyle:{color:'#335678'}},splitLine:{lineStyle:{color:'rgba(59,158,255,.09)'}}},
    yAxis:{type:'value',min:0,max:Math.ceil(maxY/20)*20,name:'元/件',nameGap:10,axisLabel:{fontSize:10,color:'#9aa9c5',formatter:value=>`¥${value}`},nameTextStyle:{color:'#9aa9c5',fontSize:11},axisLine:{lineStyle:{color:'#335678'}},splitLine:{lineStyle:{color:'rgba(59,158,255,.09)'}}},
    dataZoom:[{type:'inside',xAxisIndex:0,filterMode:'none',zoomOnMouseWheel:'shift',moveOnMouseWheel:false,moveOnMouseMove:false}],
    series:categories.map((category,index)=>({
      name:category.label,type:'scatter',clip:true,symbolSize:8,
      itemStyle:{color:category.color,opacity:.79,borderColor:'rgba(255,255,255,.32)',borderWidth:.5},
      emphasis:{scale:1.65,itemStyle:{opacity:1,borderColor:'#fff',borderWidth:1}},
      data:goods.filter(item=>item.quadrant===category.key).map(item=>({value:[item.salesUnits,item.avgUnitRevenue],raw:item})),
      ...(index===0?{markLine:{silent:true,symbol:'none',lineStyle:{color:'rgba(216,226,245,.52)',type:'dashed',width:1},data:[{xAxis:xSplit,label:{formatter:`${xSplit} 件`,position:'insideEndTop',color:'#b9c8e0',fontSize:10}},{yAxis:ySplit,label:{formatter:`¥${ySplit}/件`,position:'insideEndTop',color:'#b9c8e0',fontSize:10}}]}}:{})
    }))
  }
})
const render=()=>chart?.setOption(option.value,true)
onMounted(()=>{
  chart=echarts.init(el.value,'warehouse')
  render()
  observer=new ResizeObserver(()=>chart?.resize())
  observer.observe(el.value)
})
watch(()=>props.data,render,{deep:true})
onBeforeUnmount(()=>{observer?.disconnect();chart?.dispose()})
</script>

<template>
  <div class="quadrant-view">
    <div class="quadrant-summary" aria-label="四象限商品数量">
      <span v-for="category in categories" :key="category.key" :style="{'--dot-color':category.color}"><i></i>{{category.label}}<b>{{count(category.key)}}</b></span>
      <small>分割线：{{data.thresholds.salesUnits}} 件 / {{data.thresholds.avgUnitRevenue}} 元/件 · 共 {{data.totalGoods}} 种 · 按住 Shift 滚轮缩放</small>
    </div>
    <div ref="el" class="quadrant-canvas" role="img" :aria-label="`商品销量与成交价值四象限，共 ${data.totalGoods} 种商品`"></div>
  </div>
</template>
