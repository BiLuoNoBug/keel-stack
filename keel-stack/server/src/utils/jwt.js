import jwt from 'jsonwebtoken'
import { config } from '../config/index.js'
import { unauthorized } from './response.js'

export function signToken(payload) {
  return jwt.sign(payload, config.jwt.secret, { expiresIn: config.jwt.expiresIn })
}

export function verifyToken(token) {
  try {
    return jwt.verify(token, config.jwt.secret)
  } catch {
    throw unauthorized()
  }
}

/** 从请求头取 Bearer token */
export function readBearer(ctx) {
  const header = ctx.get('authorization') || ''
  const [scheme, token] = header.split(' ')
  if (scheme?.toLowerCase() !== 'bearer' || !token) return null
  return token
}
