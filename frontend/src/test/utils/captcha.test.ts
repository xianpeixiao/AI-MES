import { describe, expect, it } from 'vitest'
import { toCaptchaDataUrl } from '@/utils/captcha'

describe('captcha', () => {
  it('returns empty string when image missing', () => {
    expect(toCaptchaDataUrl()).toBe('')
  })

  it('keeps existing data url unchanged', () => {
    const dataUrl = 'data:image/png;base64,abc'
    expect(toCaptchaDataUrl(dataUrl)).toBe(dataUrl)
  })

  it('wraps raw base64 into png data url', () => {
    expect(toCaptchaDataUrl('abc123')).toBe('data:image/png;base64,abc123')
  })
})
