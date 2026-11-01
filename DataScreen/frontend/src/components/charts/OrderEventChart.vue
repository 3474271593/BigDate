<script setup>
import {computed,onBeforeUnmount,onMounted,ref,watch} from 'vue'
import {echarts} from '../../config/echarts'
import {toOrderEvents} from '../../utils/chartTransforms'
const props=defineProps({data:{type:Array,default:()=>[]},historical:Boolean})
const el=ref();let chart
const rows=computed(()=>toOrderEvents(props.data.slice(props.historical?0:-8),props.historical))
const option=computed(()=>({animation:false,tooltip:{trigger:'axis',appendToBody:true,axisPointer:{type:'line'},formatter:p=>{const row=rows.value[p[0].dataIndex],ratio=row.orderCount?(row.paidOrderCount/row.orderCount*100).toFixed(1):0;return `${row.sourceTime}<br/>下单订单：${row.orderCount.toLocaleString()}<br/>支付订单：${row.paidOrderCount.toLocaleString()}<br/>支付/下单比：${ratio}%`}},legend:{top:0,right:8,textStyle:{fontSize:11}},grid:{left:54,right:20,top:38,bottom:34},xAxis:{type:'category',boundaryGap:true,data:rows.value.map(x=>x.label),axisTick:{show:false}},yAxis:{type:'value',splitNumber:4,axisLabel:{formatter:v=>Number(v).toLocaleString()}},series:[{name:'下单订单',type:'bar',barMaxWidth:28,itemStyle:{borderRadius:[6,6,0,0],color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#70b8ff'},{offset:1,color:'#3451b2'}])},data:rows.value.map(x=>x.orderCount)},{name:'支付订单',type:'line',symbol:'diamond',symbolSize:11,lineStyle:{width:3,color:'#d864d8'},itemStyle:{color:'#d864d8',borderColor:'#f4f7ff',borderWidth:1},emphasis:{scale:1.4},data:rows.value.map(x=>x.paidOrderCount)}]}))
const render=()=>chart?.setOption(option.value,true)
onMounted(()=>{chart=echarts.init(el.value,'warehouse');render();addEventListener('resize',render)});watch(option,render,{deep:true});onBeforeUnmount(()=>{removeEventListener('resize',render);chart?.dispose()})
</script><template><div ref="el" class="chart" role="img" aria-label="真实活跃日下单柱与支付折线事件图"></div></template>
