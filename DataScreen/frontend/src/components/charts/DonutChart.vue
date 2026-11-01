<script setup>
import {computed,onBeforeUnmount,onMounted,ref,watch} from 'vue'
import {echarts} from '../../config/echarts'
const props=defineProps({data:{type:Array,default:()=>[]}})
const el=ref();let chart
const total=computed(()=>props.data.reduce((n,x)=>n+Number(x.value||0),0))
const loopData=computed(()=>props.data.length?Array.from({length:Math.max(6,props.data.length)},(_,index)=>props.data[index%props.data.length]):[])
const duration=computed(()=>`${Math.max(20,loopData.value.length*2.4)}s`)
const colors=['#8e4ec6','#d864d8','#2ec5ce','#3b9eff','#6e56cf','#ab4aba']
const option=computed(()=>({
  animation:false,
  color:colors,tooltip:{trigger:'item',appendToBody:true,formatter:p=>`${p.name}<br/>支付件数：${Number(p.value).toLocaleString()}<br/>占比：${p.percent}%`},
  series:[{type:'pie',radius:['62%','84%'],center:['34%','52%'],padAngle:props.data.length>12?0:2,itemStyle:{borderRadius:props.data.length>12?0:5,borderColor:'#0b1026',borderWidth:props.data.length>12?.5:2},label:{show:false},data:props.data.map(x=>({name:x.name,value:x.value}))}]
}))
const render=()=>chart?.setOption(option.value,true)
const highlight=index=>chart?.dispatchAction({type:'highlight',seriesIndex:0,dataIndex:index})
const downplay=index=>chart?.dispatchAction({type:'downplay',seriesIndex:0,dataIndex:index})
const toggle=index=>chart?.dispatchAction({type:'pieToggleSelect',seriesIndex:0,dataIndex:index})
onMounted(()=>{chart=echarts.init(el.value,'warehouse');render();addEventListener('resize',render)})
watch(option,render,{deep:true});onBeforeUnmount(()=>{removeEventListener('resize',render);chart?.dispose()})
</script>
<template>
  <div class="donut-layout">
    <div ref="el" class="chart donut-canvas" role="img" aria-label="全部商品支付件数构成环形图"></div>
    <div class="donut-center"><strong>{{Number(total).toLocaleString()}}</strong><span>支付件数</span></div>
    <div class="goods-ticker" aria-label="全部商品支付件数循环列表">
      <div class="goods-ticker-track" :style="{'--goods-ticker-duration':duration}">
        <ol v-for="copy in 2" :key="copy" class="goods-ticker-list" :aria-hidden="copy===2?'true':undefined">
          <li v-for="(item,index) in loopData" :key="`${copy}-${index}`" class="goods-ticker-row"
            :title="`${item.name}：${Number(item.value).toLocaleString('zh-CN')} 件`"
            :aria-hidden="index>=data.length?'true':undefined" :tabindex="copy===1&&index<data.length?0:-1"
            @mouseenter="highlight(index%data.length)" @mouseleave="downplay(index%data.length)"
            @focus="highlight(index%data.length)" @blur="downplay(index%data.length)"
            @click="toggle(index%data.length)" @keydown.enter="toggle(index%data.length)">
            <i :style="{background:colors[index%colors.length]}"></i>
            <span>{{item.name}}</span>
            <b>{{Number(item.value).toLocaleString('zh-CN')}}</b>
          </li>
        </ol>
      </div>
    </div>
  </div>
</template>

<style scoped>
.goods-ticker{height:154px;min-width:0;overflow:hidden;mask-image:linear-gradient(to bottom,transparent,#000 8%,#000 92%,transparent)}
.goods-ticker-track{animation:goods-ticker-scroll var(--goods-ticker-duration) linear infinite;will-change:transform}
.goods-ticker:hover .goods-ticker-track,.goods-ticker:focus-within .goods-ticker-track{animation-play-state:paused}
.goods-ticker-list{margin:0;padding:0;list-style:none}
.goods-ticker-row{height:28px;display:grid;grid-template-columns:7px minmax(0,1fr) auto;gap:7px;align-items:center;padding:0 7px;border-bottom:1px solid rgba(171,74,186,.16);font-size:11px;color:var(--text-secondary);cursor:pointer}
.goods-ticker-row:nth-child(even){background:rgba(171,74,186,.05)}
.goods-ticker-row:hover,.goods-ticker-row:focus-visible{background:rgba(171,74,186,.13);outline:1px solid rgba(216,100,216,.35)}
.goods-ticker-row i{width:7px;height:7px;border-radius:2px}
.goods-ticker-row span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.goods-ticker-row b{color:var(--text-primary);font-weight:600}
@keyframes goods-ticker-scroll{to{transform:translateY(-50%)}}
@media(prefers-reduced-motion:reduce){.goods-ticker-track{animation:none}}
/* Anchor the value itself to the pie center; keep its caption directly below. */
.donut-center {
  left: 18.36%;
  top: 52%;
  width: 0;
  height: 0;
  display: block;
}
.donut-center strong,
.donut-center span {
  position: absolute;
  left: 0;
  display: block;
  white-space: nowrap;
}
.donut-center strong {
  top: 0;
  transform: translate(-50%, -50%);
}
.donut-center span {
  top: 16px;
  margin-top: 0;
  transform: translateX(-50%);
}
@media (max-width: 760px) {
  .donut-center { left: 17%; }
}


</style>
