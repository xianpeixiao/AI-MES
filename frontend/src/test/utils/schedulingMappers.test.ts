import { describe, expect, it } from 'vitest'
import {
  formatAppliedDateTime,
  mapAppliedResultRows,
  mapSchedulingResult,
  parseLoadRate
} from '@/utils/schedulingMappers'

describe('schedulingMappers', () => {
  describe('parseLoadRate', () => {
    it('clamps numeric values to 0–100', () => {
      expect(parseLoadRate(85)).toBe(85)
      expect(parseLoadRate(120)).toBe(100)
      expect(parseLoadRate(-5)).toBe(0)
    })

    it('parses percentage strings', () => {
      expect(parseLoadRate('72%')).toBe(72)
      expect(parseLoadRate(' 45 ')).toBe(45)
    })

    it('returns 0 for invalid input', () => {
      expect(parseLoadRate('abc')).toBe(0)
      expect(parseLoadRate(null)).toBe(0)
    })
  })

  describe('mapSchedulingResult', () => {
    it('maps API aliases into normalized scheduling result', () => {
      const result = mapSchedulingResult({
        summary: '排产建议已生成',
        prioritySuggestions: [
          { workOrderNo: 'WO-001', priority: 1, reason: '交期紧' }
        ],
        bottleneckWarnings: [
          { process: '焊接', rate: '88%', advice: '加人' }
        ],
        dispatchSuggestions: [
          { orderNo: 'WO-001', team: 'A班', suggestedStartTime: '2026-08-19T08:00', estimatedHours: 4 }
        ]
      })

      expect(result.summary).toBe('排产建议已生成')
      expect(result.priorities[0]).toMatchObject({
        rank: 1,
        workOrderCode: 'WO-001',
        priorityLabel: '高',
        reason: '交期紧'
      })
      expect(result.bottlenecks[0]).toMatchObject({
        processName: '焊接',
        loadRate: 88,
        suggestion: '加人'
      })
      expect(result.dispatches[0]).toMatchObject({
        workOrderCode: 'WO-001',
        teamName: 'A班',
        startTime: '2026-08-19T08:00',
        hours: '4'
      })
    })
  })

  describe('formatAppliedDateTime', () => {
    it('formats ISO timestamps for display', () => {
      expect(formatAppliedDateTime('2026-08-19T14:30:00')).toBe('2026-08-19 14:30')
      expect(formatAppliedDateTime('')).toBe('')
    })
  })

  describe('mapAppliedResultRows', () => {
    it('maps applied API rows for result table', () => {
      const rows = mapAppliedResultRows([
        {
          workOrderCode: 'WO-002',
          teamName: 'B班',
          priority: 2,
          scheduledStartTime: '2026-08-20T09:00:00',
          estimatedHours: '6小时'
        }
      ])

      expect(rows[0]).toEqual({
        code: 'WO-002',
        team: 'B班',
        priority: '中',
        startTime: '2026-08-20 09:00',
        hours: '6'
      })
    })
  })
})
