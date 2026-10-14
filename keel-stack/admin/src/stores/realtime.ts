import { defineStore } from 'pinia'
import { io, type Socket } from 'socket.io-client'
import { ref } from 'vue'
import type { AppNotification, AppRelease, Item } from '@/types'

/**
 * 实时通道。
 *
 * 控制台连上后自报 role=admin，服务端据此把连接放进 room:admin；
 * 之后 item 增删改、新消息、新版本都会即时推过来，页面不用轮询 ——
 * 只要 watch 下面的 tick，收到事件就刷新自己。
 */
export const useRealtimeStore = defineStore('realtime', () => {
  const connected = ref(false)
  const latestItem = ref<Item | null>(null)
  // item:changed 的 action，页面据此区分是新增还是改动
  const lastItemAction = ref('')
  // 新增条目的未读数，给侧边栏菜单和顶部铃铛做角标
  const unreadItems = ref(0)
  const latestNotification = ref<AppNotification | null>(null)
  const latestRelease = ref<AppRelease | null>(null)
  // 每收到一个事件就 +1，页面 watch 它来刷新
  const tick = ref(0)
  const lastEventType = ref('')

  let socket: Socket | null = null

  function connect() {
    if (socket) return socket
    socket = io(window.location.origin, {
      path: '/socket.io',
      transports: ['websocket', 'polling']
    })

    socket.on('connect', () => {
      connected.value = true
      // 自报身份，服务端据此订阅房间
      socket?.emit('subscribe', { role: 'admin' })
    })
    socket.on('disconnect', () => {
      connected.value = false
    })

    // 增删改 / 上下架：payload 形如 { action, item?, itemId? }
    socket.on('item:changed', (payload: { action?: string; item?: Item | null } = {}) => {
      if (payload.item) latestItem.value = payload.item
      lastItemAction.value = payload.action ?? ''
      if (payload.action === 'created') unreadItems.value += 1
      lastEventType.value = 'item:changed'
      tick.value += 1
    })
    socket.on('item:status', () => {
      lastEventType.value = 'item:status'
      tick.value += 1
    })
    socket.on('notification:new', (payload: AppNotification) => {
      latestNotification.value = payload
      lastEventType.value = 'notification:new'
      tick.value += 1
    })
    socket.on('release:new', (payload: AppRelease) => {
      latestRelease.value = payload
      lastEventType.value = 'release:new'
      tick.value += 1
    })

    return socket
  }

  /** 点掉角标 */
  function clearUnread() {
    unreadItems.value = 0
  }

  function disconnect() {
    socket?.disconnect()
    socket = null
    connected.value = false
  }

  return {
    connected, latestItem, lastItemAction, unreadItems, latestNotification, latestRelease,
    tick, lastEventType, connect, disconnect, clearUnread
  }
})
