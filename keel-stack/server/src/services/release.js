import { query, queryOne, insert, execute } from '../db/pool.js'
import { badRequest, notFound } from '../utils/response.js'

/**
 * App 远程版本升级。
 *
 * 控制台发布一条版本记录 -> Socket.IO 推 release:new -> App 收到后主动查
 * /api/app/releases/latest，有新版就下载 apk 并拉起系统安装器。
 * 与业务无关，属于 App 的基础设施，可以原样保留。
 */

export function toDTO(row) {
  if (!row) return null
  return {
    id: row.id,
    versionCode: row.version_code,
    versionName: row.version_name,
    changelog: row.changelog,
    downloadUrl: row.download_url,
    forceUpdate: !!row.force_update,
    published: !!row.published,
    createdAt: row.created_at,
    updatedAt: row.updated_at
  }
}

export async function listReleases() {
  const rows = await query('SELECT * FROM app_releases ORDER BY version_code DESC')
  return rows.map(toDTO)
}

export async function getRelease(id) {
  const row = await queryOne('SELECT * FROM app_releases WHERE id = ?', [id])
  if (!row) throw notFound('版本不存在')
  return toDTO(row)
}

/** App 查更新：只返回比自己新、且已发布的最高版本 */
export async function checkUpdate(currentCode) {
  const code = Number(currentCode) || 0
  const row = await queryOne(
    'SELECT * FROM app_releases WHERE published = 1 AND version_code > ? ORDER BY version_code DESC LIMIT 1',
    [code]
  )
  const latest = await queryOne(
    'SELECT * FROM app_releases WHERE published = 1 ORDER BY version_code DESC LIMIT 1'
  )
  return {
    hasUpdate: !!row,
    currentVersionCode: code,
    latest: latest ? toDTO(latest) : null,
    release: row ? toDTO(row) : null
  }
}

export async function createRelease(input) {
  if (input.versionCode == null) throw badRequest('缺少 versionCode')
  const exists = await queryOne('SELECT id FROM app_releases WHERE version_code = ?', [input.versionCode])
  if (exists) throw badRequest(`版本号 ${input.versionCode} 已经存在了`)
  const id = await insert(
    `INSERT INTO app_releases (version_code, version_name, changelog, download_url, force_update, published)
     VALUES (?,?,?,?,?,?)`,
    [
      input.versionCode,
      input.versionName ?? '',
      input.changelog ?? '',
      input.downloadUrl ?? '',
      input.forceUpdate ? 1 : 0,
      input.published === false ? 0 : 1
    ]
  )
  return getRelease(id)
}

export async function updateRelease(id, input) {
  const current = await getRelease(id)
  if (input.versionCode != null && input.versionCode !== current.versionCode) {
    const dup = await queryOne('SELECT id FROM app_releases WHERE version_code = ? AND id <> ?', [
      input.versionCode, id
    ])
    if (dup) throw badRequest(`版本号 ${input.versionCode} 已经被占用`)
  }
  await execute(
    `UPDATE app_releases SET version_code = ?, version_name = ?, changelog = ?, download_url = ?, force_update = ?, published = ?
     WHERE id = ?`,
    [
      input.versionCode ?? current.versionCode,
      input.versionName ?? current.versionName,
      input.changelog ?? current.changelog,
      input.downloadUrl ?? current.downloadUrl,
      input.forceUpdate === undefined ? (current.forceUpdate ? 1 : 0) : (input.forceUpdate ? 1 : 0),
      input.published === undefined ? (current.published ? 1 : 0) : (input.published ? 1 : 0),
      id
    ]
  )
  return getRelease(id)
}

export async function removeRelease(id) {
  await getRelease(id)
  await execute('DELETE FROM app_releases WHERE id = ?', [id])
}

export async function publishRelease(id) {
  await getRelease(id)
  await execute('UPDATE app_releases SET published = 1 WHERE id = ?', [id])
  return getRelease(id)
}
