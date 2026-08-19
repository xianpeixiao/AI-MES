import { describe, expect, it } from 'vitest'
import {
  SCENARIO_PRESETS,
  buildApplyDiffRows,
  buildGanttItems,
  enrichTeamLoads,
  exceptionTypeLabel,
  hasAiSchedulingInfo,
  parseDispatchHours,
  parseStartTime,
  statusLabel
} from '@/utils/schedulingHelpers'

describe('schedulingHelpers', () => {
  it('parseDispatchHours treats placeholder values as default hours', () => {
    expect(parseDispatchHours('4h')).toBe(4)
    expect(parseDispatchHours('待定')).toBe(2)
    expect(parseDispatchHours('--')).toBe(2)
  })

  it('parseStartTime accepts datetime and HH:mm formats', () => {
    const full = parseStartTime('2026-08-19 08:30', '2026-08-19')
    const short = parseStartTime('08:30', '2026-08-19')

    expect(full?.getHours()).toBe(8)
    expect(short?.getMinutes()).toBe(30)
    expect(parseStartTime('--', '2026-08-19')).toBeNull()
  })

  it('buildGanttItems computes timeline bars from dispatches', () => {
    const items = buildGanttItems(
      [
        {
          workOrderCode: 'WO-001',
          teamName: '甲班',
          startTime: '2026-08-19 08:00',
          hours: '4'
        },
        {
          workOrderCode: 'WO-002',
          teamName: '乙班',
          startTime: '2026-08-19 12:00',
          hours: '2'
        }
      ],
      '2026-08-19'
    )

    expect(items).toHaveLength(2)
    expect(items[0].workOrderCode).toBe('WO-001')
    expect(items[0].startLabel).toBe('08:00')
    expect(items[1].endLabel).toBe('14:00')
    expect(items[0].widthPercent).toBeGreaterThan(0)
  })

  it('enrichTeamLoads boosts load rate by proposed hours', () => {
    const enriched = enrichTeamLoads(
      [{ teamName: '甲班', loadRate: 40, activeTaskCount: 2 }],
      [{ teamName: '甲班', hours: '4' }]
    )

    expect(enriched[0].proposedHours).toBe(4)
    expect(enriched[0].loadRate).toBe(72)
  })

  it('buildApplyDiffRows detects team and priority changes', () => {
    const rows = buildApplyDiffRows(
      [{ orderNo: 'WO-001', teamName: '未分配', priorityLabel: '中' }],
      [{ workOrderCode: 'WO-001', priorityLabel: '高', reason: '交期紧' }],
      [{ workOrderCode: 'WO-001', teamName: '甲班', startTime: '2026-08-19 08:00', hours: '4' }]
    )

    expect(rows).toHaveLength(1)
    expect(rows[0].teamChanged).toBe(true)
    expect(rows[0].priorityChanged).toBe(true)
    expect(rows[0].hasChanges).toBe(true)
    expect(rows[0].priorityReason).toBe('交期紧')
  })

  it('hasAiSchedulingInfo detects scheduling metadata on work order detail', () => {
    expect(hasAiSchedulingInfo(null)).toBe(false)
    expect(hasAiSchedulingInfo({ schedulingReason: '  ' })).toBe(false)
    expect(hasAiSchedulingInfo({ schedulingReason: '建议优先排产' })).toBe(true)
    expect(hasAiSchedulingInfo({ scheduledStartTime: '2026-08-19 08:00' })).toBe(true)
  })

  it('statusLabel and exceptionTypeLabel map known codes', () => {
    expect(statusLabel('producing')).toBe('生产中')
    expect(statusLabel('unknown')).toBe('unknown')
    expect(exceptionTypeLabel('quality')).toBe('质量异常')
    expect(exceptionTypeLabel('material')).toBe('缺料')
  })

  it('SCENARIO_PRESETS expose three constraint profiles', () => {
    expect(SCENARIO_PRESETS).toHaveLength(3)
    expect(SCENARIO_PRESETS.find((item) => item.id === 'deadline')?.teamConstraint).toBe(true)
    expect(SCENARIO_PRESETS.find((item) => item.id === 'material')?.deviceConstraint).toBe(true)
  })
})
