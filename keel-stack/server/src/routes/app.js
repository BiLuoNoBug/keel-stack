import Router from '@koa/router'
import { config } from '../config/index.js'
import * as itemService from '../services/item.js'
import * as mediaService from '../services/media.js'
import * as notificationService from '../services/notification.js'
import * as releaseService from '../services/release.js'
import { upload } from '../middlewares/upload.js'
import { badRequest, ok } from '../utils/response.js'
import { itemInput, notificationInput, validateBody } from '../middlewares/validate.js'
import { emitItemChanged, emitItemStatus, emitNotification } from '../socket/index.js'

/**
 * 客户端接口（App 用）。
 *
 * 骨架里不做登录，用 x-device-id 区分设备（见 middlewares/auth.js 的 deviceId）。
 * 新增业务模块时，在这里挂一组 RESTful 路由，写操作顺手 emit 一个事件。
 */
const router = new Router({ prefix: '/api/app' })

/* ------------------------------------------------------------ item 示例模块 */

router.get('/items', async (ctx) => {
  const { category, keyword, status, published } = ctx.query
  ok(ctx, await itemService.listItems({
    category,
    keyword,
    status,
    published: published === undefined || published === '' ? undefined : published === 'true',
    page: Number(ctx.query.page || 1),
    pageSize: Number(ctx.query.pageSize || 50)
  }))
})

router.get('/items/categories', async (ctx) => {
  ok(ctx, await itemService.listCategories())
})

// 注意：更具体的路径要放在 /items/:id 之前，否则会被当成 id
router.get('/items/status/count', async (ctx) => {
  ok(ctx, await itemService.countByStatus())
})

router.get('/items/:id', async (ctx) => {
  ok(ctx, await itemService.getItem(ctx.params.id))
})

router.post('/items', validateBody(itemInput), async (ctx) => {
  const item = await itemService.createItem(ctx.request.body)
  // 有人新增，通知另一端的列表刷新
  emitItemChanged({ action: 'created', item })
  await notifyCreated(item)
  ok(ctx, item, '已创建')
})

router.put('/items/:id', validateBody(itemInput), async (ctx) => {
  const item = await itemService.updateItem(ctx.params.id, ctx.request.body)
  emitItemChanged({ action: 'updated', item })
  ok(ctx, item, '已保存')
})

router.patch('/items/:id/status', async (ctx) => {
  const item = await itemService.updateStatus(ctx.params.id, ctx.request.body?.status)
  emitItemStatus(item)
  await notifyStatus(item)
  ok(ctx, item, '状态已更新')
})

router.delete('/items/:id', async (ctx) => {
  await itemService.removeItem(ctx.params.id)
  emitItemChanged({ action: 'deleted', itemId: Number(ctx.params.id) })
  ok(ctx, null, '已删除')
})

/** 新增后给控制台留一条站内消息，控制台右上角会弹出提醒 */
async function notifyCreated(item) {
  const created = await notificationService.createNotification({
    title: '有新的条目',
    content: `${item.title} · ${item.category}`,
    link: String(item.id),
    level: 'INFO',
    target: 'ADMIN'
  })
  emitNotification(await notificationService.markPushed(created.id))
}

/** 状态推进到「已完成」时给客户端推一条消息 */
async function notifyStatus(item) {
  if (item.status !== 'DONE') return
  const created = await notificationService.createNotification({
    title: '条目已完成',
    content: `${item.title} 已收尾`,
    link: String(item.id),
    level: 'INFO',
    target: 'CLIENT'
  })
  emitNotification(await notificationService.markPushed(created.id))
}

/* ------------------------------------------------------------ 站内消息 */

router.get('/notifications', async (ctx) => {
  const target = String(ctx.query.target || '').toUpperCase() === 'ADMIN' ? 'ADMIN' : 'CLIENT'
  ok(ctx, { list: await notificationService.listNotifications({ target }) })
})

router.post('/notifications', validateBody(notificationInput), async (ctx) => {
  const created = await notificationService.createNotification(ctx.request.body)
  const pushed = await notificationService.markPushed(created.id)
  emitNotification(pushed)
  ok(ctx, pushed, '已推送')
})

/* ------------------------------------------------------------ 版本更新 */

/** App 启动 / 手动检查更新时调这里 */
router.get('/releases/latest', async (ctx) => {
  ok(ctx, await releaseService.checkUpdate(ctx.query.versionCode))
})

/* ------------------------------------------------------------ 图片上传 */

router.post('/upload', upload.single('file'), async (ctx) => {
  const file = ctx.file
  if (!file) throw badRequest('没有收到文件')
  const url = `/uploads/${file.filename}`
  const media = await mediaService.registerMedia({
    filename: file.originalname, url, mime: file.mimetype, size: file.size
  })
  await mediaService.refreshAllRefCounts()
  ok(ctx, { ...media, fullUrl: `${config.publicBaseUrl}${url}` }, '上传成功')
})

export default router
