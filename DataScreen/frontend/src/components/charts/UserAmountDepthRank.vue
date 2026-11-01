<script setup>
import {computed} from 'vue'
import {prepareUserAmountDepthRank} from '../../utils/userAmountDepth'

const props = defineProps({data: {type: Array, default: () => []}})
const rows = computed(() => prepareUserAmountDepthRank(props.data))
</script>

<template>
  <ol class="user-depth-rank" aria-label="历史订单 userId 支付金额前十名">
    <li v-for="row in rows" :key="row.id ?? row.name" class="user-depth-row"
        :title="`${row.name} · 支付金额 ${row.exact}`"
        :aria-label="`第 ${Number(row.rank)} 名，${row.name}，支付金额 ${row.exact}`"
        tabindex="0">
      <span class="user-depth-index">{{ row.rank }}</span>
      <span class="user-depth-name">{{ row.name }}</span>
      <span class="user-depth-track" aria-hidden="true">
        <span class="user-depth-prism" :style="{width: row.width}"></span>
      </span>
      <strong class="user-depth-value">{{ row.compact }}</strong>
    </li>
  </ol>
</template>

<style scoped>
.user-depth-rank{position:relative;isolation:isolate;display:grid;grid-template-rows:repeat(10,minmax(0,1fr));height:100%;margin:0;padding:3px 8px 3px 2px;list-style:none;overflow:hidden}
.user-depth-rank::after{content:"";position:absolute;z-index:2;top:0;bottom:0;left:-16%;width:10%;pointer-events:none;background:linear-gradient(90deg,transparent,rgba(46,197,206,.17),transparent);transform:skewX(-18deg);animation:depth-scan 8s ease-in-out infinite}
.user-depth-row{position:relative;z-index:1;display:grid;grid-template-columns:21px 63px minmax(0,1fr) 59px;gap:7px;align-items:center;min-width:0;min-height:0;color:var(--text-secondary);transition:background .25s ease,transform .25s ease}
.user-depth-row:hover,.user-depth-row:focus-visible{background:rgba(110,86,207,.12);transform:translateY(-1px);outline:none}
.user-depth-row:focus-visible{box-shadow:inset 0 0 0 1px var(--goods-primary)}
.user-depth-index{font:600 10px/1 Consolas,monospace;color:var(--goods-primary);opacity:.76}
.user-depth-name{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:11px;letter-spacing:.01em}
.user-depth-value{text-align:right;white-space:nowrap;color:#e6c5f6;font-size:11px;font-weight:700;font-variant-numeric:tabular-nums}
.user-depth-track{position:relative;height:11px;min-width:0;border-top:1px solid rgba(150,99,228,.15);border-bottom:1px solid rgba(101,65,171,.18);background:linear-gradient(180deg,rgba(124,87,192,.13),rgba(72,45,123,.08));transform:perspective(220px) rotateX(13deg)}
.user-depth-prism{position:absolute;left:0;top:0;height:7px;max-width:100%;background:linear-gradient(90deg,#6e56cf 0%,#9d5cdb 55%,#d864d8 100%);border-top:1px solid rgba(255,205,255,.58);box-shadow:0 2px 0 #533188,0 4px 0 rgba(45,27,89,.85),0 6px 10px rgba(157,92,219,.32);transform:skewX(-13deg);transform-origin:left center}
.user-depth-prism::after{content:"";position:absolute;right:-6px;top:0;width:6px;height:11px;background:linear-gradient(135deg,#dd8ae7,#743aa7 70%);clip-path:polygon(0 0,100% 35%,100% 100%,0 65%)}
.user-depth-row:hover .user-depth-prism,.user-depth-row:focus-visible .user-depth-prism{filter:brightness(1.2);box-shadow:0 2px 0 #7540aa,0 4px 0 rgba(45,27,89,.85),0 7px 15px rgba(216,100,216,.44)}
@keyframes depth-scan{0%,15%{left:-16%}65%,100%{left:108%}}
@media(prefers-reduced-motion:reduce){.user-depth-rank::after{animation:none;display:none}.user-depth-row{transition:none}}
</style>
