<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import * as Icons from '@ant-design/icons-vue'
import { itemApi } from '@/api'
import ImagePicker from '@/components/ImagePicker.vue'
import { itemStatusText, priorityColor, priorityText } from '@/theme/tokens'
import { useRealtimeStore } from '@/stores/realtime'
import type { Item, ItemPriority, ItemStatus } from '@/types'

/**
 * 示例模块：item（条目）。
 *
 * 这个页面演示「标准 CRUD + 状态流转 + 实时刷新」的写法：
 * 新增自己的业务模块时，把 item 换成你的领域名词，接口和字段按需替换，
 * 其余骨架（分页、筛选、弹窗表单、实时 watch）都可以照搬。
 */
const realtime = useRealtimeStore()

const loading = ref(false)
const list = ref<Item[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const category = ref<string | undefined>(undefined)
const status = ref<string | undefined>(undefined)
const published = ref<string | undefined>(undefined)

const priorityOptions = [
  { label: '低', value: 'LOW' },
  { label: '普通', value: 'NORMAL' },
  { label: '高', value: 'HIGH' }
]

const statusFilterOptions = [
  { label: '全部状态', value: '' },
  { label: '待处理', value: 'TODO' },
  { label: '进行中', value: 'DOING' },
  { label: '已完成', value: 'DONE' }
]

/** 分类筛选候选：现有条目的分类去重 */
const categories = computed(() => {
  const set = new Set(list.value.map((i) => i.category))
  return [...set].map((c) => ({ label: c, value: c }))
})

/** 编辑时的分类候选：几个常用值 + 现有分类，也可直接输入新分类 */
const categoryOptions = computed(() => {
  const base = ['准备工作', '搭建', '发布']
  const set = new Set<string>([...base, ...list.value.map((i) => i.category)])
  return [...set].map((c) => ({ value: c, label: c }))
})

/* -------------------------------------------------- 表单 */
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const form = reactive({
  title: '',
  category: '准备工作',
  content: '',
  coverUrl: null as string | null,
  priority: 'NORMAL' as ItemPriority,
  published: true,
  sortOrder: 0
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择或输入分类', trigger: 'change' }]
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    title: '',
    category: '准备工作',
    content: '',
    coverUrl: null,
    priority: 'NORMAL',
    published: true,
    sortOrder: list.value.length + 1
  })
}

