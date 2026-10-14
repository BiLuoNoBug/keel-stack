import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

/** server/sql */
export const sqlDir = path.resolve(__dirname, '../../sql')
/** server/uploads */
export const uploadsDir = path.resolve(__dirname, '../../uploads')
