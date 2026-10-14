<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Badge, notification, Tooltip } from 'ant-design-vue'
import * as Icons from '@ant-design/icons-vue'
import { brand } from '@/theme/tokens'
import { useAuthStore } from '@/stores/auth'
import { useRealtimeStore } from '@/stores/realtime'
import { routes } from '@/router'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const realtime = useRealtimeStore()

const collapsed = ref(false)

/** 图标名 -> 组件：meta.icon 写字符串，渲染时从这里取，省去逐个 import */
function iconOf(name?: string) {
  if (!name) return null
  return ((Icons as Record<string, unknown>)[name] as never) ?? null
}

// 菜单直接由路由 meta 生成，新增页面不用回来改这里
const menuRoutes = computed(() =>
  (routes.find((r) => r.path === '/')?.children ?? [])
    .filter((r) => r.meta?.title)
    .map((r) => ({
      key: String(r.name),
      title: r.meta?.title as string,
      icon: iconOf(r.meta?.icon as string),
      // 条目管理带未读角标
      badge: r.name === 'items'
    }))
)

const selectedKeys = computed(() => [String(route.name ?? 'dashboard')])

const currentTitle = computed(() => (route.meta?.title as string) || '概览')
const currentSub = computed(() => (route.meta?.sub as string) || '')

function onMenuClick(info: { key: string | number }) {
  router.push({ name: String(info.key) })
}

async function handleLogout() {
  auth.logout()
  realtime.disconnect()
  router.replace({ name: 'login' })
}

/** 点铃铛：清角标并跳到条目页 */
function goItems() {
  realtime.clearUnread()
  router.push({ name: 'items' })
}

onMounted(() => {
  realtime.connect()
  if (!auth.user) auth.fetchProfile().catch(() => {})
})

onUnmounted(() => {
  realtime.disconnect()
})

// 有新条目落库时弹一个桌面通知，点了直接跳过去
watch(
  () => realtime.tick,
  () => {
    if (realtime.lastEventType !== 'item:changed') return
    if (realtime.lastItemAction !== 'created') return
    const item = realtime.latestItem
    if (!item) return
    notification.info({
      message: `新条目：${item.title}`,
      description: `${item.category} · ${item.statusText}`,
      placement: 'topRight',
      duration: 6,
      onClick: () => router.push({ name: 'items' })
    })
  }
)
</script>

<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider
      v-model:collapsed="collapsed"
      collapsible
      :trigger="null"
      :width="228"
      :collapsed-width="72"
      theme="light"
      style="border-right: 1px solid #eeeef3; position: sticky; top: 0; height: 100vh"
    >
      <div class="brand" :class="{ collapsed }">
        <div class="brand-logo">
          <Icons.DeploymentUnitOutlined />
        </div>
        <div v-if="!collapsed" class="brand-text">
          <div class="brand-name">Keel</div>
          <div class="brand-sub">后台控制台</div>
        </div>
      </div>

      <a-menu
        :selected-keys="selectedKeys"
        mode="inline"
        style="border-inline-end: none"
        @click="onMenuClick"
      >
        <a-menu-item v-for="item in menuRoutes" :key="item.key">
          <template #icon>
            <component :is="item.icon" v-if="item.icon" />
          </template>
          <span>{{ item.title }}</span>
          <Badge
            v-if="item.badge && realtime.unreadItems > 0"
            :count="realtime.unreadItems"
            :offset="[10, 0]"
            :overflow-count="99"
          />
        </a-menu-item>
      </a-menu>

      <div class="sider-foot">
        <Tooltip :title="realtime.connected ? '实时通道已连接' : '实时通道断开'" placement="right">
          <div class="conn">
            <span class="conn-dot" :style="{ background: realtime.connected ? brand.success : brand.danger }" />
            <span v-if="!collapsed">{{ realtime.connected ? '实时同步中' : '连接断开' }}</span>
          </div>
        </Tooltip>
      </div>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="header">
        <div class="header-left">
          <button
            class="icon-btn"
            :title="collapsed ? '展开菜单' : '收起菜单'"
            @click="collapsed = !collapsed"
          >
            <Icons.MenuUnfoldOutlined v-if="collapsed" />
            <Icons.MenuFoldOutlined v-else />
          </button>

          <div class="header-titles">
            <h1 class="header-title">{{ currentTitle }}</h1>
            <span v-if="currentSub" class="header-sub">{{ currentSub }}</span>
          </div>
        </div>

        <div class="header-right">
          <button class="icon-btn" title="查看条目" @click="goItems">
            <a-badge :count="realtime.unreadItems" :overflow-count="99" :offset="[2, -2]">
              <Icons.BellOutlined />
            </a-badge>
          </button>

          <span class="divider-v" />

          <a-dropdown placement="bottomRight" :trigger="['click']">
            <div class="user">
              <a-avatar :size="30" class="user-avatar">
                <template #icon><Icons.UserOutlined /></template>
              </a-avatar>
              <div class="user-meta">
                <span class="user-name">{{ auth.user?.nickname || auth.user?.username || '管理员' }}</span>
                <span class="user-role">管理员</span>
              </div>
              <Icons.DownOutlined class="user-caret" />
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="system" @click="router.push({ name: 'system' })">
                  <Icons.SettingOutlined /> 系统设置
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" danger @click="handleLogout">
                  <Icons.LogoutOutlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content>
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 18px 14px;
  margin-bottom: 4px;
}

