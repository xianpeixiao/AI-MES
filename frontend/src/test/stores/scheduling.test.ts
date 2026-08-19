import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { USER_STORAGE_KEY } from '@/api/request'
import { useSchedulingStore } from '@/stores/scheduling'

function createMemoryStorage() {
  const map = new Map<string, string>()
  return {
    getItem: (key: string) => (map.has(key) ? map.get(key)! : null),
    setItem: (key: string, value: string) => {
      map.set(key, value)
    },
    removeItem: (key: string) => {
      map.delete(key)
    },
    clear: () => {
      map.clear()
    }
  }
}

const sampleResult = {
  summary: '建议优先排产 WO-001',
  priorities: [{ rank: 1, workOrderCode: 'WO-001', priorityLabel: '高', reason: '交期紧' }],
  bottlenecks: [{ processName: '装配', loadRate: 85, suggestion: '关注负荷' }],
  dispatches: [{ workOrderCode: 'WO-001', teamName: '甲班', startTime: '2026-08-19 08:00', hours: '4' }]
}

describe('scheduling store', () => {
  beforeEach(() => {
    vi.stubGlobal('sessionStorage', createMemoryStorage())
    vi.stubGlobal('localStorage', createMemoryStorage())
    setActivePinia(createPinia())
    sessionStorage.clear()
    localStorage.clear()
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify({ id: 7 }))
  })

  it('setSchedulingResult stores live result and marks hasResult', () => {
    const store = useSchedulingStore()
    store.hydrate()

    store.setSchedulingResult({
      result: sampleResult,
      resultMode: 'live',
      resultProvider: 'deepseek',
      resultHint: 'ok',
      appliedConstraints: {
        materialAvailability: true,
        deviceLoad: false,
        teamHours: true
      }
    })

    expect(store.hasResult).toBe(true)
    expect(store.resultMode).toBe('live')
    expect(store.resultProvider).toBe('deepseek')
    expect(store.appliedConstraints.deviceLoad).toBe(false)
    expect(store.generatedAt).toBeTruthy()
  })

  it('clearSchedulingResult resets result metadata', () => {
    const store = useSchedulingStore()
    store.hydrate()
    store.setSchedulingResult({ result: sampleResult, resultMode: 'mock' })

    store.clearSchedulingResult()

    expect(store.hasResult).toBe(false)
    expect(store.resultMode).toBe('')
    expect(store.generatedAt).toBeNull()
  })

  it('resetForm restores defaults and clears context', () => {
    const store = useSchedulingStore()
    store.hydrate()
    store.form.selectedWorkOrderId = 99
    store.activePreset = 'deadline'
    store.setSchedulingResult({ result: sampleResult, resultMode: 'mock' })
    store.schedulingContext = {
      workOrders: [],
      materialAlerts: [],
      exceptions: [],
      teams: [],
      kpi: {}
    }

    store.resetForm()

    expect(store.form.selectedWorkOrderId).toBeNull()
    expect(store.activePreset).toBe('')
    expect(store.hasResult).toBe(false)
    expect(store.schedulingContext).toBeNull()
  })

  it('hydrate restores snapshot from sessionStorage', () => {
    sessionStorage.setItem(
      'ai_mes_scheduling_7',
      JSON.stringify({
        form: {
          planDate: '2026-08-20',
          selectedWorkOrderId: 12,
          materialConstraint: false,
          deviceConstraint: true,
          teamConstraint: false
        },
        activePreset: 'material',
        result: sampleResult,
        resultMode: 'mock',
        resultProvider: 'coze',
        resultHint: 'demo',
        resultSummary: sampleResult.summary,
        appliedConstraints: {
          materialAvailability: false,
          deviceLoad: true,
          teamHours: false
        },
        schedulingContext: null,
        generatedAt: '2026-08-19T08:00:00.000Z'
      })
    )

    const store = useSchedulingStore()
    store.hydrate()

    expect(store.form.planDate).toBe('2026-08-20')
    expect(store.form.selectedWorkOrderId).toBe(12)
    expect(store.activePreset).toBe('material')
    expect(store.result?.priorities[0].workOrderCode).toBe('WO-001')
    expect(store.resultMode).toBe('mock')
  })
})
