import { http } from './client'
import type {
  AdminUser, AppNotification, AppRelease, Item, ItemPayload, ItemStatus,
  Media, NotifyLevel, NotifyTarget, PageResult, StatsOverview
} from '@/types'

/**
 * 控制台所有接口按模块聚在这里。
 * 前缀 /api/admin 由 client 的 baseURL（/api）加上各方法的 '/admin/...' 拼出，
 * 除 login 外都需要携带 Bearer token（拦截器自动加）。
 */

export const authApi = {
  login: (data: { username: string; password: string }) =>
    http.post<{ token: string; user: AdminUser }>('/admin/login', data),
  profile: () => http.get<AdminUser>('/admin/profile'),
  updateProfile: (data: { nickname?: string; avatarUrl?: string | null }) =>
    http.put<AdminUser>('/admin/profile', data),
  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    http.post<null>('/admin/password', data)
}

export const mediaApi = {
  list: (params: { page?: number; pageSize?: number; keyword?: string } = {}) =>
    http.get<PageResult<Media>>('/admin/media', { params }),
  upload: (file: File) => {
    // 上传字段名固定为 file，和 server 的 upload.single('file') 对应
    const form = new FormData()
    form.append('file', file)
    return http.post<Media>('/admin/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  remove: (id: number) => http.delete<null>(`/admin/media/${id}`)
}

/* -------------------------------------------------- 示例模块：item（条目） */

export const itemApi = {
  list: (
    params: {
      page?: number
      pageSize?: number
      category?: string
      keyword?: string
      status?: string
      published?: string
    } = {}
  ) => http.get<PageResult<Item>>('/admin/items', { params }),
  create: (data: ItemPayload) => http.post<Item>('/admin/items', data),
  update: (id: number, data: ItemPayload) => http.put<Item>(`/admin/items/${id}`, data),
  // 状态流转单独走一个口，payload 只有 { status }
  setStatus: (id: number, status: ItemStatus) =>
    http.patch<Item>(`/admin/items/${id}/status`, { status }),
  setPublished: (id: number, published: boolean) =>
    http.patch<Item>(`/admin/items/${id}/publish`, { published }),
  remove: (id: number) => http.delete<null>(`/admin/items/${id}`)
}

/* -------------------------------------------------- 站内消息 */

export interface NotificationPayload {
  title: string
  content: string
  level: NotifyLevel
  target: NotifyTarget
  /** 客户端跳转标识，骨架里可不填 */
  link?: string
  /** true/缺省=立即推送，false=存草稿 */
  push?: boolean
}

export const notificationApi = {
  list: (params: { pushed?: string } = {}) =>
    http.get<{ list: AppNotification[] }>('/admin/notifications', { params }),
  create: (data: NotificationPayload) => http.post<AppNotification>('/admin/notifications', data),
  update: (id: number, data: Partial<NotificationPayload>) =>
    http.put<AppNotification>(`/admin/notifications/${id}`, data),
  push: (id: number) => http.post<AppNotification>(`/admin/notifications/${id}/push`),
  remove: (id: number) => http.delete<null>(`/admin/notifications/${id}`)
}

/* -------------------------------------------------- App 版本 */

export interface ReleasePayload {
  versionCode: number
  versionName: string
  changelog: string
  downloadUrl: string
  forceUpdate: boolean
  published: boolean
}

export const releaseApi = {
  list: () => http.get<{ list: AppRelease[] }>('/admin/releases'),
  create: (data: ReleasePayload) => http.post<AppRelease>('/admin/releases', data),
  update: (id: number, data: Partial<ReleasePayload>) =>
    http.put<AppRelease>(`/admin/releases/${id}`, data),
  publish: (id: number) => http.post<AppRelease>(`/admin/releases/${id}/publish`),
  remove: (id: number) => http.delete<null>(`/admin/releases/${id}`)
}

/* -------------------------------------------------- 概览统计 */

export const statsApi = {
  get: () => http.get<StatsOverview>('/admin/stats')
}
