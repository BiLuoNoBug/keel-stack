<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import * as Icons from '@ant-design/icons-vue'
import { mediaApi } from '@/api'
import type { Media } from '@/types'

/**
 * 通用图片选择器：v-model 绑定一个图片 url。
 * 既可以从媒体库里挑，也可以就地传一张新的，选中后回写 url。
 */
const props = defineProps<{ modelValue?: string | null; title?: string }>()
const emit = defineEmits<{ 'update:modelValue': [string | null] }>()

const open = ref(false)
const loading = ref(false)
const uploading = ref(false)
const list = ref<Media[]>([])

async function loadMedia() {
  loading.value = true
  try {
    const res = await mediaApi.list({ pageSize: 100 })
    list.value = res.list
  } finally {
    loading.value = false
  }
}

function show() {
  open.value = true
  loadMedia()
}

function pick(m: Media) {
  emit('update:modelValue', m.url)
  open.value = false
  message.success('已选择这张图')
}

function clear() {
  emit('update:modelValue', null)
}

async function onBeforeUpload(file: File) {
  uploading.value = true
  try {
    const media = await mediaApi.upload(file)
    message.success('上传成功')
    await loadMedia()
    emit('update:modelValue', media.url)
  } catch {
    /* 拦截器已提示 */
  } finally {
    uploading.value = false
  }
  // 返回 false 阻止 antd 的默认上传行为，改由上面的接口处理
  return false
}

watch(() => props.modelValue, () => { /* 仅外部变化时同步预览 */ })
</script>

<template>
  <div class="picker">
    <div v-if="modelValue" class="picker-preview">
      <img :src="modelValue" alt="" />
      <div class="picker-mask">
        <a-button size="small" type="primary" ghost @click="show">
          <Icons.SwapOutlined /> 更换
        </a-button>
        <a-button size="small" danger ghost @click="clear">
          <Icons.DeleteOutlined /> 移除
        </a-button>
      </div>
    </div>
    <a-button v-else block class="picker-empty" @click="show">
      <Icons.PlusOutlined /> 选择图片
    </a-button>
    <div class="picker-hint">建议 4:3，JPG / PNG，10MB 以内</div>

    <a-modal
      v-model:open="open"
      :title="title || '从媒体库选择图片'"
      :width="760"
      :footer="null"
      destroy-on-close
    >
      <div class="picker-toolbar">
        <a-upload :show-upload-list="false" accept="image/*" :before-upload="onBeforeUpload">
          <a-button type="primary" :loading="uploading">
            <Icons.UploadOutlined /> 上传新图
          </a-button>
        </a-upload>
        <span class="picker-count">共 {{ list.length }} 张</span>
      </div>

      <a-spin :spinning="loading">
        <a-empty v-if="!list.length" description="媒体库还是空的，先上传一张吧" />
        <div v-else class="picker-grid">
          <div
            v-for="m in list"
            :key="m.id"
            class="picker-tile"
            :class="{ active: m.url === modelValue }"
            @click="pick(m)"
          >
            <img :src="m.url" alt="" />
            <div class="picker-tile-name">{{ m.filename }}</div>
            <Icons.CheckCircleFilled v-if="m.url === modelValue" class="picker-check" />
          </div>
        </div>
      </a-spin>
    </a-modal>
  </div>
</template>

<style scoped>
.picker-preview {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #eeeef3;
}

.picker-preview img {
  width: 100%;
  height: 148px;
  object-fit: cover;
  display: block;
}

.picker-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(23, 23, 37, 0.46);
  opacity: 0;
  transition: opacity 0.18s ease;
}

.picker-preview:hover .picker-mask {
  opacity: 1;
}

.picker-empty {
  height: 148px;
  border-style: dashed;
  border-radius: 14px;
  color: #9a9aa8;
}

.picker-hint {
  font-size: 12px;
  color: #b3b3c0;
  margin-top: 6px;
}

.picker-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.picker-count {
  font-size: 12px;
  color: #9a9aa8;
}

.picker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(132px, 1fr));
  gap: 12px;
  max-height: 460px;
  overflow-y: auto;
  padding: 2px;
}

.picker-tile {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  background: #fafafc;
  transition: border-color 0.16s ease, transform 0.16s ease;
}

.picker-tile:hover {
  transform: translateY(-2px);
}

.picker-tile.active {
  border-color: #ff6b35;
}

.picker-tile img {
  width: 100%;
  height: 96px;
  object-fit: cover;
  display: block;
}

.picker-tile-name {
  font-size: 11px;
  color: #9a9aa8;
  padding: 5px 7px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.picker-check {
  position: absolute;
  top: 6px;
  right: 6px;
  color: #ff6b35;
  background: #fff;
  border-radius: 50%;
  font-size: 16px;
}
</style>
