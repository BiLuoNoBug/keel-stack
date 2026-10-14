<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as Icons from '@ant-design/icons-vue'
import { statsApi } from '@/api'
import { itemStatusColor } from '@/theme/tokens'
import { useRealtimeStore } from '@/stores/realtime'
import type { StatsOverview } from '@/types'

const router = useRouter()
const realtime = useRealtimeStore()

const loading = ref(false)
const stats = ref<StatsOverview | null>(null)

async function load() {
  loading.value = true
  try {
    stats.value = await statsApi.get()
  } finally {
    loading.value = false
  }
}

onMounted(load)
// 收到任何实时事件都拉一次最新统计
watch(() => realtime.tick, load)

/** 顶部四张通用指标卡：都只统计「有多少」，不做业务分析 */
const cards = computed(() => [
  {
    title: '条目总数',
    value: stats.value?.itemTotal ?? 0,
    foot: '示例模块 item 的数据量',
    icon: Icons.AppstoreOutlined,
    to: 'items'
  },
  {
    title: '媒体图片',
    value: stats.value?.mediaTotal ?? 0,
    foot: '存放在 uploads 目录',
    icon: Icons.PictureOutlined,
    to: 'media'
  },
  {
    title: '站内消息',
    value: stats.value?.notificationTotal ?? 0,
    foot: '已写入的消息条数',
    icon: Icons.NotificationOutlined,
    to: 'notifications'
  },
  {
    title: '已发布版本',
    value: stats.value?.latestRelease?.versionName ?? '—',
    foot: stats.value?.latestRelease
      ? `versionCode ${stats.value.latestRelease.versionCode}`
      : '还没有发布任何版本',
    icon: Icons.CloudUploadOutlined,
    to: 'releases'
  }
])
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">概览</h2>
        <div class="page-sub">骨架运行概况 · 收到实时事件会自动刷新</div>
      </div>
      <a-button @click="load">
        <Icons.ReloadOutlined /> 刷新
      </a-button>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="[16, 16]">
        <a-col v-for="c in cards" :key="c.title" :xs="24" :sm="12" :xl="6">
          <a-card :bordered="false" hoverable class="stat-card" @click="router.push({ name: c.to })">
            <a-statistic :title="c.title" :value="c.value">
              <template #prefix>
                <component :is="c.icon" class="stat-icon" />
              </template>
            </a-statistic>
            <div class="metric-foot">{{ c.foot }}</div>
          </a-card>
        </a-col>
      </a-row>

      <a-card :bordered="false" title="条目状态分布" style="margin-top: 16px">
        <template #extra>
          <a-button type="link" size="small" @click="router.push({ name: 'items' })">
            去条目管理 <Icons.RightOutlined />
          </a-button>
        </template>
        <a-row v-if="stats?.byStatus?.length" :gutter="[16, 16]">
          <a-col v-for="s in stats.byStatus" :key="s.status" :xs="24" :sm="8">
            <a-statistic
              :title="s.statusText"
              :value="s.count"
              :value-style="{ color: itemStatusColor[s.status] }"
            />
          </a-col>
        </a-row>
        <a-empty v-else description="暂无数据" />
      </a-card>

      <a-card :bordered="false" title="最新版本" style="margin-top: 16px">
        <template v-if="stats?.latestRelease">
          <div class="rel-head">
            <span class="rel-name">{{ stats.latestRelease.versionName }}</span>
            <a-tag color="green">已发布</a-tag>
          </div>
          <div class="rel-log">{{ stats.latestRelease.changelog || '（这条版本没有写更新说明）' }}</div>
        </template>
        <a-empty v-else description="还没有发布任何版本" />
      </a-card>
    </a-spin>
  </div>
</template>

<style scoped>
.stat-card {
  height: 100%;
  cursor: pointer;
}

.stat-icon {
  color: #ff6b35;
  margin-right: 6px;
}

.rel-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.rel-name {
  letter-spacing: -0.2px;
}

.rel-log {
  margin-top: 8px;
  font-size: 13px;
  color: #5a5a6b;
  line-height: 1.7;
  white-space: pre-wrap;
}
</style>
