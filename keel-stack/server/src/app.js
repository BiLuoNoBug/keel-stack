import Koa from 'koa'
import cors from '@koa/cors'
import { koaBody } from 'koa-body'
import serve from 'koa-static'
import mount from 'koa-mount'
import { config } from './config/index.js'
import { errorHandler } from './middlewares/error.js'
import { deviceId } from './middlewares/auth.js'
import appRouter from './routes/app.js'
import adminRouter from './routes/admin.js'

/**
 * Koa 应用装配。
 * 中间件顺序：错误兜底 -> CORS -> body -> 静态资源 -> 设备标识 -> 健康检查 -> 业务路由
 */
export function createApp() {
  const app = new Koa()
  app.proxy = true

  app.use(errorHandler)
  app.use(cors({ origin: '*', credentials: false }))
  app.use(koaBody({ multipart: false, jsonLimit: '4mb', urlencoded: true }))

  // 上传的图片直接静态服务：/uploads/xxx.jpg
  app.use(mount('/uploads', serve(config.uploadDir, { maxage: 7 * 24 * 3600 * 1000, index: false })))

  app.use(deviceId)

  // 健康检查：部署后用来确认服务和端口是否活着
  app.use(async (ctx, next) => {
    if (ctx.path === '/api/health') {
      ctx.body = { code: 0, message: 'ok', data: { env: config.env, time: new Date().toISOString() } }
      return
    }
    await next()
  })

  app.use(appRouter.routes()).use(appRouter.allowedMethods())
  app.use(adminRouter.routes()).use(adminRouter.allowedMethods())

  return app
}
