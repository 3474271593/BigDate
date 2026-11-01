import test from 'node:test'
import assert from 'node:assert/strict'
import {prepareUserAmountDepthRank} from '../src/utils/userAmountDepth.js'

test('depth rank keeps real userId amounts and scales bars from zero',()=>{
  const rows=prepareUserAmountDepthRank([
    {id:3,name:'userId 3',value:50},
    {id:2,name:'userId 2',value:100.25},
    {id:4,name:'userId 4',value:0}
  ])
  assert.deepEqual(rows.map(row=>row.id),[2,3,4])
  assert.deepEqual(rows.map(row=>row.width),['100%','49.88%','0%'])
  assert.equal(rows[0].exact,'¥100.25')
  assert.equal(rows[0].name,'userId 2')
})
