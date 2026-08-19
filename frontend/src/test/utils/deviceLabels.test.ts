import { describe, expect, it } from 'vitest'
import {
  deviceActionLabel,
  deviceStatusLabel,
  deviceStatusTagType
} from '@/utils/deviceLabels'

describe('deviceLabels', () => {
  it('deviceStatusLabel falls back to idle label', () => {
    expect(deviceStatusLabel('running')).toBe('运行中')
    expect(deviceStatusLabel('fault')).toBe('故障')
    expect(deviceStatusLabel(undefined)).toBe('空闲')
  })

  it('deviceStatusTagType maps severity tags', () => {
    expect(deviceStatusTagType('running')).toBe('success')
    expect(deviceStatusTagType('fault')).toBe('danger')
    expect(deviceStatusTagType(undefined)).toBe('info')
  })

  it('deviceActionLabel maps device history actions', () => {
    expect(deviceActionLabel('repair')).toBe('设备维修')
    expect(deviceActionLabel('inspection')).toBe('设备点检')
    expect(deviceActionLabel('unknown')).toBe('unknown')
  })
})
