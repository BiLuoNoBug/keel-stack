<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import * as Icons from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { notificationApi } from '@/api'
import { useRealtimeStore } from '@/stores/realtime'
import { brand } from '@/theme/tokens'
import type { AppNotification, NotifyLevel, NotifyTarget } from '@/types'

/**
 * 站内消息。
 *
 * 写一条 -> 通过 Socket.IO 推给目标角色 -> 客户端弹横幅。
 * 骨架里 target 只有「全部 / 客户端(cli) / 控制台(admin)」三种。
 */
const realtime = useRealtimeStore()

const loading = ref(false)
const sending = ref(false)
const list = ref<AppNotification[]>([])
const filter = ref<string>('')

const form = reactive({
  title: '',
  content: '',
  level: 'INFO' as NotifyLevel,
  target: 'ALL' as NotifyTarget
})

const levelOptions = [
  { label: '普通提醒', value: 'INFO' },
  { label: '重要通知', value: 'IMPORTANT' }
]

const targetOptions = [
  { label: '全部', value: 'ALL' },
  { label: '只推客户端', value: 'CLIENT' },
  { label: '只推控制台', value: 'ADMIN' }
]

const targetLabel: Record<NotifyTarget, string> = {
  ALL: '全部',
  CLIENT: '客户端',
  ADMIN: '控制台'
}

const pushedCount = computed(() => list.value.filter((n) => n.pushed).length)

const canSend = computed(() => form.title.trim().length > 0 && form.content.trim().length > 0)

