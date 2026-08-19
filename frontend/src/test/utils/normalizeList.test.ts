import { describe, expect, it } from 'vitest'
import { normalizeList } from '@/utils/normalizeList'

describe('normalizeList', () => {
  it('returns array input directly', () => {
    expect(normalizeList([1, 2])).toEqual([1, 2])
  })

  it('unwraps axios-style data arrays', () => {
    expect(normalizeList({ data: [{ id: 1 }] })).toEqual([{ id: 1 }])
  })

  it('extracts records from paginated payloads', () => {
    expect(normalizeList({ records: [{ id: 2 }] })).toEqual([{ id: 2 }])
    expect(normalizeList({ list: [{ id: 3 }] })).toEqual([{ id: 3 }])
    expect(normalizeList({ items: [{ id: 4 }] })).toEqual([{ id: 4 }])
  })

  it('returns empty array for unsupported shapes', () => {
    expect(normalizeList(null)).toEqual([])
    expect(normalizeList({ total: 0 })).toEqual([])
  })
})
