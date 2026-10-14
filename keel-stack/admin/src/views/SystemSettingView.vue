<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { authApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useRealtimeStore } from '@/stores/realtime'

const auth = useAuthStore()
const realtime = useRealtimeStore()

const savingProfile = ref(false)
const savingPassword = ref(false)
const profileForm = reactive({ nickname: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

// 当前服务地址：客户端对接时填这里
const env = {
  apiBase: location.origin + '/api',
  wsPath: '/socket.io',
  appHint: '客户端里填这个地址即可连接到本服务'
}

onMounted(() => {
  profileForm.nickname = auth.user?.nickname || ''
})

async function saveProfile() {
  savingProfile.value = true
  try {
    await authApi.updateProfile({ nickname: profileForm.nickname })
    await auth.fetchProfile()
    message.success('已保存')
  } finally {
    savingProfile.value = false
  }
}

async function changePassword() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    message.warning('请填写完整')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    message.warning('两次输入的新密码不一致')
    return
  }
  if (pwdForm.newPassword.length < 6) {
    message.warning('新密码至少 6 位')
    return
  }
  savingPassword.value = true
  try {
    await authApi.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    message.success('密码已修改，下次登录用新密码')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirm = ''
  } finally {
    savingPassword.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">系统设置</h2>
        <div class="page-sub">账号、服务地址与运行状态</div>
      </div>
    </div>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :xl="14">
        <a-card :bordered="false" title="管理账号">
          <a-form layout="vertical" style="max-width: 420px">
            <a-form-item label="登录账号">
              <a-input :value="auth.user?.username" disabled />
            </a-form-item>
            <a-form-item label="昵称">
              <a-input v-model:value="profileForm.nickname" placeholder="显示在控制台右上角" />
            </a-form-item>
            <a-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</a-button>
          </a-form>

          <a-divider orientation="left" style="font-size: 13px; color: #8a8a99">修改密码</a-divider>

          <a-form layout="vertical" style="max-width: 420px">
            <a-form-item label="当前密码">
              <a-input-password v-model:value="pwdForm.oldPassword" placeholder="请输入当前密码" />
            </a-form-item>
            <a-form-item label="新密码">
              <a-input-password v-model:value="pwdForm.newPassword" placeholder="至少 6 位" />
            </a-form-item>
            <a-form-item label="确认新密码">
              <a-input-password
                v-model:value="pwdForm.confirm"
                placeholder="再输一次"
                @press-enter="changePassword"
              />
            </a-form-item>
            <a-button :loading="savingPassword" @click="changePassword">修改密码</a-button>
          </a-form>
        </a-card>
      </a-col>

      <a-col :xs="24" :xl="10">
        <a-card :bordered="false" title="服务地址">
          <div class="addr">
            <div class="addr-label">后端 API</div>
            <div class="addr-value">
              <code>{{ env.apiBase }}</code>
              <a-tag color="green" style="margin-left: 8px">运行中</a-tag>
            </div>
            <div class="addr-tip">{{ env.appHint }}</div>
          </div>

          <div class="addr" style="margin-top: 18px">
            <div class="addr-label">实时通道</div>
            <div class="addr-value">
              <code>{{ env.wsPath }}</code>
              <a-tag :color="realtime.connected ? 'green' : 'red'" style="margin-left: 8px">
                {{ realtime.connected ? '已连接' : '未连接' }}
              </a-tag>
            </div>
            <div class="addr-tip">条目变动、新消息、新版本都通过它即时下发</div>
          </div>

          <div class="addr" style="margin-top: 18px">
            <div class="addr-label">上传目录</div>
            <div class="addr-value"><code>server/uploads</code></div>
            <div class="addr-tip">图片上传后以 /uploads/xxx.jpg 对外提供</div>
          </div>
        </a-card>

        <a-card :bordered="false" title="关于" style="margin-top: 16px">
          <a-descriptions :column="1" size="small">
            <a-descriptions-item label="产品">Keel 1.0.0</a-descriptions-item>
            <a-descriptions-item label="控制台">Vue 3 + TypeScript + Ant Design Vue</a-descriptions-item>
            <a-descriptions-item label="后端">Node.js + Koa + MySQL + Redis + Socket.IO</a-descriptions-item>
            <a-descriptions-item label="上次登录">
              {{ auth.user?.lastLoginAt ? dayjs(auth.user.lastLoginAt).format('YYYY-MM-DD HH:mm') : '—' }}
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
.addr-label {
  font-size: 12px;
  color: #9a9aa8;
  margin-bottom: 6px;
}

.addr-value {
  display: flex;
  align-items: center;
  font-size: 13px;
}

.addr-value code {
  background: #f6f6fa;
  padding: 5px 9px;
  border-radius: 8px;
  font-size: 12px;
}

.addr-tip {
  font-size: 12px;
  color: #b3b3c0;
  margin-top: 8px;
  line-height: 1.6;
}
</style>