async function load() {
  loading.value = true
  try {
    const res = await itemApi.list({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      category: category.value,
      status: status.value,
      published: published.value
    })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch([page, pageSize], load)
// 两端任何一侧改动条目，列表自动跟着刷新
watch(() => realtime.tick, () => {
  if (realtime.lastEventType === 'item:changed' || realtime.lastEventType === 'item:status') load()
})

function openCreate() {
  resetForm()
  modalOpen.value = true
}

function openEdit(record: Item) {
  editingId.value = record.id
  Object.assign(form, {
    title: record.title,
    category: record.category,
    content: record.content,
    coverUrl: record.coverUrl ?? null,
    priority: record.priority,
    published: record.published,
    sortOrder: record.sortOrder
  })
  modalOpen.value = true
}

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (editingId.value) {
      await itemApi.update(editingId.value, payload)
      message.success('已保存')
    } else {
      await itemApi.create(payload)
      message.success('已添加')
    }
    modalOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

/** 状态流转下拉：只给出「当前状态 + 后端允许的下一步」 */
function statusChoices(record: Item) {
  const opts = [{ label: record.statusText, value: record.status }]
  for (const next of record.allowNext) {
    if (next !== record.status) opts.push({ label: itemStatusText[next] ?? next, value: next })
  }
  return opts
}

async function changeStatus(record: Item, next: ItemStatus) {
  if (next === record.status) return
  const updated = await itemApi.setStatus(record.id, next)
  Object.assign(record, updated)
  message.success(`已更新为「${updated.statusText}」`)
}

async function togglePublished(record: Item, checked: boolean) {
  const updated = await itemApi.setPublished(record.id, checked)
  Object.assign(record, updated)
  message.success(checked ? '已发布' : '已下架')
}

function confirmRemove(record: Item) {
  Modal.confirm({
    title: `删除「${record.title}」？`,
    content: '删除后客户端会立刻看不到这条条目。',
    okText: '删除',
    okButtonProps: { danger: true },
    cancelText: '算了',
    async onOk() {
      await itemApi.remove(record.id)
      message.success('已删除')
      await load()
    }
  })
}

function search() {
  page.value = 1
  load()
}

function reset() {
  keyword.value = ''
  category.value = undefined
  status.value = undefined
  published.value = undefined
  page.value = 1
  load()
}
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">条目管理</h2>
        <div class="page-sub">共 {{ total }} 条 · 示例模块，收到实时事件会自动刷新</div>
      </div>
      <a-space>
        <a-input
          v-model:value="keyword"
          placeholder="搜标题 / 内容"
          allow-clear
          style="width: 170px"
          @press-enter="search"
        >
          <template #prefix><Icons.SearchOutlined style="color: #b3b3c0" /></template>
        </a-input>
        <a-select
          v-model:value="category"
          :options="categories"
          placeholder="全部分类"
          allow-clear
          style="width: 130px"
          @change="search"
        />
        <a-select
          v-model:value="status"
          :options="statusFilterOptions"
          style="width: 130px"
          @change="search"
        />
        <a-select
          v-model:value="published"
          :options="[
            { label: '全部发布状态', value: '' },
            { label: '已发布', value: 'true' },
            { label: '未发布', value: 'false' }
          ]"
          style="width: 150px"
          @change="search"
        />
        <a-button @click="reset">重置</a-button>
        <a-button type="primary" @click="openCreate">
          <Icons.PlusOutlined /> 新增条目
        </a-button>
      </a-space>
    </div>

    <a-card :bordered="false" :body-style="{ padding: '8px 20px 20px' }">
      <a-table
        :data-source="list"
        :loading="loading"
        :row-key="(r: Item) => r.id"
        :pagination="{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`
        }"
        @change="(p: any) => { page = p.current; pageSize = p.pageSize }"
      >
        <a-table-column title="条目" key="item" :width="300">
          <template #default="{ record }">
            <div class="item-cell">
              <img v-if="record.coverUrl" :src="record.coverUrl" class="item-cover" alt="" />
              <div v-else class="item-cover empty"><Icons.PictureOutlined /></div>
              <div class="item-main">
                <div class="item-title">
                  {{ record.title }}
                  <a-tag v-if="!record.published" color="default" style="margin-left: 6px">未发布</a-tag>
                </div>
                <div class="item-desc">{{ record.content || '还没有写内容' }}</div>
              </div>
            </div>
          </template>
        </a-table-column>

        <a-table-column title="分类" data-index="category" :width="110" />

        <a-table-column title="优先级" key="priority" :width="90" align="center">
          <template #default="{ record }">
            <a-tag :color="priorityColor[record.priority]">{{ priorityText[record.priority] }}</a-tag>
          </template>
        </a-table-column>

        <a-table-column title="状态" key="status" :width="130" align="center">
          <template #default="{ record }">
            <a-select
              :value="record.status"
              :options="statusChoices(record)"
              size="small"
              style="width: 108px"
              @change="(v: any) => changeStatus(record, v as ItemStatus)"
            />
          </template>
        </a-table-column>

        <a-table-column title="排序" data-index="sortOrder" :width="80" align="center" />

        <a-table-column title="发布" key="published" :width="90" align="center">
          <template #default="{ record }">
            <a-switch
              :checked="record.published"
              size="small"
              @change="(v: any) => togglePublished(record, !!v)"
            />
          </template>
        </a-table-column>

        <a-table-column title="操作" key="action" :width="130" align="right">
          <template #default="{ record }">
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" size="small" danger @click="confirmRemove(record)">删除</a-button>
          </template>
        </a-table-column>

        <template #emptyText><a-empty description="还没有条目，点右上角新增" /></template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑条目' : '新增条目'"
      :width="680"
      :confirm-loading="saving"
      ok-text="保存"
      cancel-text="取消"
      destroy-on-close
      @ok="submit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" style="margin-top: 8px">
        <a-row :gutter="16">
          <a-col :span="10">
            <a-form-item label="封面图">
              <ImagePicker v-model="form.coverUrl" title="为这条条目选一张图" />
            </a-form-item>
          </a-col>
          <a-col :span="14">
            <a-form-item label="标题" name="title">
              <a-input v-model:value="form.title" placeholder="例如：接上实时通道" />
            </a-form-item>
            <a-form-item label="分类" name="category">
              <!-- 可自由输入：既能选已有分类，也能直接打字新建 -->
              <a-auto-complete
                v-model:value="form.category"
                :options="categoryOptions"
                :filter-option="false"
                placeholder="选择已有分类，或直接输入新分类"
              />
            </a-form-item>
            <a-form-item label="优先级">
              <a-select v-model:value="form.priority" :options="priorityOptions" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="内容">
          <a-textarea
            v-model:value="form.content"
            :rows="4"
            :maxlength="1000"
            show-count
            placeholder="一句话说明这条条目要做什么"
          />
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="排序">
              <a-input-number v-model:value="form.sortOrder" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="是否发布">
              <a-switch v-model:checked="form.published" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.item-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-cover {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  object-fit: cover;
  flex: none;
}

.item-cover.empty {
  display: grid;
  place-items: center;
  background: #f4f4f8;
  color: #c3c3ce;
  font-size: 20px;
}

.item-main {
  min-width: 0;
}

.item-title {
  font-weight: 600;
  display: flex;
  align-items: center;
}

.item-desc {
  font-size: 12px;
  color: #9a9aa8;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 220px;
}
</style>
