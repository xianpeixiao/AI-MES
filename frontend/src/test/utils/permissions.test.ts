import { describe, expect, it } from 'vitest'
import { ALL_PERMISSIONS, matchPermission } from '@/utils/permissions'

describe('permissions', () => {
  it('matchPermission allows everything when fullAccess is true', () => {
    expect(matchPermission([], '生产计划', true)).toBe(true)
  })

  it('matchPermission checks single required permission', () => {
    expect(matchPermission(['工单管理', '排产'], '排产')).toBe(true)
    expect(matchPermission(['工单管理'], '排产')).toBe(false)
  })

  it('matchPermission accepts any permission from required array', () => {
    expect(matchPermission(['AI 客服'], ['排产', 'AI 客服'])).toBe(true)
    expect(matchPermission(['物料'], ['排产', 'AI 客服'])).toBe(false)
  })

  it('exports stable permission catalog', () => {
    expect(ALL_PERMISSIONS).toContain('生产计划')
    expect(ALL_PERMISSIONS).toContain('Coze 配置')
    expect(ALL_PERMISSIONS.length).toBe(17)
  })
})
