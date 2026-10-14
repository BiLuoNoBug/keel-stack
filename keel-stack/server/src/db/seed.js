import bcrypt from 'bcryptjs'
import { query, queryOne, insert, execute } from './pool.js'

/**
 * 演示数据：一个管理员 + 若干 item + 一条版本记录。
 * 幂等，重复执行不会产生重复数据：npm run db:seed
 */

const DEMO_ITEMS = [
  { title: '克隆骨架仓库', category: '准备工作', content: 'git clone 之后 npm install', status: 'DONE', priority: 'NORMAL', sortOrder: 1 },
  { title: '起 MySQL 与 Redis', category: '准备工作', content: 'docker compose up -d', status: 'DONE', priority: 'HIGH', sortOrder: 2 },
  { title: '跑通服务端接口', category: '搭建', content: 'npm start，然后访问 /api/health', status: 'DOING', priority: 'HIGH', sortOrder: 3 },
  { title: '接上实时通道', category: '搭建', content: '控制台右下角应显示「实时同步中」', status: 'TODO', priority: 'NORMAL', sortOrder: 4 },
  { title: '发布第一个 App 版本', category: '发布', content: '在控制台「版本管理」里新增一条版本记录', status: 'TODO', priority: 'LOW', sortOrder: 5 }
]

async function seed() {
  // 1. 管理员（密码用 bcrypt 存，不要明文入库）
  const hash = await bcrypt.hash('admin123', 10)
  await execute(
    `INSERT INTO admin_users (username, password_hash, nickname) VALUES (?,?,?)
     ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash)`,
    ['admin', hash, '管理员']
  )
  console.log('[seed] 管理员 admin / admin123')

  // 2. 示例条目
  const existing = Number((await queryOne('SELECT COUNT(*) AS c FROM items'))?.c ?? 0)
  if (existing > 0) {
    console.log(`[seed] items 已有 ${existing} 条，跳过`)
  } else {
    for (const it of DEMO_ITEMS) {
      await insert(
        `INSERT INTO items (title, category, content, status, priority, published, sort_order)
         VALUES (?,?,?,?,?,1,?)`,
        [it.title, it.category, it.content, it.status, it.priority, it.sortOrder]
      )
    }
    console.log(`[seed] items 写入 ${DEMO_ITEMS.length} 条`)
  }

  // 3. 版本记录（App 启动时会查 /api/app/releases/latest）
  const hasRelease = await queryOne('SELECT id FROM app_releases LIMIT 1')
  if (!hasRelease) {
    await insert(
      `INSERT INTO app_releases (version_code, version_name, changelog, download_url, force_update, published)
       VALUES (?,?,?,?,0,1)`,
      [1, '1.0.0', '第一个版本：架子搭好了。', '']
    )
    console.log('[seed] 写入版本记录 1.0.0')
  }

  const items = await query('SELECT status, COUNT(*) AS c FROM items GROUP BY status')
  console.log('[seed] 完成：', items.map((r) => `${r.status}=${r.c}`).join(' '))
}

seed()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error('[seed] 失败：', err.message)
    process.exit(1)
  })
