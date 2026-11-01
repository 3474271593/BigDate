export function selectHeatmapDay(payload, date, metric='paidOrders') {
  const shops = payload?.shops || []
  const windows = (payload?.windows || []).filter(window => window.time?.slice(0,10) === date)
  const x = new Map(windows.map((window,index) => [window.time,index]))
  const y = new Map(shops.map((shop,index) => [String(shop.shopId),index]))
  const cells = (payload?.data || []).filter(row => row.date === date || row.time?.slice(0,10) === date).flatMap(row => {
    const column = x.get(row.time), line = y.get(String(row.shopId))
    return column === undefined || line === undefined ? [] : [{value:[column,line,Number(row[metric])],raw:row}]
  })
  return {shops,windows,cells,max:Math.max(0,...cells.map(cell=>cell.value[2]))}
}

export function bubbleRadius(amount,min,max) {
  const a = Number(amount), lo = Number(min), hi = Number(max)
  if (!Number.isFinite(a) || !Number.isFinite(lo) || !Number.isFinite(hi)) return 7
  if (hi <= lo) return 12
  return 7 + 15 * Math.sqrt(Math.max(0,Math.min(1,(a-lo)/(hi-lo))))
}

export function groupBubblesByDate(points=[]) {
  const grouped = new Map()
  for (const point of points) {
    if (!grouped.has(point.date)) grouped.set(point.date,[])
    grouped.get(point.date).push({value:[Number(point.paidOrders),Number(point.avgOrderPaid),Number(point.paidAmount)],raw:point})
  }
  return [...grouped].sort(([a],[b])=>a.localeCompare(b)).map(([date,points])=>({date,points}))
}

export function useLogRevenueAxis(points=[]) {
  const values = points.map(point=>Number(point.avgOrderPaid))
  return values.length>0 && values.every(value=>Number.isFinite(value) && value>0) && Math.max(...values)/Math.min(...values)>100
}
