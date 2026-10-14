import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import { runRaw } from './pool.js'
import { sqlDir } from './paths.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

/**
 * 重置数据库：DROP 掉 sql/ 里声明过的所有表，再重新建表 + 灌种子。
 * 只在本机开发用，别对着有数据的库跑：npm run db:reset
 */

async function dropAll() {
  const files = (await fs.readdir(sqlDir)).filter((f) => f.endsWith('.sql')).sort()
  const tables = new Set()
  for (const file of files) {
    const sql = await fs.readFile(path.join(sqlDir, file), 'utf8')
    for (const m of sql.matchAll(/CREATE TABLE IF NOT EXISTS\s+`?(\w+)`?/gi)) tables.add(m[1])
  }
  if (!tables.size) return
  const stmts = ['SET FOREIGN_KEY_CHECKS = 0']
  for (const t of tables) stmts.push(`DROP TABLE IF EXISTS \`${t}\``)
  stmts.push('SET FOREIGN_KEY_CHECKS = 1')
  await runRaw(stmts.join(';'))
  console.log(`[reset] 已删除 ${tables.size} 张表：${[...tables].join(', ')}`)
}

async function main() {
  await dropAll()
  for (const script of ['migrate.js', 'seed.js']) {
    console.log(`[reset] 执行 ${script}`)
    const r = spawnSync(process.execPath, [path.resolve(__dirname, script)], { stdio: 'inherit' })
    if (r.status !== 0) process.exit(r.status ?? 1)
  }
  console.log('[reset] 完成')
}

main().catch((err) => {
  console.error('[reset] 失败：', err.message)
  process.exit(1)
})
