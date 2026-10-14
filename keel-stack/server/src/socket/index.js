import { Server } from 'socket.io'

let io = null

/**
 * 房间划分。
 *
 * 骨架保留三个房间的概念：两端 App（CLIENT）与控制台（ADMIN）各自订阅自己关心的推送。
 * 新增业务时，事件按「谁关心」投递到对应房间即可。
 */
export const ROOM = {
  CLIENT: 'room:client',
  ADMIN: 'room:admin'
}

export function initSocket(httpServer) {
  io = new Server(httpServer, {
    cors: { origin: '*', methods: ['GET', 'POST'] },
    path: '/socket.io'
  })

  io.on('connection', (socket) => {
    // 客户端连上后自报身份：role 决定进哪个房间
    socket.on('subscribe', ({ role, deviceId } = {}) => {
      const room = role === 'admin' ? ROOM.ADMIN : ROOM.CLIENT
      socket.join(room)
      if (deviceId) socket.join(`device:${deviceId}`)
      socket.emit('subscribed', { room, at: new Date().toISOString() })
    })

    socket.on('unsubscribe', ({ role } = {}) => {
      socket.leave(role === 'admin' ? ROOM.ADMIN : ROOM.CLIENT)
    })

    socket.on('disconnect', () => { /* 房间自动清理 */ })
  })

  return io
}

/** 底层投递：给某个房间发事件 */
export function emitTo(room, event, payload) {
  if (!io) return
  io.to(room).emit(event, payload)
}

/** 给所有人发（比如「有新数据」这种低敏感广播） */
export function emitAll(event, payload) {
  if (!io) return
  io.emit(event, payload)
}

/* ------------------------------------------------------------ 业务事件 */

/**
 * 示例模块 item 的两个事件：
 *   item:changed —— 增删改 / 上下架，两端列表都该刷新
 *   item:status  —— 状态流转，列表原地更新
 * 新业务照着加一组 emitXxx 即可。
 */
export function emitItemChanged(payload) {
  emitTo(ROOM.CLIENT, 'item:changed', payload)
  emitTo(ROOM.ADMIN, 'item:changed', payload)
}

export function emitItemStatus(item) {
  emitTo(ROOM.CLIENT, 'item:status', item)
  emitTo(ROOM.ADMIN, 'item:status', item)
}

/** 站内消息 —— 按 target 只推给对应角色 */
export function emitNotification(payload) {
  const target = payload?.target ?? 'ALL'
  if (target === 'ALL' || target === 'CLIENT') emitTo(ROOM.CLIENT, 'notification:new', payload)
  if (target === 'ALL' || target === 'ADMIN') emitTo(ROOM.ADMIN, 'notification:new', payload)
}

/** 新版本发布 */
export function emitRelease(payload) {
  emitTo(ROOM.CLIENT, 'release:new', payload)
  emitTo(ROOM.ADMIN, 'release:new', payload)
}
