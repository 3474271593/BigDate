import assert from 'node:assert/strict'
import test from 'node:test'
import {createBackgroundGlobeOption,decorativeArcs} from '../src/config/backgroundGlobe.js'

test('background globe stays decorative, silent and tooltip-free',()=>{
  const option=createBackgroundGlobeOption(false)
  assert.equal(option.tooltip.show,false)
  assert.ok(option.series.length>=2)
  assert.ok(option.series.every(series=>series.silent===true))
  assert.ok(option.series.every(series=>series.coordinateSystem==='globe'))
})

test('decorative coordinates are deterministic and valid longitude/latitude pairs',()=>{
  assert.ok(decorativeArcs.length>0)
  decorativeArcs.flatMap(item=>item.coords).forEach(([longitude,latitude])=>{
    assert.ok(longitude>=-180&&longitude<=180)
    assert.ok(latitude>=-90&&latitude<=90)
  })
})

test('reduced motion disables rotation and moving light trails',()=>{
  const option=createBackgroundGlobeOption(true)
  assert.equal(option.globe.viewControl.autoRotate,false)
  assert.ok(option.series.every(series=>series.effect?.show!==true))
})
