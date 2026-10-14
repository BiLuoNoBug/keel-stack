import path from 'node:path'
import { fileURLToPath } from 'node:url'
import dotenv from 'dotenv'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
export const rootDir = path.resolve(__dirname, '../..')

dotenv.config({ path: path.join(rootDir, '.env') })

/**
 * 全部配置集中在这里，代码里不要再直接读 process.env。
 * 好处：换环境只改 .env；默认值保证 clone 下来就能跑通 demo。
 */
export const config = {
  env: process.env.NODE_ENV || 'development',
  port: Number(process.env.PORT || 8080),
  rootDir,
  uploadDir: path.resolve(rootDir, process.env.UPLOAD_DIR || 'uploads'),
  publicBaseUrl: (process.env.PUBLIC_BASE_URL || `http://127.0.0.1:${process.env.PORT || 8080}`).replace(/\/$/, ''),

  mysql: {
    host: process.env.MYSQL_HOST || '127.0.0.1',
    port: Number(process.env.MYSQL_PORT || 3306),
    user: process.env.MYSQL_USER || 'root',
    password: process.env.MYSQL_PASSWORD || '',
    database: process.env.MYSQL_DATABASE || 'keel'
  },

  redis: {
    host: process.env.REDIS_HOST || '127.0.0.1',
    port: Number(process.env.REDIS_PORT || 6379),
    db: Number(process.env.REDIS_DB || 0)
  },

  jwt: {
    secret: process.env.JWT_SECRET || 'keel-dev-secret-change-me',
    expiresIn: process.env.JWT_EXPIRES_IN || '7d'
  }
}

export const isDev = config.env !== 'production'
