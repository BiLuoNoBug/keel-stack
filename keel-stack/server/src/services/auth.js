import bcrypt from 'bcryptjs'
import { queryOne, execute } from '../db/pool.js'
import { signToken } from '../utils/jwt.js'
import { badRequest, unauthorized } from '../utils/response.js'

/** 后台管理员。骨架里就一个角色：admin，够用；要 RBAC 在这里加 role 字段。 */

export function toDTO(row) {
  return {
    id: row.id,
    username: row.username,
    nickname: row.nickname,
    avatarUrl: row.avatar_url,
    lastLoginAt: row.last_login_at,
    createdAt: row.created_at
  }
}

export async function login(username, password) {
  const row = await queryOne('SELECT * FROM admin_users WHERE username = ? LIMIT 1', [username])
  if (!row) throw unauthorized('账号或密码不对')
  const matched = await bcrypt.compare(password, row.password_hash)
  if (!matched) throw unauthorized('账号或密码不对')

  await execute('UPDATE admin_users SET last_login_at = NOW() WHERE id = ?', [row.id])
  const token = signToken({ id: row.id, username: row.username, scope: 'admin' })
  return { token, user: toDTO({ ...row, last_login_at: new Date() }) }
}

export async function getAdmin(id) {
  const row = await queryOne('SELECT * FROM admin_users WHERE id = ?', [id])
  if (!row) throw unauthorized()
  return toDTO(row)
}

export async function updateProfile(id, { nickname, avatarUrl }) {
  await execute('UPDATE admin_users SET nickname = ?, avatar_url = ? WHERE id = ?', [
    nickname ?? '', avatarUrl ?? null, id
  ])
  return getAdmin(id)
}

export async function changePassword(id, oldPassword, newPassword) {
  const row = await queryOne('SELECT * FROM admin_users WHERE id = ?', [id])
  if (!row) throw unauthorized()
  const matched = await bcrypt.compare(oldPassword, row.password_hash)
  if (!matched) throw badRequest('原密码不对')
  if (!newPassword || newPassword.length < 6) throw badRequest('新密码至少 6 位')
  const hash = await bcrypt.hash(newPassword, 10)
  await execute('UPDATE admin_users SET password_hash = ? WHERE id = ?', [hash, id])
}

/** 初始化脚本 / 种子数据用 */
export async function createAdmin({ username, password, nickname }) {
  const exists = await queryOne('SELECT id FROM admin_users WHERE username = ?', [username])
  if (exists) throw badRequest('这个账号已经存在了')
  const hash = await bcrypt.hash(password, 10)
  await execute('INSERT INTO admin_users (username, password_hash, nickname) VALUES (?,?,?)', [
    username, hash, nickname ?? username
  ])
}
