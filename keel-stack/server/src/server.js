import http from 'node:http'
import { createApp } from './app.js'
import { config } from './config/index.js'
import { ping } from './db/pool.js'
import { redis } from './db/redis.js'
import { initSocket } from './socket/index.js'

const app = createApp()
const server = http.createServer(app.callback())
initSocket(server)

async function bootstrap() {
  // MySQL 连不上直接退出：没有数据库，接口全都不可用，早点失败更好排查
  try {
    await ping()
    console.log(`[mysql] 已连接 ${config.mysql.host}:${config.mysql.port}/${config.mysql.database}`)
  } catch (err) {
    console.error('[mysql] 连接失败：', err.message)
    console.error('       先执行：cd server && docker compose up -d')
    process.exit(1)
  }

  // Redis 只做缓存与自增，连不上就降级，不让整个服务起不来
  try {
    await redis.ping()
    console.log(`[redis] 已连接 ${config.redis.host}:${config.redis.port}/${config.redis.db}`)
  } catch (err) {
    console.warn('[redis] 连接失败，缓存与实时推送将降级：', err.message)
  }

  server.listen(config.port, () => {
    console.log(`[server] 服务已启动 -> http://127.0.0.1:${config.port}`)
    console.log(`[server] 图片静态目录 -> ${config.uploadDir}`)
    console.log(`[server] 环境 -> ${config.env}`)
  })
}

const shutdown = async (signal) => {
  console.log(`\n[server] 收到 ${signal}，正在关闭...`)
  server.close()
  await redis.quit().catch(() => {})
  process.exit(0)
}

process.on('SIGINT', () => shutdown('SIGINT'))
process.on('SIGTERM', () => shutdown('SIGTERM'))

bootstrap()
