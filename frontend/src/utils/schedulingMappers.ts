import { priorityLabel } from '@/utils/labels'
import { normalizeList } from '@/utils/normalizeList'
import type { SchedulingResult } from '@/stores/scheduling'

export interface AppliedResultRow {
  code: string
  team: string
  priority: string
  startTime: string
  hours: string
}

export function parseLoadRate(value: unknown): number {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return Math.min(100, Math.max(0, value))
  }
  const text = String(value ?? '').replace('%', '').trim()
  const num = Number(text)
  return Number.isFinite(num) ? Math.min(100, Math.max(0, num)) : 0
}

export function mapSchedulingResult(dataObj: Record<string, unknown>): SchedulingResult {
  return {
    summary: String(dataObj.summary ?? ''),
    priorities: normalizeList(dataObj.priorities ?? dataObj.prioritySuggestions).map((item: any, index: number) => ({
      rank: Number(item.rank ?? index + 1),
      workOrderCode: String(item.workOrderCode ?? item.workOrderNo ?? item.orderNo ?? item.code ?? '--'),
      priorityLabel: String(item.priorityLabel ?? priorityLabel(item.priority) ?? '--'),
      reason: String(item.reason ?? item.comment ?? item.rationale ?? '--')
    })),
    bottlenecks: normalizeList(dataObj.bottlenecks ?? dataObj.bottleneckWarnings).map((item: any) => ({
      processName: String(item.processName ?? item.process ?? item.name ?? '--'),
      loadRate: parseLoadRate(item.loadRate ?? item.rate),
      suggestion: String(item.suggestion ?? item.advice ?? item.reason ?? '--')
    })),
    dispatches: normalizeList(dataObj.dispatches ?? dataObj.dispatchSuggestions).map((item: any) => ({
      workOrderCode: String(item.workOrderCode ?? item.workOrderNo ?? item.orderNo ?? item.code ?? '--'),
      teamName: String(item.teamName ?? item.team ?? item.suggestedTeam ?? '--'),
      startTime: String(item.startTime ?? item.suggestedStart ?? item.suggestedStartTime ?? '--'),
      hours: String(item.hours ?? item.estimatedHours ?? '--')
    }))
  }
}

export function formatAppliedDateTime(value: unknown) {
  const text = String(value ?? '').trim()
  if (!text) return ''
  return text.replace('T', ' ').substring(0, 16)
}

export function mapAppliedResultRows(applied: any[]): AppliedResultRow[] {
  return applied.map((item) => {
    const hoursRaw = item.estimatedHours ?? item.suggestedHours
    const hoursText = hoursRaw != null ? String(hoursRaw).replace(/小时/g, '').trim() : ''
    return {
      code: String(item.orderNo ?? item.workOrderCode ?? item.code ?? '--'),
      team: String(item.teamName ?? '未分配'),
      priority: priorityLabel(item.priority),
      startTime: formatAppliedDateTime(item.scheduledStartTime ?? item.suggestedStartTime),
      hours: hoursText
    }
  })
}
