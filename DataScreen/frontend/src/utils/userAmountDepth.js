import {formatCurrency, formatCurrencyCompact} from './chartTransforms.js'

export function prepareUserAmountDepthRank(data = []) {
  const sorted = [...data]
    .filter(row => Number.isFinite(Number(row.value)))
    .sort((a, b) => Number(b.value) - Number(a.value))
    .slice(0, 10)
  const maximum = Math.max(0, ...sorted.map(row => Number(row.value)))
  return sorted.map((row, index) => {
    const value = Math.max(0, Number(row.value))
    return {
      id: row.id,
      name: String(row.name || `userId ${row.id}`),
      rank: String(index + 1).padStart(2, '0'),
      value,
      width: `${maximum ? (value / maximum * 100).toFixed(2).replace(/\.00$/, '') : '0'}%`,
      compact: formatCurrencyCompact(value),
      exact: formatCurrency(value)
    }
  })
}
