import { describe, expect, it } from 'vitest'
import { getChatQuickQuestions } from '@/utils/chatQuickQuestions'

describe('getChatQuickQuestions', () => {
  it('returns admin questions when fullAccess is true', () => {
    const questions = getChatQuickQuestions('', [], true)
    expect(questions[0]).toBe('AI-MES 是什么系统？')
    expect(questions.length).toBeLessThanOrEqual(4)
  })

  it('filters admin questions by permissions when not fullAccess', () => {
    const questions = getChatQuickQuestions('admin', ['物料'], false)
    expect(questions.some((q) => q.includes('缺料'))).toBe(true)
    expect(questions.some((q) => q.includes('设备'))).toBe(false)
  })

  it('builds supervisor questions with team name', () => {
    const questions = getChatQuickQuestions('supervisor', ['工单管理'], false, '焊接班')
    expect(questions.some((q) => q.includes('焊接班'))).toBe(true)
  })

  it('builds worker questions with default team label', () => {
    const questions = getChatQuickQuestions('worker', ['工序进度'], false)
    expect(questions.some((q) => q.includes('本班组'))).toBe(true)
  })

  it('returns unrestricted questions when user lacks specific permissions', () => {
    const questions = getChatQuickQuestions('worker', [], false, 'A班')
    expect(questions).toEqual(['AI-MES 是什么系统？'])
  })
})
