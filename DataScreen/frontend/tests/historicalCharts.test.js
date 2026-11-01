import test from 'node:test'
import assert from 'node:assert/strict'
import {selectHeatmapDay,bubbleRadius,groupBubblesByDate,useLogRevenueAxis} from '../src/utils/historicalCharts.js'

const heatmap={
 dates:[{date:'2019-06-22',windows:2},{date:'2019-09-05',windows:1}],
 shops:[{shopId:1,name:'A'},{shopId:2,name:'B'}],
 windows:[{time:'2019-06-22T11:11:00+08:00'},{time:'2019-06-22T11:12:00+08:00'},{time:'2019-09-05T20:27:00+08:00'}],
 data:[
  {time:'2019-06-22T11:11:00+08:00',shopId:1,paidOrders:0,paidAmount:0},
  {time:'2019-06-22T11:11:00+08:00',shopId:2,paidOrders:2,paidAmount:20.01},
  {time:'2019-06-22T11:12:00+08:00',shopId:1,paidOrders:3,paidAmount:30.02},
  {time:'2019-09-05T20:27:00+08:00',shopId:2,paidOrders:1,paidAmount:9.99}
 ]
}

test('heatmap day selection retains real zero and leaves absent shop-window blank',()=>{
 const view=selectHeatmapDay(heatmap,'2019-06-22','paidOrders')
 assert.equal(view.windows.length,2)
 assert.deepEqual(view.cells.map(x=>x.value),[[0,0,0],[0,1,2],[1,0,3]])
 assert.equal(view.max,3)
 assert.equal(view.cells.find(x=>x.value[0]===1&&x.value[1]===1),undefined)
 const amount=selectHeatmapDay(heatmap,'2019-06-22','paidAmount')
 assert.deepEqual(amount.cells.map(x=>x.value[2]),[0,20.01,30.02])
 assert.equal(amount.max,30.02)
})

test('bubble data groups by actual paid date and maps real GMV monotonically to bounded sizes',()=>{
 const points=[
  {date:'2019-06-22',time:'a',paidOrders:1,avgOrderPaid:500,paidAmount:500},
  {date:'2019-09-05',time:'b',paidOrders:100,avgOrderPaid:500000,paidAmount:50000000}
 ]
 const groups=groupBubblesByDate(points)
 assert.deepEqual(groups.map(x=>x.date),['2019-06-22','2019-09-05'])
 assert.deepEqual(groups[0].points[0].value,[1,500,500])
 assert.ok(bubbleRadius(500,500,50000000)<bubbleRadius(50000000,500,50000000))
 assert.ok(bubbleRadius(500,500,50000000)>=7)
 assert.ok(bubbleRadius(50000000,500,50000000)<=22)
 assert.equal(useLogRevenueAxis(points),true)
 assert.equal(useLogRevenueAxis([{avgOrderPaid:0},{avgOrderPaid:500000}]),false)
})
