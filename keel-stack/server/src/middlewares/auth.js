import { readBearer, verifyToken } from '../utils/jwt.js'
import { forbidden, unauthorized } from '../utils/response.js'

/** 后台控制台鉴权：必须带合法 JWT，且 scope 为 admin */
export async function requireAdmin(ctx, next) {
  const token = readBearer(ctx)
  if (!token) throw unauthorized('请先登录')
  const payload = verifyToken(token)
  if (payload.scope !== 'admin') throw forbidden('该账号无权访问后台')
  ctx.state.admin = payload
  await next()
}

/** 可选鉴权：带上就解析，不带也放行 */
export async function optionalAuth(ctx, next) {
  const token = readBearer(ctx)
  if (token) {
    try {
      ctx.state.admin = verifyToken(token)
    } catch {
      /* 匿名访问，忽略非法 token */
    }
  }
  await next()
}

/**
 * 客户端身份。
 *
 * 骨架里没有做登录，用 x-device-id 头区分设备（默认 default）。
 * 换成真正的账号体系时，把这里替换成解析 token 即可，后续路由不用动。
 */
export async function deviceId(ctx, next) {
  ctx.state.deviceId = ctx.get('x-device-id') || 'default'
  await next()
}
