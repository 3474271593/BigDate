import test from 'node:test'
import assert from 'node:assert/strict'
import { createLatestRequestRunner } from '../src/utils/requestCoordinator.js'

const deferred = () => {
  let resolve
  const promise = new Promise(r => { resolve = r })
  return { promise, resolve }
}

test('slow test response cannot overwrite newer historical response', async () => {
  const state = { dataset: 'test', payload: null }
  const runner = createLatestRequestRunner((dataset, payload) => {
    state.dataset = dataset
    state.payload = payload
  })
  const slowTest = deferred()
  const fastHistorical = deferred()
  const first = runner.run('test', () => slowTest.promise)
  const second = runner.run('historical', () => fastHistorical.promise)
  fastHistorical.resolve('historical-data')
  await second
  slowTest.resolve('test-data')
  await first
  assert.deepEqual(state, { dataset: 'historical', payload: 'historical-data' })
})
