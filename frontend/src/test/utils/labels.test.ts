import { describe, expect, it } from 'vitest'
import {
  exceptionTypeLabel,
  normalizePriority,
  priorityLabel,
  roleLabel
} from '@/utils/labels'

describe('labels', () => {
  it('priorityLabel maps numeric and string priorities', () => {
    expect(priorityLabel(1)).toBe('高')
    expect(priorityLabel('low')).toBe('低')
    expect(priorityLabel(undefined)).toBe('中')
  })

  it('normalizePriority converts labels to backend values', () => {
    expect(normalizePriority('high')).toBe(1)
    expect(normalizePriority('3')).toBe(3)
    expect(normalizePriority(undefined)).toBe(2)
  })

  it('exceptionTypeLabel maps production exception types', () => {
    expect(exceptionTypeLabel('device')).toBe('设备停机')
    expect(exceptionTypeLabel('shortage')).toBe('缺料')
    expect(exceptionTypeLabel('other')).toBe('其他')
  })

  it('roleLabel maps built-in role keys', () => {
    expect(roleLabel('admin')).toBe('管理员')
    expect(roleLabel('worker')).toBe('普通员工')
    expect(roleLabel('custom')).toBe('custom')
  })
})
