const money = value => `¥${Number(value).toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2})}`
const escapeHtml = value => String(value).replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]))
const compact = value => value.length > 8 ? `${value.slice(0, 8)}…` : value
const timeLabel = value => String(value).slice(5, 16).replace('T', ' ')

export function buildShopTime3DOption(result = {}, autoRotate = true) {
  const times = result.times || []
  const shops = result.shops || []
  const rows = result.data || []
  const xIndex = new Map(times.map((time, index) => [time, index]))
  const yIndex = new Map(shops.map((shop, index) => [String(shop.shopId), index]))
  const points = rows.filter(row => xIndex.has(row.time) && yIndex.has(String(row.shopId))).map(row => ({
    value: [xIndex.get(row.time), yIndex.get(String(row.shopId)), Number(row.paidAmount)],
    time: row.time,
    shopName: row.shopName,
    paidOrderCount: row.paidOrderCount
  }))
  const max = Math.max(0, ...points.map(point => point.value[2]))
  return {
    backgroundColor: 'transparent',
    animation: false,
    tooltip: {
      show: true,
      appendToBody: true,
      backgroundColor: 'rgba(8, 15, 30, .96)',
      borderColor: '#3b9eff',
      textStyle: {color: '#edeef0'},
      formatter: param => {
        const item = param.data
        if (!item) return ''
        return `<strong>${escapeHtml(item.shopName)}</strong><br/>时间：${escapeHtml(item.time)}<br/>支付金额：${money(item.value[2])}<br/>支付订单：${Number(item.paidOrderCount).toLocaleString('zh-CN')}`
      }
    },
    visualMap: {
      show: true, type: 'continuous', dimension: 2, min: 0, max: max || 1,
      calculable: false, orient: 'vertical', right: 8, top: 'center', itemHeight: 120,
      text: ['高', '低'], textStyle: {color: '#b0b4ba', fontSize: 10},
      inRange: {color: ['#3451b2', '#3b9eff', '#2ec5ce']}
    },
    xAxis3D: {
      type: 'category', name: '时间', data: times.map(timeLabel),
      axisLabel: {interval: 0, formatter: value => value.slice(6), textStyle: {color: '#aabdd7', fontSize: 11}},
      axisLine: {lineStyle: {color: 'rgba(79, 145, 218, .42)'}}
    },
    yAxis3D: {
      type: 'category', name: '店铺', data: shops.map(shop => shop.shopName),
      axisLabel: {interval: 0, formatter: compact, textStyle: {color: '#c0cce0', fontSize: 12}},
      axisLine: {lineStyle: {color: 'rgba(79, 145, 218, .42)'}}
    },
    zAxis3D: {
      type: 'value', name: '支付金额', min: 0,
      axisLabel: {formatter: value => value >= 10000 ? `¥${(value / 10000).toFixed(1)}万` : `¥${Math.round(value)}`, textStyle: {color: '#aabdd7', fontSize: 11}},
      splitLine: {lineStyle: {color: 'rgba(59, 158, 255, .12)'}}
    },
    grid3D: {
      show: true, boxWidth: 190, boxDepth: 160, boxHeight: 90,
      top: -30, left: '4%', width: '88%', height: '90%',
      environment: 'none',
      axisPointer: {lineStyle: {color: '#85dce8'}},
      viewControl: {alpha: 22, beta: 32, distance: 285, minDistance: 220, maxDistance: 380, rotateSensitivity: 1,
        autoRotate, autoRotateDirection: 'ccw', autoRotateSpeed: 8},
      light: {main: {intensity: 1.1, shadow: false}, ambient: {intensity: .55}},
      postEffect: {enable: false}
    },
    series: [{
      type: 'bar3D', name: '支付金额', coordinateSystem: 'cartesian3D', data: points,
      barSize: 5, shading: 'lambert', itemStyle: {opacity: .82},
      emphasis: {itemStyle: {opacity: 1}}
    }]
  }
}
