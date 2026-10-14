/**
 * 全站统一响应格式：{ code, message, data }
 * code = 0 表示成功，非 0 直接等于 HTTP 状态码。
 *
 * 前端只要在 axios 拦截器里判断 code，就不用每个接口各写一套解包逻辑。
 */
export function ok(ctx, data = null, message = 'ok') {
  ctx.status = 200
  ctx.body = { code: 0, message, data }
}

export function created(ctx, data = null, message = 'created') {
  ctx.status = 200
  ctx.body = { code: 0, message, data }
}

export function fail(ctx, message = '请求失败', status = 400, code = status) {
  ctx.status = status
  ctx.body = { code, message, data: null }
}

/** 业务异常，由 error 中间件统一转成响应 */
export class BizError extends Error {
  constructor(message, status = 400, code = status) {
    super(message)
    this.name = 'BizError'
    this.status = status
    this.code = code
  }
}

export const badRequest = (msg) => new BizError(msg, 400)
export const unauthorized = (msg = '登录已过期，请重新登录') => new BizError(msg, 401)
export const forbidden = (msg = '没有权限') => new BizError(msg, 403)
export const notFound = (msg = '资源不存在') => new BizError(msg, 404)
