<script setup>
import {computed} from 'vue'

const props=defineProps({data:{type:Array,default:()=>[]}})
const products=computed(()=>props.data.filter(item=>item&&Number.isFinite(Number(item.value))&&Number(item.value)>0))
const loopProducts=computed(()=>products.value.length?Array.from({length:Math.max(6,products.value.length)},(_,index)=>products.value[index%products.value.length]):[])
const duration=computed(()=>`${Math.max(20,loopProducts.value.length*2.8)}s`)
</script>

<template>
  <div class="goods-ticker" aria-label="全部商品支付件数循环排行">
    <div class="goods-ticker-track" :style="{'--ticker-duration':duration}">
      <ol v-for="copy in 2" :key="copy" class="goods-ticker-list" :aria-hidden="copy===2?'true':undefined">
        <li v-for="(product,index) in loopProducts" :key="`${copy}-${index}`" class="goods-ticker-row" :title="`${product.name} · 支付 ${Number(product.value).toLocaleString('zh-CN')} 件`" :aria-hidden="index>=products.length?'true':undefined">
          <span class="goods-ticker-index">{{String(index%products.length+1).padStart(3,'0')}}</span>
          <span class="goods-ticker-name">{{product.name}}</span>
          <strong class="goods-ticker-value">{{Number(product.value).toLocaleString('zh-CN')}} 件</strong>
        </li>
      </ol>
    </div>
  </div>
</template>

<style scoped>
.goods-ticker{height:154px;overflow:hidden;mask-image:linear-gradient(to bottom,transparent,#000 9%,#000 91%,transparent)}
.goods-ticker-track{animation:goods-ticker-scroll var(--ticker-duration) linear infinite;will-change:transform}
.goods-ticker:hover .goods-ticker-track,.goods-ticker:focus-within .goods-ticker-track{animation-play-state:paused}
.goods-ticker-list{margin:0;padding:0;list-style:none}
.goods-ticker-row{height:30px;display:grid;grid-template-columns:30px minmax(0,1fr) auto;align-items:center;gap:10px;padding:0 10px;border-bottom:1px solid rgba(218,91,216,.14);background:linear-gradient(90deg,rgba(153,83,208,.07),transparent 75%);white-space:nowrap;font-size:11px}
.goods-ticker-row:nth-child(even){background:linear-gradient(90deg,rgba(218,91,216,.08),transparent 75%)}
.goods-ticker-index{font:10px Consolas,monospace;color:#c66bdb}
.goods-ticker-name{overflow:hidden;text-overflow:ellipsis;color:var(--text-primary)}
.goods-ticker-value{min-width:48px;text-align:right;color:#e4a2ec;font-weight:600}
@keyframes goods-ticker-scroll{to{transform:translateY(-50%)}}
@media(prefers-reduced-motion:reduce){.goods-ticker-track{animation:none}}
</style>
