export const formatCurrency=value=>`¥${Number(value||0).toLocaleString('zh-CN',{maximumFractionDigits:2})}`

export function formatCurrencyCompact(value){
  const number=Number(value||0)
  if(Math.abs(number)>=100000000)return `¥${(number/100000000).toFixed(2).replace(/\.00$/,'')}亿`
  if(Math.abs(number)>=10000)return `¥${(number/10000).toFixed(2).replace(/\.00$/,'')}万`
  return `¥${number.toLocaleString('zh-CN',{maximumFractionDigits:0})}`
}

export function makeOrderStructure(overview={}){
  const ordered=Number(overview.orderCount||0)
  const paid=Number(overview.paidOrderCount||0)
  return [{name:'支付订单',value:paid},{name:'下单与支付计数差额',value:Math.max(ordered-paid,0)}]
}

export const truncateLabel=(value,max=12)=>String(value||'').length>max?`${String(value).slice(0,max)}…`:String(value||'')

export function toOrderEvents(data=[],historical=false){
  return data.map(row=>({
    label:historical?String(row.date).slice(5):String(row.time).slice(11,16),
    sourceTime:historical?row.date:row.time,
    orderCount:Number(row.orderCount||0),
    paidOrderCount:Number(row.paidOrderCount||0)
  }))
}

export function summarizeTrend(data=[]){
  if(!data.length)return{latest:0,peak:0,peakTime:''}
  const peakRow=data.reduce((peak,row)=>Number(row.paidAmount||0)>Number(peak.paidAmount||0)?row:peak,data[0])
  return{latest:Number(data.at(-1)?.paidAmount||0),peak:Number(peakRow.paidAmount||0),peakTime:peakRow.time||''}
}
