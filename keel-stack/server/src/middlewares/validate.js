import { z } from 'zod'

/**
 * 参数校验。
 * 校验失败抛 ZodError，由 error 中间件统一转成 400，路由里不用写 try/catch。
 */

/** 校验请求体，解析后的结果覆盖 ctx.request.body（多余字段会被剥掉） */
export const validateBody = (schema) => async (ctx, next) => {
  ctx.request.body = schema.parse(ctx.request.body ?? {})
  await next()
}

/** 校验 query，结果放到 ctx.state.query */
export const validateQuery = (schema) => async (ctx, next) => {
  ctx.state.query = schema.parse(ctx.query ?? {})
  await next()
}

/* ------------------------------------------------------------ 通用片段 */

export const idParam = z.coerce.number().int().positive()

export const pagination = z.object({
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(20)
})

/* ------------------------------------------------------------ 各业务 schema */

/**
 * 示例模块：item（条目）。
 * 只是演示「输入校验 + 默认值」怎么写，换成自己的业务时整块替换即可。
 */
export const itemInput = z.object({
  title: z.string().trim().min(1, '标题不能为空').max(64),
  category: z.string().trim().min(1).max(32).default('默认分类'),
  content: z.string().trim().max(1000).default(''),
  coverUrl: z.string().trim().max(255).nullish(),
  priority: z.enum(['LOW', 'NORMAL', 'HIGH']).default('NORMAL'),
  published: z.coerce.boolean().default(true),
  sortOrder: z.coerce.number().int().default(0)
})

export const loginInput = z.object({
  username: z.string().trim().min(1, '请输入账号'),
  password: z.string().min(1, '请输入密码')
})

export const notificationInput = z.object({
  title: z.string().trim().min(1, '标题不能为空').max(32),
  content: z.string().trim().min(1, '内容不能为空').max(200),
  level: z.enum(['INFO', 'IMPORTANT']).default('INFO'),
  target: z.enum(['ALL', 'CLIENT', 'ADMIN']).default('ALL'),
  // 控制台用来区分「立即推送 / 存草稿」；zod 默认会剥离未声明字段，所以必须显式列出
  push: z.coerce.boolean().optional()
})
