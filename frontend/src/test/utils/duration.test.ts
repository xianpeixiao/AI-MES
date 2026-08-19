import { describe, expect, it } from 'vitest'
import { formatDurationMinutes } from '@/utils/duration'

describe('duration', () => {
  it('formatDurationMinutes handles empty and zero values', () => {
    expect(formatDurationMinutes(null)).toBe('0分')
    expect(formatDurationMinutes(0)).toBe('0分')
  })

  it('formatDurationMinutes formats minutes only', () => {
    expect(formatDurationMinutes(25)).toBe('25分')
  })

  it('formatDurationMinutes formats hours only', () => {
    expect(formatDurationMinutes(120)).toBe('2小时')
  })

  it('formatDurationMinutes formats mixed hours and minutes', () => {
    expect(formatDurationMinutes(95)).toBe('1小时35分')
  })
})
