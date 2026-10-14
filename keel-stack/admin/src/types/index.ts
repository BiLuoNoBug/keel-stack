/** 与后端 DTO 一一对应的类型，字段名保持一致，改接口时同步改这里 */

export interface AdminUser {
  id: number
  username: string
  nickname: string
  avatarUrl?: string | null
  lastLoginAt?: string | null
  createdAt?: string
}

export interface Media {
  id: number
  filename: string
  url: string
  mime: string
  size: number
  refCount: number
  createdAt: string
  fullUrl?: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

/* -------------------------------------------------- 示例模块：item（条目） */

export type ItemStatus = 'TODO' | 'DOING' | 'DONE'
export type ItemPriority = 'LOW' | 'NORMAL' | 'HIGH'

export interface Item {
  id: number
  title: string
  category: string
  content: string
  coverUrl?: string | null
  status: ItemStatus
  statusText: string
  /** 当前状态可流转到的下一步（后端状态机下发） */
  allowNext: string[]
  priority: ItemPriority
  published: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string
}

/** 新增 / 编辑条目提交的字段；状态单独走 PATCH，不在这里 */
export interface ItemPayload {
  title: string
  category: string
  content: string
  coverUrl?: string | null
  priority: ItemPriority
  published: boolean
  sortOrder: number
}

/* -------------------------------------------------- 站内消息 */

export type NotifyLevel = 'INFO' | 'IMPORTANT'
export type NotifyTarget = 'ALL' | 'CLIENT' | 'ADMIN'

export interface AppNotification {
  id: number
  title: string
  content: string
  link: string
  level: NotifyLevel
  target: NotifyTarget
  pushed: boolean
  pushedAt?: string | null
  pushCount: number
  createdAt: string
}

/* -------------------------------------------------- App 版本 */

export interface AppRelease {
  id: number
  versionCode: number
  versionName: string
  changelog: string
  downloadUrl: string
  forceUpdate: boolean
  published: boolean
  createdAt: string
  updatedAt: string
}

/* -------------------------------------------------- 概览统计 */

export interface ItemStatusCount {
  status: ItemStatus
  statusText: string
  count: number
}

export interface StatsOverview {
  itemTotal: number
  byStatus: ItemStatusCount[]
  mediaTotal: number
  notificationTotal: number
  latestRelease: AppRelease | null
}
