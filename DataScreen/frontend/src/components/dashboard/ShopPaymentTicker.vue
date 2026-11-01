<script setup>
import {computed} from 'vue'
import {formatCurrency,formatCurrencyCompact} from '../../utils/chartTransforms'

const props=defineProps({data:{type:Array,default:()=>[]}})
const records=computed(()=>props.data.filter(row=>row&&Number.isFinite(Number(row.paidAmount))&&Number.isFinite(Number(row.paidOrderCount))).map((row,index)=>({
  id:row.shopId??index,
  name:row.name||`店铺 ${row.shopId}`,
  orders:`支付 ${Number(row.paidOrderCount).toLocaleString('zh-CN')} 单`,
  amount:formatCurrencyCompact(row.paidAmount),
  detail:`${row.name||`店铺 ${row.shopId}`} · 支付订单 ${Number(row.paidOrderCount).toLocaleString('zh-CN')} 单 · 支付金额 ${formatCurrency(row.paidAmount)}（店铺汇总，非逐笔记录）`
})))
const loopRecords=computed(()=>records.value.length?Array.from({length:Math.max(6,records.value.length)},(_,index)=>records.value[index%records.value.length]):[])
const duration=computed(()=>`${Math.max(20,loopRecords.value.length*3.2)}s`)
</script>

<template>
  <div class="payment-ticker" aria-label="店铺支付汇总循环播报">
    <div class="payment-ticker-track" :style="{'--ticker-duration':duration}">
      <ol v-for="copy in 2" :key="copy" class="payment-ticker-list" :aria-hidden="copy===2?'true':undefined">
        <li v-for="(record,index) in loopRecords" :key="`${copy}-${index}`" class="payment-ticker-row" :title="record.detail" :aria-hidden="index>=records.length?'true':undefined">
          <span class="payment-ticker-index">{{String(index%records.length+1).padStart(2,'0')}}</span>
          <span class="payment-ticker-name">{{record.name}}</span>
          <span class="payment-ticker-orders">{{record.orders}}</span>
          <strong class="payment-ticker-amount">{{record.amount}}</strong>
        </li>
      </ol>
    </div>
  </div>
</template>

<style scoped>
.payment-ticker{height:154px;overflow:hidden;mask-image:linear-gradient(to bottom,transparent,#000 9%,#000 91%,transparent)}
.payment-ticker-track{animation:payment-ticker-scroll var(--ticker-duration) linear infinite;will-change:transform}
.payment-ticker:hover .payment-ticker-track{animation-play-state:paused}
.payment-ticker-list{margin:0;padding:0;list-style:none}
.payment-ticker-row{height:30px;display:grid;grid-template-columns:24px minmax(0,1fr) auto auto;align-items:center;gap:10px;padding:0 10px;border-bottom:1px solid rgba(59,158,255,.12);background:linear-gradient(90deg,rgba(46,197,206,.055),transparent 75%);white-space:nowrap;font-size:11px}
.payment-ticker-row:nth-child(even){background:linear-gradient(90deg,rgba(59,158,255,.075),transparent 75%)}
.payment-ticker-index{font:10px Consolas,monospace;color:var(--trade-primary)}
.payment-ticker-name{overflow:hidden;text-overflow:ellipsis;color:var(--text-primary)}
.payment-ticker-orders{color:var(--text-secondary)}
.payment-ticker-amount{min-width:56px;text-align:right;color:#79dbf5;font-weight:600}
@keyframes payment-ticker-scroll{to{transform:translateY(-50%)}}
@media(prefers-reduced-motion:reduce){.payment-ticker-track{animation:none}}
</style>
