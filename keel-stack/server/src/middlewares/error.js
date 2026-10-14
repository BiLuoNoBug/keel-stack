import { BizError, fail } from '../utils/response.js'
import { ZodError } from 'zod'

/**
 * 全局错误处理 —— 放在最外层，所有异常都在这里落地成统一响应。
 * 顺序：业务异常 -> 参数校验异常 -> 401 -> 兜底 500。
 */
export async function errorHandler(ctx, next) {
  try {
    await next()
    // 路由没命中
    if (ctx.status === 404 && !ctx.body) fail(ctx, `接口不存在：${ctx.method} ${ctx.path}`, 404)
  } catch (err) {
    if (err instanceof BizError) {
      fail(ctx, err.message, err.status, err.code)
      return
    }
    if (err instanceof ZodError) {
      const first = err.errors[0]
      fail(ctx, `参数错误：${first?.path?.join('.') || ''} ${first?.message || ''}`.trim(), 400)
      return
    }
    if (err?.status === 401) {
      fail(ctx, err.message || '未授权', 401)
      return
    }
    console.error('[error]', ctx.method, ctx.path, err)
    fail(ctx, err?.message || '服务器内部错误', 500)
  }
}

/** 包裹 async 中间件，异常统一交给 errorHandler */
export const asyncHandler = (fn) => (ctx, next) =>
  Promise.resolve(fn(ctx, next)).catch((err) => {
    throw err
  })
