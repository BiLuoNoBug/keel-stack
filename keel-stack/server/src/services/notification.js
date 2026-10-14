import { query, queryOne, insert, execute } from '../db/pool.js'
import { notFound } from '../utils/response.js'

/**
 * 站内消息。
 *
 * 控制台（或 App）写一条，markPushed 之后通过 Socket.IO 推给目标房间，
 * 客户端收到 notification:new 就弹顶部横幅 —— 这就是「服务器主动喊一声」的通道。
 *
 * 字段里的 link 是给客户端用的跳转标识（例如某个业务对象的 id），骨架里不绑定任何业务。
 */

const VALID_LEVEL = ['INFO', 'IMPORTANT']
const VALID_TARGET = ['ALL', 'CLIENT', 'ADMIN']

export function toDTO(row) {
  if (!row) return null
  return {
    id: row.id,
    title: row.title,
    content: row.content,
    link: row.link ?? '',
    level: row.level,
    target: row.target,
    pushed: !!row.pushed,
    pushedAt: row.pushed_at,
    pushCount: row.push_count,
    createdAt: row.created_at
  }
}

/** 客户端拉消息：按角色过滤，广播的（ALL）永远可见 */
export async function listNotifications({ target, limit = 50 } = {}) {
  const where = []
  const params = []
  if (target) {
    where.push("(target = 'ALL' OR target = ?)")
    params.push(target)
  }
  const cond = where.length ? `WHERE ${where.join(' AND ')}` : ''
  const rows = await query(
    `SELECT * FROM notifications ${cond} ORDER BY created_at DESC, id DESC LIMIT ?`,
    [...params, limit]
  )
  return rows.map(toDTO)
}

/** 控制台拉全量，可按推送状态筛 */
export async function listAll({ pushed, limit = 100 } = {}) {
  const where = []
  const params = []
  if (pushed === true) where.push('pushed = 1')
  if (pushed === false) where.push('pushed = 0')
  const cond = where.length ? `WHERE ${where.join(' AND ')}` : ''
  const rows = await query(
    `SELECT * FROM notifications ${cond} ORDER BY created_at DESC, id DESC LIMIT ?`,
    [...params, limit]
  )
  return rows.map(toDTO)
}

/** 计数（看板用） */
export async function countAll() {
  return Number((await queryOne('SELECT COUNT(*) AS c FROM notifications'))?.c ?? 0)
}

export async function getNotification(id) {
  const row = await queryOne('SELECT * FROM notifications WHERE id = ?', [id])
  if (!row) throw notFound('消息不存在')
  return toDTO(row)
}

export async function createNotification(input) {
  const level = VALID_LEVEL.includes(input.level) ? input.level : 'INFO'
  const target = VALID_TARGET.includes(input.target) ? input.target : 'ALL'
  const id = await insert(
    'INSERT INTO notifications (title, content, link, level, target) VALUES (?,?,?,?,?)',
    [input.title, input.content, input.link ?? null, level, target]
  )
  return getNotification(id)
}

export async function updateNotification(id, input) {
  const current = await getNotification(id)
  const level = VALID_LEVEL.includes(input.level) ? input.level : current.level
  const target = VALID_TARGET.includes(input.target) ? input.target : current.target
  await execute(
    'UPDATE notifications SET title = ?, content = ?, level = ?, target = ? WHERE id = ?',
    [input.title ?? current.title, input.content ?? current.content, level, target, id]
  )
  return getNotification(id)
}

/** 记录一次推送；重推时 push_count 会累加 */
export async function markPushed(id) {
  await getNotification(id)
  await execute(
    'UPDATE notifications SET pushed = 1, pushed_at = NOW(), push_count = push_count + 1 WHERE id = ?',
    [id]
  )
  return getNotification(id)
}

export async function removeNotification(id) {
  await getNotification(id)
  await execute('DELETE FROM notifications WHERE id = ?', [id])
}
