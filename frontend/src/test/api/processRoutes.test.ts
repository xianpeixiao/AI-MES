import { describe, expect, it } from 'vitest'
import {
  buildOperationPayload,
  buildSopPreviewUrl,
  materialTypeLabel,
  routeStatusLabel,
  routeStatusType,
  type ProcessOperation
} from '@/api/processRoutes'

describe('processRoutes helpers', () => {
  it('routeStatusLabel maps known statuses', () => {
    expect(routeStatusLabel('published')).toBe('已发布')
    expect(routeStatusLabel('unknown')).toBe('unknown')
    expect(routeStatusLabel()).toBe('--')
  })

  it('routeStatusType maps tag types', () => {
    expect(routeStatusType('published')).toBe('success')
    expect(routeStatusType('rejected')).toBe('danger')
    expect(routeStatusType('draft')).toBe('info')
  })

  it('materialTypeLabel defaults to raw material', () => {
    expect(materialTypeLabel('semi')).toBe('半成品')
    expect(materialTypeLabel()).toBe('原材料')
  })

  it('buildOperationPayload trims names and filters empty parameters', () => {
    const operations: ProcessOperation[] = [
      {
        seqNo: 1,
        operationName: '  下料  ',
        parameters: [{ paramName: '  ' }, { paramName: '厚度', paramValue: '2mm' }],
        devices: [
          { bindType: 'device', deviceId: 10 },
          { bindType: 'category', categoryId: 3 }
        ],
        materials: [{ materialId: 5, qty: 2, materialType: '', remark: '主材' }]
      }
    ]

    const payload = buildOperationPayload(operations)
    expect(payload[0].operationName).toBe('下料')
    expect(payload[0].parameters).toEqual([{ paramName: '厚度', paramValue: '2mm' }])
    expect(payload[0].deviceIds).toEqual([10])
    expect(payload[0].categoryIds).toEqual([3])
    expect(payload[0].materials[0].materialType).toBe('raw')
  })

  it('buildSopPreviewUrl joins base URL and sop id', () => {
    expect(buildSopPreviewUrl(99)).toMatch(/\/process-routes\/sop\/99\/file$/)
  })
})
