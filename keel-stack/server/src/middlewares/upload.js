import path from 'node:path'
import multer from '@koa/multer'
import dayjs from 'dayjs'
import { nanoid } from 'nanoid'
import { config } from '../config/index.js'

const storage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, config.uploadDir),
  filename: (_req, file, cb) => {
    const ext = (path.extname(file.originalname) || '.jpg').toLowerCase()
    cb(null, `${dayjs().format('YYYYMMDDHHmmss')}-${nanoid(6)}${ext}`)
  }
})

/** 只放行具体到子类型的 mime；image/* 这种通配不算数（客户端必须给真实 mime） */
const ALLOWED_IMAGE_MIME = /^image\/(jpe?g|png|webp|gif|bmp|heic|heif|avif)$/i

/** 图片上传中间件，控制台与 App 共用 */
export const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024 },
  fileFilter: (_req, file, cb) => {
    if (!ALLOWED_IMAGE_MIME.test(file.mimetype || '')) {
      cb(new Error('只支持 jpg / png / webp / gif / bmp / heic 图片'))
      return
    }
    cb(null, true)
  }
})
