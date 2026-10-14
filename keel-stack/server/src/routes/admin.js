import Router from '@koa/router'
import { config } from '../config/index.js'
import { requireAdmin } from '../middlewares/auth.js'
import { upload } from '../middlewares/upload.js'
import { validateBody, itemInput, loginInput, notificationInput } from '../middlewares/validate.js'
import { badRequest, ok } from '../utils/response.js'
import * as authService from '../services/auth.js'
import * as itemService from '../services/item.js'
import * as mediaService from '../services/media.js'
import * as notificationService from '../services/notification.js'
import * as releaseService from '../services/release.js'
import { emitItemChanged, emitItemStatus, emitNotification, emitRelease } from '../socket/index.js'

/**
 * 后台控制台接口。
 *
 * 除 /login 外，全部经过 requireAdmin（JWT）；新增模块时照抄下面 item 那一组即可。
 */
const router = new Router({ prefix: '/api/admin' })

/* ------------------------------------------------------------ 登录（公开） */
router.post('/login', validateBody(loginInput), async (ctx) => {
  const { username, password } = ctx.request.body
  ok(ctx, await authService.login(username, password), '登录成功')
})

// 这一行之后的接口全部需要 JWT
router.use(requireAdmin)

/* ------------------------------------------------------------ 当前账号 */
router.get('/profile', async (ctx) => {
  ok(ctx, await authService.getAdmin(ctx.state.admin.id))
})

router.put('/profile', async (ctx) => {
  const { nickname, avatarUrl } = ctx.request.body ?? {}
  ok(ctx, await authService.updateProfile(ctx.state.admin.id, { nickname, avatarUrl }), '已保存')
})

router.post('/password', async (ctx) => {
  const { oldPassword, newPassword } = ctx.request.body ?? {}
  await authService.changePassword(ctx.state.admin.id, oldPassword, newPassword)
  ok(ctx, null, '密码已修改')
})

/* ------------------------------------------------------------ 图片上传 / 媒体库 */
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

router.get('/media', async (ctx) => {
  ok(ctx, await mediaService.listMedia({
    keyword: ctx.query.keyword,
    page: Number(ctx.query.page || 1),
    pageSize: Number(ctx.query.pageSize || 60)
  }))
})

router.delete('/media/:id', async (ctx) => {
  await mediaService.removeMedia(ctx.params.id)
  ok(ctx, null, '已删除')
})

/* ------------------------------------------------------------ item 示例模块 */
router.get('/items', async (ctx) => {
  const { category, keyword, status, published } = ctx.query
  ok(ctx, await itemService.listItems({
    category,
    keyword,
    status,
    published: published === undefined || published === '' ? undefined : published === 'true',
    page: Number(ctx.query.page || 1),
    pageSize: Number(ctx.query.pageSize || 20)
  }))
})

router.post('/items', validateBody(itemInput), async (ctx) => {
  const item = await itemService.createItem(ctx.request.body)
  emitItemChanged({ action: 'created', item })
  ok(ctx, item, '已添加')
})

router.put('/items/:id', validateBody(itemInput), async (ctx) => {
  const item = await itemService.updateItem(ctx.params.id, ctx.request.body)
  emitItemChanged({ action: 'updated', item })
  ok(ctx, item, '已保存')
})

router.patch('/items/:id/status', async (ctx) => {
  const item = await itemService.updateStatus(ctx.params.id, ctx.request.body?.status)
  emitItemStatus(item)
  ok(ctx, item, '状态已更新')
})

router.patch('/items/:id/publish', async (ctx) => {
  const published = ctx.request.body?.published !== false
  const item = await itemService.togglePublished(ctx.params.id, published)
  emitItemChanged({ action: published ? 'on' : 'off', item })
  ok(ctx, item, published ? '已发布' : '已下架')
})

router.delete('/items/:id', async (ctx) => {
  await itemService.removeItem(ctx.params.id)
  emitItemChanged({ action: 'deleted', itemId: Number(ctx.params.id) })
  ok(ctx, null, '已删除')
})

/* ------------------------------------------------------------ 消息推送 */
router.get('/notifications', async (ctx) => {
  const pushed = ctx.query.pushed
  ok(ctx, {
    list: await notificationService.listAll({
      pushed: pushed === undefined || pushed === '' ? undefined : pushed === 'true'
    })
  })
})

/** 新建消息；默认直接推送，传 push:false 则只存草稿 */
router.post('/notifications', validateBody(notificationInput), async (ctx) => {
  const { push, ...rest } = ctx.request.body ?? {}
  const created = await notificationService.createNotification(rest)
  if (push === false) {
    ok(ctx, created, '已保存为草稿')
    return
  }
  const pushed = await notificationService.markPushed(created.id)
  emitNotification(pushed)
  ok(ctx, pushed, '已推送')
})

router.put('/notifications/:id', async (ctx) => {
  ok(ctx, await notificationService.updateNotification(ctx.params.id, ctx.request.body ?? {}), '已保存')
})

/** 重推（比如客户端当时不在线） */
router.post('/notifications/:id/push', async (ctx) => {
  const pushed = await notificationService.markPushed(ctx.params.id)
  emitNotification(pushed)
  ok(ctx, pushed, '已重新推送')
})

router.delete('/notifications/:id', async (ctx) => {
  await notificationService.removeNotification(ctx.params.id)
  ok(ctx, null, '已删除')
})

/* ------------------------------------------------------------ 版本发布 */
router.get('/releases', async (ctx) => {
  ok(ctx, { list: await releaseService.listReleases() })
})

router.post('/releases', async (ctx) => {
  const release = await releaseService.createRelease(ctx.request.body ?? {})
  if (release.published) emitRelease(release)
  ok(ctx, release, release.published ? '版本已发布，App 会收到更新提醒' : '已存为未发布版本')
})

router.put('/releases/:id', async (ctx) => {
  ok(ctx, await releaseService.updateRelease(ctx.params.id, ctx.request.body ?? {}), '已保存')
})

router.post('/releases/:id/publish', async (ctx) => {
  const release = await releaseService.publishRelease(ctx.params.id)
  emitRelease(release)
  ok(ctx, release, '已上线')
})

router.delete('/releases/:id', async (ctx) => {
  await releaseService.removeRelease(ctx.params.id)
  ok(ctx, null, '已删除')
})

/* ------------------------------------------------------------ 概览统计 */

/** 看板用的轻量聚合：计数类查询都放这里，避免控制台拼多个请求 */
router.get('/stats', async (ctx) => {
  const [byStatus, media, notifications, releases] = await Promise.all([
    itemService.countByStatus(),
    mediaService.listMedia({ pageSize: 1 }),
    notificationService.countAll(),
    releaseService.listReleases()
  ])
  ok(ctx, {
    itemTotal: byStatus.reduce((sum, s) => sum + s.count, 0),
    byStatus,
    mediaTotal: media.total,
    notificationTotal: notifications,
    latestRelease: releases[0] ?? null
  })
})

export default router
