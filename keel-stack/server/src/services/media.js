import fs from 'node:fs/promises'
import path from 'node:path'
import { query, queryOne, insert, execute } from '../db/pool.js'
import { config } from '../config/index.js'
import { notFound, badRequest } from '../utils/response.js'

/**
 * 图片媒体库。
 *
 * 「先入库再引用」的通用附件方案：图片的物理文件在 uploads/，
 * 元数据在 media 表，ref_count 记录被业务表引用了几次，>0 时不允许删除。
 *
 * 新增业务表时，只要它也存了图片 URL，就把它加进下面的引用统计里。
 */

export function toDTO(row) {
  return {
    id: row.id,
    filename: row.filename,
    url: row.url,
    mime: row.mime,
    size: row.size,
    refCount: row.ref_count,
    createdAt: row.created_at
  }
}

export async function listMedia({ keyword, page = 1, pageSize = 60 } = {}) {
  const where = []
  const params = []
  if (keyword) { where.push('filename LIKE ?'); params.push(`%${keyword}%`) }
  const cond = where.length ? `WHERE ${where.join(' AND ')}` : ''
  const total = Number((await queryOne(`SELECT COUNT(*) AS c FROM media ${cond}`, params))?.c ?? 0)
  const rows = await query(
    `SELECT * FROM media ${cond} ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?`,
    [...params, pageSize, (page - 1) * pageSize]
  )
  return { list: rows.map(toDTO), total, page, pageSize }
}

/** 上传后登记；同一个 url 重复上传时直接返回已有记录 */
export async function registerMedia({ filename, url, mime, size }) {
  const existing = await queryOne('SELECT * FROM media WHERE url = ?', [url])
  if (existing) return toDTO(existing)
  const id = await insert(
    'INSERT INTO media (filename, url, mime, size) VALUES (?,?,?,?)',
    [filename, url, mime ?? '', size ?? 0]
  )
  const row = await queryOne('SELECT * FROM media WHERE id = ?', [id])
  return toDTO(row)
}

export async function getMedia(id) {
  const row = await queryOne('SELECT * FROM media WHERE id = ?', [id])
  if (!row) throw notFound('图片不存在')
  return toDTO(row)
}

/** 单张图的引用数：被业务表的封面字段引用了几次 */
export async function refreshRefCount(id) {
  const row = await queryOne('SELECT url FROM media WHERE id = ?', [id])
  if (!row) return 0
  const itemRefs = Number((await queryOne('SELECT COUNT(*) AS c FROM items WHERE cover_url = ?', [row.url]))?.c ?? 0)
  await execute('UPDATE media SET ref_count = ? WHERE id = ?', [itemRefs, id])
  return itemRefs
}

/** 全量重算（新增业务表后调一次，或上传后顺手调） */
export async function refreshAllRefCounts() {
  await execute(`UPDATE media m SET ref_count = (
      (SELECT COUNT(*) FROM items i WHERE i.cover_url = m.url)
    )`)
}

export async function removeMedia(id) {
  const media = await getMedia(id)
  const refs = await refreshRefCount(id)
  if (refs > 0) throw badRequest(`这张图还被 ${refs} 处引用着，先换掉再删`)
  const abs = path.join(config.uploadDir, path.basename(media.url))
  await fs.rm(abs, { force: true })
  await execute('DELETE FROM media WHERE id = ?', [id])
}
