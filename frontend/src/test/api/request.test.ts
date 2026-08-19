import { describe, expect, it } from 'vitest'
import type { AxiosError } from 'axios'
import {
  createAbortError,
  createApiError,
  isAbortError,
  isNetworkFailure,
  normalizeRequestError,
  resolveErrorMessage,
  resolveErrorStatus,
  type ApiResult
} from '@/api/request'

describe('request error helpers', () => {
  it('resolveErrorStatus prefers apiCode then httpStatus', () => {
    expect(resolveErrorStatus(createApiError('x', 403, 500))).toBe(403)
    expect(resolveErrorStatus({ response: { status: 404, data: { code: 4001 } } })).toBe(404)
    expect(resolveErrorStatus({ response: { data: { code: 4001 } } })).toBe(4001)
  })

  it('isNetworkFailure detects missing response or network codes', () => {
    expect(isNetworkFailure({ code: 'ERR_NETWORK' })).toBe(true)
    expect(isNetworkFailure({ response: { status: 500 } })).toBe(false)
    expect(isNetworkFailure({})).toBe(true)
  })

  it('isAbortError recognizes canceled requests', () => {
    expect(isAbortError(createAbortError())).toBe(true)
    expect(isAbortError({ name: 'AbortError' })).toBe(true)
    expect(isAbortError(createApiError('fail'))).toBe(false)
  })

  it('resolveErrorMessage returns fallback for aborts', () => {
    expect(resolveErrorMessage(createAbortError(), '已取消')).toBe('已取消')
    expect(resolveErrorMessage(createApiError('权限不足'), '操作失败')).toBe('权限不足')
  })

  it('normalizeRequestError maps business message from response body', () => {
    const error = {
      response: {
        status: 400,
        data: { code: 4001, message: '参数校验失败' }
      },
      message: 'Request failed with status code 400'
    } as AxiosError<ApiResult>

    const normalized = normalizeRequestError(error)
    expect(normalized.message).toBe('参数校验失败')
    expect(normalized.apiCode).toBe(4001)
    expect(normalized.httpStatus).toBe(400)
  })

  it('normalizeRequestError maps network and timeout messages', () => {
    const network = normalizeRequestError({ message: 'Network Error' } as AxiosError<ApiResult>)
    expect(network.message).toContain('网络连接失败')

    const timeout = normalizeRequestError({
      code: 'ECONNABORTED',
      message: 'timeout of 30000ms exceeded'
    } as AxiosError<ApiResult>)
    expect(timeout.message).toContain('请求超时')
  })

  it('normalizeRequestError maps HTTP status fallbacks', () => {
    const forbidden = normalizeRequestError({
      response: { status: 403 },
      message: 'Request failed with status code 403'
    } as AxiosError<ApiResult>)
    expect(forbidden.message).toContain('403')

    const server = normalizeRequestError({
      response: { status: 500 },
      message: 'Request failed with status code 500'
    } as AxiosError<ApiResult>)
    expect(server.message).toContain('500')
  })
})
