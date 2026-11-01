import test from 'node:test'
import assert from 'node:assert/strict'
import {buildShopTime3DOption} from '../src/utils/shopTime3D.js'

test('maps real API cells to time x shop y paid amount z and dynamic visual range',()=>{
  const option=buildShopTime3DOption({times:['2026-10-07T21:00:00+08:00'],shops:[{shopId:7,shopName:'真实店铺'}],data:[{time:'2026-10-07T21:00:00+08:00',shopId:7,shopName:'真实店铺',paidAmount:1234.5,paidOrderCount:3}]})
  assert.equal(option.xAxis3D.type,'category')
  assert.equal(option.yAxis3D.type,'category')
  assert.equal(option.zAxis3D.type,'value')
  assert.equal(option.visualMap.max,1234.5)
  assert.equal(option.series[0].type,'bar3D')
  assert.equal(option.grid3D.environment,'none')
  assert.ok(option.yAxis3D.axisLabel.textStyle.fontSize>=11)
  assert.deepEqual(option.series[0].data[0].value,[0,0,1234.5])
  assert.match(option.tooltip.formatter({data:option.series[0].data[0]}),/¥1,234\.50/)
  assert.match(option.tooltip.formatter({data:option.series[0].data[0]}),/支付订单：3/)
})

test('empty shop-time result stays renderable',()=>{
  const option=buildShopTime3DOption({times:[],shops:[],data:[]})
  assert.deepEqual(option.series[0].data,[])
  assert.ok(option.visualMap.max>0)
})

test('3D view rotates rightward by default and can pause on hover',()=>{
  const rotating=buildShopTime3DOption()
  const paused=buildShopTime3DOption({},false)
  assert.equal(rotating.grid3D.viewControl.autoRotate,true)
  assert.equal(rotating.grid3D.viewControl.autoRotateDirection,'ccw')
  assert.ok(rotating.grid3D.viewControl.autoRotateSpeed>0)
  assert.equal(paused.grid3D.viewControl.autoRotate,false)
  assert.equal(paused.grid3D.viewControl.alpha,rotating.grid3D.viewControl.alpha)
  assert.equal(paused.grid3D.viewControl.beta,rotating.grid3D.viewControl.beta)
})