.brand.collapsed {
  justify-content: center;
  padding: 18px 0 14px;
}

.brand-logo {
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 18px;
  background: linear-gradient(135deg, #ff8a58, #f4531e);
  box-shadow: 0 6px 16px rgba(244, 83, 30, 0.28);
}

.brand-name {
  font-weight: 700;
  font-size: 16px;
  letter-spacing: -0.3px;
  line-height: 1.2;
}

.brand-sub {
  font-size: 11px;
  color: #9a9aa8;
  margin-top: 2px;
}

.header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  line-height: normal;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: saturate(1.8) blur(16px);
  -webkit-backdrop-filter: saturate(1.8) blur(16px);
  box-shadow: 0 1px 0 rgba(23, 23, 37, 0.05), 0 10px 26px -20px rgba(23, 23, 37, 0.35);
}

/* 底部一条品牌色细渐变，给顶部一点温度 */
.header::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  pointer-events: none;
  background: linear-gradient(
    90deg,
    rgba(255, 107, 53, 0.9),
    rgba(255, 107, 53, 0.28) 22%,
    transparent 58%
  );
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.header-titles {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.header-title {
  font-size: 17px;
  font-weight: 650;
  line-height: 1.2;
  margin: 0;
  letter-spacing: -0.2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-sub {
  font-size: 12px;
  color: #9a9aa8;
  margin-top: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.icon-btn {
  width: 38px;
  height: 38px;
  flex: none;
  display: grid;
  place-items: center;
  padding: 0;
  border: none;
  border-radius: 11px;
  background: transparent;
  color: #6b6b7b;
  font-size: 17px;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, transform 0.12s ease;
}

.icon-btn:hover {
  background: rgba(23, 23, 37, 0.05);
  color: var(--df-ink);
}

.icon-btn:active {
  transform: scale(0.94);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.divider-v {
  width: 1px;
  height: 22px;
  margin: 0 4px;
  background: rgba(23, 23, 37, 0.09);
}

.user {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 5px 10px 5px 5px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.18s ease;
}

.user:hover {
  background: rgba(23, 23, 37, 0.05);
}

.user-avatar {
  background: linear-gradient(135deg, #ff8a58, #f4531e);
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
}

.user-role {
  font-size: 11px;
  color: #a0a0af;
  margin-top: 2px;
}

.user-caret {
  font-size: 10px;
  color: #b3b3c0;
}

.sider-foot {
  position: absolute;
  bottom: 56px;
  left: 0;
  right: 0;
  padding: 0 18px;
  font-size: 12px;
  color: #9a9aa8;
}

.conn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.conn-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.fade-leave-to {
  opacity: 0;
}
</style>
