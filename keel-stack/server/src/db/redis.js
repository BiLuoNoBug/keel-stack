import Redis from 'ioredis'
import { config } from '../config/index.js'

export const redis = new Redis({
  host: config.redis.host,
  port: config.redis.port,
  db: config.redis.db,
  lazyConnect: false,
  maxRetriesPerRequest: 3,
  // 连不上时不要疯狂刷屏，交给上层降级
  retryStrategy: (times) => Math.min(times * 500, 5000)
})

redis.on('error', (err) => {
  if (config.env !== 'test') console.warn('[redis]', err.message)
})

/**
 * Redis 在这一层被刻意设计成「可降级」：
 * 缓存挂了不能影响主流程，所以所有失败都吞掉并退化成直连数据库。
 */

/** 缓存读取，失败时返回 null 并降级到数据库 */
export async function cacheGet(key) {
  try {
    const raw = await redis.get(key)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

/** 缓存写入，失败时静默忽略 */
export async function cacheSet(key, value, ttlSeconds = 60) {
  try {
    await redis.set(key, JSON.stringify(value), 'EX', ttlSeconds)
  } catch {
    /* 缓存不可用时不影响主流程 */
  }
}

/** 按前缀清理缓存 */
export async function cacheDelByPrefix(prefix) {
  try {
    const keys = await redis.keys(`${prefix}*`)
    if (keys.length) await redis.del(keys)
  } catch {
    /* ignore */
  }
}

/** 自增计数：订单号之类的「全局递增序号」用它，Redis 挂了退回时间戳 */
export async function nextSequence(key, ttlSeconds = 24 * 3600) {
  try {
    const n = await redis.incr(key)
    if (n === 1) await redis.expire(key, ttlSeconds)
    return n
  } catch {
    return Date.now() % 100000
  }
}
