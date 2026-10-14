import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getToken } from '@/api/client'

/**
 * 路由表就是「控制台有哪些页面」的单一来源：
 * BasicLayout 的侧边栏菜单直接读这里每个子路由的 meta（title/sub/icon）生成，
 * 新增页面只改这一处即可。
 */
export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '概览', sub: '骨架运行概况', icon: 'DashboardOutlined' }
      },
      {
        path: 'items',
        name: 'items',
        component: () => import('@/views/ItemListView.vue'),
        meta: { title: '条目管理', sub: '示例模块，照它抄自己的业务', icon: 'AppstoreOutlined' }
      },
      {
        path: 'media',
        name: 'media',
        component: () => import('@/views/MediaLibraryView.vue'),
        meta: { title: '图片媒体库', sub: '封面与配图都从这里取', icon: 'PictureOutlined' }
      },
      {
        path: 'notifications',
        name: 'notifications',
        component: () => import('@/views/NotificationView.vue'),
        meta: { title: '消息推送', sub: '给客户端和控制台发消息', icon: 'NotificationOutlined' }
      },
      {
        path: 'releases',
        name: 'releases',
        component: () => import('@/views/ReleaseView.vue'),
        meta: { title: '版本管理', sub: '客户端更新从这里发', icon: 'CloudUploadOutlined' }
      },
      {
        path: 'system',
        name: 'system',
        component: () => import('@/views/SystemSettingView.vue'),
        meta: { title: '系统设置', sub: '账号与服务状态', icon: 'SettingOutlined' }
      }
    ]
  },
  // 未知路径一律回到概览，避免白屏
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const hasToken = !!getToken()
  // 没登录又访问受保护页 -> 去登录，并带上回跳地址
  if (!to.meta.public && !hasToken) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // 已登录还去登录页 -> 直接进控制台
  if (to.name === 'login' && hasToken) {
    return { name: 'dashboard' }
  }
  // 有 token 但内存里没有用户信息（比如刚刷新），补拉一次
  const auth = useAuthStore()
  if (hasToken && !auth.user) auth.fetchProfile().catch(() => {})
  return true
})

export default router
