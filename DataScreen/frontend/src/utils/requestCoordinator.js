export function createLatestRequestRunner(apply) {
  let sequence = 0
  let controller = null
  return {
    async run(dataset, fetcher) {
      controller?.abort()
      controller = new AbortController()
      const requestId = ++sequence
      try {
        const payload = await fetcher(controller.signal)
        if (requestId === sequence && !controller.signal.aborted) apply(dataset, payload)
        return payload
      } catch (error) {
        if (error?.name === 'AbortError' || error?.code === 'ERR_CANCELED') return null
        throw error
      }
    },
    cancel() { controller?.abort(); sequence++ }
  }
}
