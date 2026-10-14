<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import * as Icons from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { releaseApi } from '@/api'
import { useRealtimeStore } from '@/stores/realtime'
import { brand } from '@/theme/tokens'
import type { AppRelease } from '@/types'

/**
 * 版本发布。
 *
 * 客户端启动 / 手动检查更新时来比对 versionCode，比本地大就提示下载。
 * 与具体业务无关，属于客户端基础设施，可原样保留。
 */
const realtime = useRealtimeStore()

// 与客户端工程的版本号保持一致；仅用于「本地是否比服务端旧」的对比展示
const CURRENT_CLIENT = { versionCode: 1, versionName: '1.0.0' }

const loading = ref(false)
const saving = ref(false)
const list = ref<AppRelease[]>([])
const modalOpen = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  versionCode: CURRENT_CLIENT.versionCode + 1,
  versionName: '',
  changelog: '',
  downloadUrl: '',
  forceUpdate: false,
  published: true
})

const latestPublished = computed(() => list.value.find((r) => r.published) ?? null)

const clientHasNewer = computed(() => {
  const latest = latestPublished.value
  if (!latest) return false
  return latest.versionCode > CURRENT_CLIENT.versionCode
})

async function load() {
  loading.value = true
  try {
    const res = await releaseApi.list()
    list.value = res.list
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => realtime.tick, () => {
  if (realtime.lastEventType === 'release:new') load()
})

function suggestName(code: number) {
  // 2 -> 2.0.0 的简单推导，仅作为默认值
  return `${code}.0.0`
}

function openCreate() {
  editingId.value = null
  const maxCode = list.value.reduce((m, r) => Math.max(m, r.versionCode), CURRENT_CLIENT.versionCode)
  Object.assign(form, {
    versionCode: maxCode + 1,
    versionName: suggestName(maxCode + 1),
    changelog: '',
    downloadUrl: '',
    forceUpdate: false,
    published: true
  })
  modalOpen.value = true
}

function openEdit(r: AppRelease) {
  editingId.value = r.id
  Object.assign(form, {
    versionCode: r.versionCode,
    versionName: r.versionName,
    changelog: r.changelog,
    downloadUrl: r.downloadUrl,
    forceUpdate: r.forceUpdate,
    published: r.published
  })
  modalOpen.value = true
}

async function submit() {
  if (!form.versionName.trim()) {
    message.warning('请填写版本名')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await releaseApi.update(editingId.value, { ...form })
      message.success('已保存')
    } else {
      await releaseApi.create({ ...form })
      message.success(form.published ? '已发布，客户端会收到更新提醒' : '已存为未发布版本')
    }
    modalOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function publish(r: AppRelease) {
  await releaseApi.publish(r.id)
  message.success(`${r.versionName} 已上线`)
  await load()
}

function remove(r: AppRelease) {
  Modal.confirm({
    title: `删除版本 ${r.versionName}？`,
    content: r.published ? '删除后客户端就查不到这个版本了。' : undefined,
    okText: '删除',
    okButtonProps: { danger: true },
    cancelText: '算了',
    async onOk() {
      await releaseApi.remove(r.id)
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
        <h2 class="page-title">版本管理</h2>
        <div class="page-sub">
          客户端靠 versionCode 判断有没有新版，比本地大就提示更新
        </div>
      </div>
      <a-button type="primary" @click="openCreate">
        <Icons.CloudUploadOutlined /> 发布新版本
      </a-button>
    </div>

    <a-row :gutter="[16, 16]" style="margin-bottom: 16px">
      <a-col :xs="24" :md="12">
        <div class="metric-card">
          <div class="metric-label"><Icons.MobileOutlined /> 客户端当前版本</div>
          <div class="metric-value">{{ CURRENT_CLIENT.versionName }}</div>
          <div class="metric-foot">versionCode = {{ CURRENT_CLIENT.versionCode }}</div>
        </div>
      </a-col>
      <a-col :xs="24" :md="12">
        <div class="metric-card">
          <div class="metric-label"><Icons.CloudDownloadOutlined /> 服务端最新版本</div>
          <div class="metric-value" :style="{ color: clientHasNewer ? brand.orange : brand.success }">
            {{ latestPublished?.versionName ?? '—' }}
          </div>
          <div class="metric-foot">
            {{
              !latestPublished
                ? '还没有发布任何版本'
                : clientHasNewer
                  ? `客户端比它旧，会提示更新（versionCode ${latestPublished.versionCode}）`
                  : '客户端已是最新'
            }}
          </div>
        </div>
      </a-col>
    </a-row>

    <a-card :bordered="false" :body-style="{ padding: '8px 20px 20px' }">
      <a-table
        :data-source="list"
        :loading="loading"
        :row-key="(r: AppRelease) => r.id"
        :pagination="false"
      >
        <a-table-column title="版本" key="version" :width="180">
          <template #default="{ record }">
            <div class="ver-name">
              {{ record.versionName }}
              <a-tag v-if="record.forceUpdate" color="red" style="margin-left: 6px">强制</a-tag>
            </div>
            <div class="ver-code">versionCode {{ record.versionCode }}</div>
          </template>
        </a-table-column>

        <a-table-column title="更新说明" key="changelog">
          <template #default="{ record }">
            <div class="changelog">{{ record.changelog || '—' }}</div>
          </template>
        </a-table-column>

        <a-table-column title="状态" key="published" :width="110" align="center">
          <template #default="{ record }">
            <a-tag :color="record.published ? 'green' : 'default'">
              {{ record.published ? '已发布' : '未发布' }}
            </a-tag>
          </template>
        </a-table-column>

        <a-table-column title="时间" key="time" :width="130">
          <template #default="{ record }">
            {{ dayjs(record.createdAt).format('YYYY-MM-DD HH:mm') }}
          </template>
        </a-table-column>

        <a-table-column title="操作" key="action" :width="180" align="right">
          <template #default="{ record }">
            <a-button v-if="!record.published" type="link" size="small" @click="publish(record)">
              上线
            </a-button>
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" size="small" danger @click="remove(record)">删除</a-button>
          </template>
        </a-table-column>

        <template #emptyText><a-empty description="还没有发布过版本" /></template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑版本' : '发布新版本'"
      :width="620"
      :confirm-loading="saving"
      ok-text="保存"
      cancel-text="取消"
      destroy-on-close
      @ok="submit"
    >
      <a-form :model="form" layout="vertical" style="margin-top: 8px">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="版本号（versionCode）">
              <a-input-number v-model:value="form.versionCode" :min="1" style="width: 100%" />
              <div class="tip">客户端靠它的大小判断有没有新版，必须比上一版大</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="版本名（versionName）">
              <a-input v-model:value="form.versionName" placeholder="例如：1.1.0" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="更新说明">
          <a-textarea
            v-model:value="form.changelog"
            :rows="4"
            :maxlength="500"
            show-count
            placeholder="一行一条，例如：&#10;新增消息中心&#10;修复了若干问题"
          />
        </a-form-item>

        <a-form-item label="安装包下载地址">
          <a-input v-model:value="form.downloadUrl" placeholder="https://... 或 /uploads/xxx.apk" />
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="强制更新">
              <a-switch v-model:checked="form.forceUpdate" />
              <div class="tip">开启后客户端会弹不可取消的提示</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="立即发布">
              <a-switch v-model:checked="form.published" />
              <div class="tip">关掉就先存着，之后可以点「上线」</div>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.ver-name {
  font-weight: 600;
  display: flex;
  align-items: center;
}

.ver-code {
  font-size: 12px;
  color: #9a9aa8;
  margin-top: 2px;
}

.changelog {
  color: #5a5a6b;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  max-width: 460px;
}

.tip {
  font-size: 12px;
  color: #b3b3c0;
  margin-top: 6px;
  line-height: 1.5;
}
</style>