async function load() {
  loading.value = true
  try {
    const res = await notificationApi.list({
      pushed: filter.value === '' ? undefined : filter.value
    })
    list.value = res.list
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(filter, load)
// 别的控制台窗口推了新消息，这里跟着刷新
watch(() => realtime.tick, () => {
  if (realtime.lastEventType === 'notification:new') load()
})

async function send() {
  if (!canSend.value) {
    message.warning('标题和内容都要填')
    return
  }
  sending.value = true
  try {
    await notificationApi.create({ ...form, push: true })
    message.success('已经推出去了')
    form.title = ''
    form.content = ''
    form.level = 'INFO'
    await load()
  } finally {
    sending.value = false
  }
}

async function saveDraft() {
  if (!canSend.value) {
    message.warning('标题和内容都要填')
    return
  }
  await notificationApi.create({ ...form, push: false })
  message.success('已保存为草稿，还没推出去')
  form.title = ''
  form.content = ''
  await load()
}

async function resend(n: AppNotification) {
  await notificationApi.push(n.id)
  message.success('已重新推送')
  await load()
}

function remove(n: AppNotification) {
  Modal.confirm({
    title: `删除「${n.title}」？`,
    content: '删除后客户端消息列表里也会没有这条（已经收到的通知无法撤回）。',
    okText: '删除',
    okButtonProps: { danger: true },
    cancelText: '算了',
    async onOk() {
      await notificationApi.remove(n.id)
      message.success('已删除')
      await load()
    }
  })
}
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">消息推送</h2>
        <div class="page-sub">
          推出去的消息会实时到达客户端 · 已推送 {{ pushedCount }} 条
        </div>
      </div>
      <a-segmented
        v-model:value="filter"
        :options="[
          { label: '全部', value: '' },
          { label: '已推送', value: 'true' },
          { label: '草稿', value: 'false' }
        ]"
      />
    </div>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :xl="10">
        <a-card :bordered="false" title="写一条消息">
          <a-form layout="vertical">
            <a-form-item label="标题">
              <a-input
                v-model:value="form.title"
                placeholder="例如：新版本已发布"
                :maxlength="32"
                show-count
              />
            </a-form-item>

            <a-form-item label="内容">
              <a-textarea
                v-model:value="form.content"
                :rows="4"
                :maxlength="200"
                show-count
                placeholder="例如：修复了若干问题，去版本管理看看更新内容～"
              />
            </a-form-item>

            <a-row :gutter="12">
              <a-col :span="12">
                <a-form-item label="类型">
                  <a-select v-model:value="form.level" :options="levelOptions" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="推送给">
                  <a-select v-model:value="form.target" :options="targetOptions" />
                </a-form-item>
              </a-col>
            </a-row>

            <div class="preview">
              <div class="preview-label"><Icons.MobileOutlined /> 客户端里长这样</div>
              <div class="bubble" :class="{ important: form.level === 'IMPORTANT' }">
                <div class="bubble-head">
                  <span class="bubble-dot" :style="{ background: form.level === 'IMPORTANT' ? brand.danger : brand.orange }" />
                  <span class="bubble-title">{{ form.title || '消息标题' }}</span>
                  <span class="bubble-time">刚刚</span>
                </div>
                <div class="bubble-body">{{ form.content || '消息内容会显示在这里' }}</div>
              </div>
            </div>

            <div class="actions">
              <a-button type="primary" block :loading="sending" :disabled="!canSend" @click="send">
                <Icons.SendOutlined /> 立即推送
              </a-button>
              <a-button block :disabled="!canSend" @click="saveDraft">存为草稿</a-button>
            </div>
          </a-form>
        </a-card>
      </a-col>

      <a-col :xs="24" :xl="14">
        <a-card :bordered="false" :body-style="{ paddingTop: '8px' }">
          <a-spin :spinning="loading">
            <a-empty v-if="!list.length" description="还没有发过消息" />
            <div v-else class="feed">
              <div v-for="n in list" :key="n.id" class="feed-item">
                <div class="feed-line">
                  <span
                    class="feed-dot"
                    :style="{ background: n.level === 'IMPORTANT' ? brand.danger : brand.orange }"
                  />
                </div>
                <div class="feed-body">
                  <div class="feed-head">
                    <span class="feed-title">{{ n.title }}</span>
                    <a-tag v-if="n.level === 'IMPORTANT'" color="red" style="margin-left: 8px">重要</a-tag>
                    <a-tag :color="n.pushed ? 'green' : 'default'" style="margin-left: 4px">
                      {{ n.pushed ? '已推送' : '草稿' }}
                    </a-tag>
                  </div>
                  <div class="feed-content">{{ n.content }}</div>
                  <div class="feed-meta">
                    <span><Icons.TeamOutlined /> 推给{{ targetLabel[n.target] }}</span>
                    <span><Icons.ClockCircleOutlined /> {{ dayjs(n.createdAt).format('MM-DD HH:mm') }}</span>
                    <span v-if="n.pushCount > 1"><Icons.RedoOutlined /> 推送 {{ n.pushCount }} 次</span>
                  </div>
                  <div class="feed-actions">
                    <a-button type="link" size="small" @click="resend(n)">
                      <Icons.SendOutlined /> 重新推送
                    </a-button>
                    <a-button type="link" size="small" danger @click="remove(n)">删除</a-button>
                  </div>
                </div>
              </div>
            </div>
          </a-spin>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
.preview {
  margin: 4px 0 18px;
  padding: 14px;
  border-radius: 14px;
  background: #fafafc;
}

.preview-label {
  font-size: 12px;
  color: #9a9aa8;
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 10px;
}

.bubble {
  background: #fff;
  border: 1px solid #eeeef3;
  border-left: 3px solid #ff6b35;
  border-radius: 12px;
  padding: 12px 14px;
}

.bubble.important {
  border-left-color: #e65353;
  background: rgba(230, 83, 83, 0.04);
}

.bubble-head {
  display: flex;
  align-items: center;
  gap: 7px;
}

.bubble-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.bubble-title {
  font-weight: 600;
  font-size: 14px;
}

.bubble-time {
  margin-left: auto;
  font-size: 11px;
  color: #b3b3c0;
}

.bubble-body {
  font-size: 13px;
  color: #5a5a6b;
  line-height: 1.6;
  margin-top: 6px;
  padding-left: 13px;
}

.actions {
  display: grid;
  gap: 10px;
}

.feed {
  display: grid;
  gap: 2px;
  padding: 8px 0;
}

.feed-item {
  display: flex;
  gap: 14px;
}

.feed-line {
  position: relative;
  width: 10px;
  flex: none;
  display: flex;
  justify-content: center;
}

.feed-line::after {
  content: '';
  position: absolute;
  top: 18px;
  bottom: -2px;
  width: 1px;
  background: #eeeef3;
}

.feed-item:last-child .feed-line::after {
  display: none;
}

.feed-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  margin-top: 6px;
  box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.12);
}

.feed-body {
  flex: 1;
  min-width: 0;
  padding: 0 0 20px;
}

.feed-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.feed-title {
  font-weight: 600;
  font-size: 15px;
}

.feed-content {
  color: #5a5a6b;
  font-size: 13px;
  line-height: 1.65;
  margin-top: 5px;
}

.feed-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 8px;
  font-size: 12px;
  color: #a0a0af;
}

.feed-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.feed-actions {
  margin-top: 4px;
}
</style>
