import fs from 'node:fs/promises'
import path from 'node:path'
import { runRaw } from './pool.js'
import { config } from '../config/index.js'
import { sqlDir } from './paths.js'

/**
 * 建表：按文件名顺序执行 sql/ 下的所有 .sql。
 *
 * 容器首次启动也会自动跑一遍（docker-compose 把 sql/ 挂到了 initdb.d），
 * 这里主要是给「本机已有 MySQL / 想手动重建」的场景用：npm run db:migrate
 */
async function migrate() {
  const files = (await fs.readdir(sqlDir)).filter((f) => f.endsWith('.sql')).sort()
  for (const file of files) {
    const sql = await fs.readFile(path.join(sqlDir, file), 'utf8')
    process.stdout.write(`[migrate] ${file} ... `)
    await runRaw(sql)
    console.log('ok')
  }
  console.log(`[migrate] 完成，共 ${files.length} 个脚本（库：${config.mysql.database}）`)
}

migrate().catch((err) => {
  console.error('[migrate] 失败：', err.message)
  process.exit(1)
})
