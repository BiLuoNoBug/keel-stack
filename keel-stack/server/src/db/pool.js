import mysql from 'mysql2/promise'
import { config } from '../config/index.js'

export const pool = mysql.createPool({
  ...config.mysql,
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
  charset: 'utf8mb4_unicode_ci',
  timezone: '+08:00',
  // 让 DATETIME 原样返回字符串、DECIMAL 返回 number，前端不用再处理
  dateStrings: true,
  decimalNumbers: true
})

/** 查询多行 */
export async function query(sql, params = []) {
  const [rows] = await pool.query(sql, params)
  return rows
}

/** 查询单行，无结果返回 null */
export async function queryOne(sql, params = []) {
  const rows = await query(sql, params)
  return rows[0] ?? null
}

/** 插入并返回自增 id */
export async function insert(sql, params = []) {
  const [result] = await pool.query(sql, params)
  return result.insertId
}

/** 更新/删除，返回受影响行数 */
export async function execute(sql, params = []) {
  const [result] = await pool.query(sql, params)
  return result.affectedRows
}

/** 事务包装：handler 里抛错会自动回滚 */
export async function transaction(handler) {
  const conn = await pool.getConnection()
  try {
    await conn.beginTransaction()
    const result = await handler(conn)
    await conn.commit()
    return result
  } catch (err) {
    await conn.rollback()
    throw err
  } finally {
    conn.release()
  }
}

export async function ping() {
  const conn = await pool.getConnection()
  try {
    await conn.ping()
  } finally {
    conn.release()
  }
}

/** 建表脚本用：一次执行多条语句（多语句模式只在迁移时开） */
export async function runRaw(sql) {
  const conn = await mysql.createConnection({ ...config.mysql, multipleStatements: true })
  try {
    await conn.query(sql)
  } finally {
    await conn.end()
  }
}
