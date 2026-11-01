import { onBeforeUnmount,ref } from 'vue'
import { loadDashboard } from '../api/dashboard'
import { dashboardConfig } from '../config/dashboardConfig'
import { createLatestRequestRunner } from '../utils/requestCoordinator'
const baseKeys=['overview','gmv','orders','goodsPaid','goodsOrder','shopAmount','shopOrder','shopUsers','userAmount','userOrder','meta','goodsShare','shopContribution','userDistribution','activeDays','shopTimeAmount']
export function useDashboard(){
 const dataset=ref(dashboardConfig.defaultDataset),data=ref({}),warnings=ref([]),loading=ref(false),error=ref(''),moduleErrors=ref({}),dataAsOf=ref(null); let timer
 const runner=createLatestRequestRunner((requestDataset,results)=>{if(requestDataset!==dataset.value)return;const keys=requestDataset==='historical'?[...baseKeys,'shopHeatmap','tradeVolumePrice']:[...baseKeys,'goodsQuadrant'];let failed=0;const next={...data.value};const nextWarnings=[];const nextErrors={};results.forEach((r,i)=>{if(r.status==='fulfilled'){next[keys[i]]=r.value.data;if(!['goodsQuadrant','shopHeatmap','tradeVolumePrice'].includes(keys[i]))dataAsOf.value=r.value.meta.dataAsOf;nextWarnings.push(...r.value.meta.warnings)}else{failed++;nextErrors[keys[i]]=r.reason?.message||'数据服务暂时不可用'}});moduleErrors.value=nextErrors;data.value=next;warnings.value=[...new Map(nextWarnings.map(w=>[w.code,w])).values()];error.value=failed?`${failed} 个数据模块刷新失败 · 当前展示上次结果`:''})
 const refresh=async()=>{const requested=dataset.value;loading.value=true;try{await runner.run(requested,signal=>loadDashboard({dataset:requested,granularity:requested==='historical'?'day':'minute',limit:10},signal))}catch{error.value='数据更新失败 · 当前展示上次结果'}finally{if(requested===dataset.value)loading.value=false}}
 const resetTimer=()=>{clearInterval(timer);if(dataset.value==='test')timer=setInterval(refresh,dashboardConfig.realtimeRefreshMs)}
 const switchDataset=async(value)=>{dataset.value=value;data.value={};warnings.value=[];error.value='';moduleErrors.value={};resetTimer();await refresh()}
 const start=()=>{refresh();resetTimer()}
 onBeforeUnmount(()=>{clearInterval(timer);runner.cancel()});return{dataset,data,warnings,loading,error,moduleErrors,dataAsOf,refresh,switchDataset,start}
}
