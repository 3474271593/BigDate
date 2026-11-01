import test from 'node:test'
import assert from 'node:assert/strict'
import {formatCurrencyCompact,makeOrderStructure,summarizeTrend,toOrderEvents} from '../src/utils/chartTransforms.js'

test('currency axes use one compact Chinese unit strategy',()=>{
  assert.equal(formatCurrencyCompact(9984),'¥9,984')
  assert.equal(formatCurrencyCompact(998400),'¥99.84万')
  assert.equal(formatCurrencyCompact(998400399.81),'¥9.98亿')
})

test('order structure never assigns business meaning to a negative difference',()=>{
  assert.deepEqual(makeOrderStructure({orderCount:20015,paidOrderCount:19995}),[
    {name:'支付订单',value:19995},
    {name:'下单与支付计数差额',value:20}
  ])
  assert.equal(makeOrderStructure({orderCount:1,paidOrderCount:5})[1].value,0)
})

test('order events preserve only supplied dates and both real metrics',()=>{
  const rows=toOrderEvents([{date:'2019-06-22',orderCount:20,paidOrderCount:6},{date:'2019-09-05',orderCount:19995,paidOrderCount:19989}],true)
  assert.deepEqual(rows.map(x=>x.label),['06-22','09-05'])
  assert.deepEqual(rows[1],{label:'09-05',sourceTime:'2019-09-05',orderCount:19995,paidOrderCount:19989})
})

test('trend summary derives latest and peak without inventing a metric',()=>{
  assert.deepEqual(summarizeTrend([{time:'a',paidAmount:2},{time:'b',paidAmount:9},{time:'c',paidAmount:4}]),{latest:4,peak:9,peakTime:'b'})
})
