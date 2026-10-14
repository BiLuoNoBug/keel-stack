import { query, queryOne, insert, execute } from '../db/pool.js'
import { notFound } from '../utils/response.js'
import { createStateMachine } from '../utils/status.js'

/**
 * 示例模块：item（条目）。
 *
 * ⚠️ 这块是「照着抄」的样板，和具体业务无关：
 *   表结构 -> toDTO -> list/get/create/update/状态流转/删除 -> 路由 -> 前端页面
 * 新增自己的业务模块时，把 item 换成你的领域名词，四处替换即可（见 docs/workflow.md）。
 */

export const ITEM_STATUS = createStateMachine({
  text: { TODO: '待处理', DOING: '进行中', DONE: '已完成' },
  transitions: {
    TODO: ['DOING', 'DONE'],
    DOING: ['DONE', 'TODO'],
    DONE: ['TODO']
  }
})

const FIELDS = 'id, title, category, content, cover_url, status, priority, published, sort_order, created_at, updated_at'

export function toDTO(row) {
  if (!row) return null
  return {
    id: row.id,
    title: row.title,
    category: row.category,
    content: row.content ?? '',
    coverUrl: row.cover_url ?? null,
    status: row.status,
    statusText: ITEM_STATUS.text(row.status),
    allowNext: ITEM_STATUS.allowed(row.status),
    priority: row.priority,
    published: !!row.published,
    sortOrder: row.sort_order,
    createdAt: row.created_at,
    updatedAt: row.updated_at
  }
}

/** 列表：分页 + 分类 / 关键字 / 状态筛选，控制台和 App 共用 */
export async function listItems({ category, keyword, status, published, page = 1, pageSize = 20 } = {}) {
  const where = []
  const params = []

  if (category) { where.push('category = ?'); params.push(category) }
  if (status) { where.push('status = ?'); params.push(status) }
  if (typeof published === 'boolean') { where.push('published = ?'); params.push(published ? 1 : 0) }
  if (keyword) {
    where.push('(title LIKE ? OR content LIKE ?)')
    params.push(`%${keyword}%`, `%${keyword}%`)
  }

  const cond = where.length ? `WHERE ${where.join(' AND ')}` : ''
  const total = Number((await queryOne(`SELECT COUNT(*) AS c FROM items ${cond}`, params))?.c ?? 0)
  const rows = await query(
    `SELECT ${FIELDS} FROM items ${cond} ORDER BY sort_order ASC, id DESC LIMIT ? OFFSET ?`,
    [...params, pageSize, (page - 1) * pageSize]
  )
  return { list: rows.map(toDTO), total, page, pageSize }
}

/** 分类枚举（给筛选下拉用） */
export async function listCategories() {
  const rows = await query('SELECT DISTINCT category FROM items ORDER BY category ASC')
  return rows.map((r) => r.category)
}

export async function getItem(id) {
  const row = await queryOne(`SELECT ${FIELDS} FROM items WHERE id = ?`, [id])
  if (!row) throw notFound('条目不存在')
  return toDTO(row)
}

export async function createItem(input) {
  const id = await insert(
    `INSERT INTO items (title, category, content, cover_url, priority, published, sort_order)
     VALUES (?,?,?,?,?,?,?)`,
    [
      input.title,
      input.category,
      input.content ?? '',
      input.coverUrl ?? null,
      input.priority ?? 'NORMAL',
      input.published === false ? 0 : 1,
      input.sortOrder ?? 0
    ]
  )
  return getItem(id)
}

export async function updateItem(id, input) {
  const current = await getItem(id)
  await execute(
    `UPDATE items SET title = ?, category = ?, content = ?, cover_url = ?, priority = ?, published = ?, sort_order = ?
     WHERE id = ?`,
    [
      input.title ?? current.title,
      input.category ?? current.category,
      input.content ?? current.content,
      input.coverUrl === undefined ? current.coverUrl : input.coverUrl,
      input.priority ?? current.priority,
      input.published === undefined ? (current.published ? 1 : 0) : (input.published ? 1 : 0),
      input.sortOrder ?? current.sortOrder,
      id
    ]
  )
  return getItem(id)
}

/** 状态流转：先对照状态机校验，不合法直接抛错 */
export async function updateStatus(id, next) {
  const current = await getItem(id)
  if (!next) throw new Error('缺少 status')
  ITEM_STATUS.assert(current.status, next)
  await execute('UPDATE items SET status = ? WHERE id = ?', [next, id])
  return getItem(id)
}

export async function togglePublished(id, published) {
  await getItem(id)
  await execute('UPDATE items SET published = ? WHERE id = ?', [published ? 1 : 0, id])
  return getItem(id)
}

export async function removeItem(id) {
  await getItem(id)
  await execute('DELETE FROM items WHERE id = ?', [id])
}

/** 控制台看板用：按状态计数 */
export async function countByStatus() {
  const rows = await query('SELECT status, COUNT(*) AS count FROM items GROUP BY status')
  const map = Object.fromEntries(rows.map((r) => [r.status, Number(r.count)]))
  return ITEM_STATUS.keys.map((status) => ({ status, statusText: ITEM_STATUS.text(status), count: map[status] ?? 0 }))
}
