<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import * as Icons from '@ant-design/icons-vue'
import { mediaApi } from '@/api'
import type { Media } from '@/types'

const loading = ref(false)
const uploading = ref(false)
const list = ref<Media[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(24)
const keyword = ref('')

const preview = ref<string | null>(null)

async function load() {
  loading.value = true
  try {
    const res = await mediaApi.list({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined
    })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function onBeforeUpload(file: File) {
  uploading.value = true
  try {
    await mediaApi.upload(file)
    message.success(`${file.name} 上传成功`)
    page.value = 1
    await load()
  } finally {
    uploading.value = false
  }
  // 返回 false 交给接口处理，不用 antd 自带的上传流程
  return false
}

async function copyUrl(m: Media) {
  const full = `${location.origin}${m.url}`
  try {
    await navigator.clipboard.writeText(full)
    message.success('图片地址已复制')
  } catch {
    message.info(full)
  }
}

function confirmRemove(m: Media) {
  // 还被引用时后端也会拦，这里先提示一下，少跑一次请求
  if (m.refCount > 0) {
    message.warning(`这张图还被 ${m.refCount} 处引用着，先换掉再删`)
    return
  }
  Modal.confirm({
    title: '删除这张图片？',
    content: m.filename,
    okText: '删除',
    okButtonProps: { danger: true },
    cancelText: '算了',
    async onOk() {
      await mediaApi.remove(m.id)
      message.success('已删除')
      await load()
    }
  })
}

function search() {
  page.value = 1
  load()
}
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">图片媒体库</h2>
        <div class="page-sub">共 {{ total }} 张 · 条目封面等都从这里取</div>
      </div>
      <a-space>
        <a-input
          v-model:value="keyword"
          placeholder="搜文件名"
          allow-clear
          style="width: 180px"
          @press-enter="search"
        >
          <template #prefix><Icons.SearchOutlined style="color: #b3b3c0" /></template>
        </a-input>
        <a-upload
          :show-upload-list="false"
          accept="image/*"
          multiple
          :before-upload="onBeforeUpload"
        >
          <a-button type="primary" :loading="uploading">
            <Icons.UploadOutlined /> 上传图片
          </a-button>
        </a-upload>
      </a-space>
    </div>

    <a-card :bordered="false">
      <a-spin :spinning="loading">
        <a-empty v-if="!list.length" description="还没有图片，点右上角上传" />
        <div v-else class="grid">
          <div v-for="m in list" :key="m.id" class="media-tile">
            <img :src="m.url" alt="" @click="preview = m.url" />
            <div class="tile-body">
              <div class="tile-name" :title="m.filename">{{ m.filename }}</div>
              <div class="tile-meta">
                <span v-if="m.refCount > 0" class="used">
                  <Icons.LinkOutlined /> 被引用 {{ m.refCount }}
                </span>
                <span v-else class="free">未使用</span>
              </div>
              <div class="tile-actions">
                <a-tooltip title="复制地址">
                  <a-button size="small" type="text" @click="copyUrl(m)">
                    <Icons.CopyOutlined />
                  </a-button>
                </a-tooltip>
                <a-tooltip :title="m.refCount > 0 ? '正在被引用，无法删除' : '删除'">
                  <a-button
                    size="small"
                    type="text"
                    danger
                    :disabled="m.refCount > 0"
                    @click="confirmRemove(m)"
                  >
                    <Icons.DeleteOutlined />
                  </a-button>
                </a-tooltip>
              </div>
            </div>
          </div>
        </div>
      </a-spin>

      <div v-if="total > pageSize" class="pager">
        <a-pagination
          v-model:current="page"
          v-model:page-size="pageSize"
          :total="total"
          :show-size-changer="true"
          :page-size-options="['12', '24', '48']"
          @change="load"
        />
      </div>
    </a-card>

    <a-modal
      :open="!!preview"
      :footer="null"
      :width="820"
      @cancel="preview = null"
    >
      <img v-if="preview" :src="preview" style="width: 100%; border-radius: 12px" alt="" />
    </a-modal>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 16px;
}

.tile-body {
  padding: 10px 12px 8px;
}

.tile-name {
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tile-meta {
  font-size: 11px;
  margin-top: 4px;
}

.used {
  color: #ff6b35;
}

.free {
  color: #b3b3c0;
}

.tile-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
