<script setup>
import {ref} from 'vue'
defineProps({notes:{type:Array,default:()=>[]},loading:Boolean,activeDays:[Number,String]})
const open=ref(false)
</script>
<template><div class="quality-popover"><button class="status" :class="{warn:notes.length}" :aria-expanded="open" aria-controls="quality-details" @click="open=!open">{{loading?'刷新中…':notes.length?`数据提示 ${notes.length}`:'数据状态正常'}}</button><div v-if="open" id="quality-details" class="quality-card" role="dialog" aria-label="数据质量与统计口径"><header><strong>数据质量与统计口径</strong><button aria-label="关闭数据提示" @click="open=false">×</button></header><div class="quality-state"><i :class="{ok:!notes.length}"></i><b>{{notes.length?'存在需关注的数据提示':'当前数据状态正常'}}</b><span>{{activeDays??'—'}} 个活跃日</span></div><ul v-if="notes.length"><li v-for="item in notes" :key="item">{{item}}</li></ul><p v-else>当前数据集未发现需要提示的问题。</p><small>支付/下单比为窗口指标；窗口计数跨窗口求和不代表全周期独立用户。</small></div></div></template>
