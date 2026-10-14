<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import * as Icons from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

// 默认填上种子数据里的账号，方便本地一把登进去
const form = reactive({ username: 'admin', password: 'admin123' })
const loading = ref(false)

async function handleSubmit() {
  if (!form.username || !form.password) {
    message.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const user = await auth.login(form.username, form.password)
    message.success(`欢迎回来，${user?.nickname || user?.username || ''}`)
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.replace(redirect)
  } catch {
    /* 错误提示已由拦截器统一处理 */
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login">
    <div class="login-visual">
      <div class="visual-inner">
        <div class="visual-logo">
          <Icons.DeploymentUnitOutlined />
        </div>
        <h1 class="visual-title">Keel</h1>
        <p class="visual-sub">Keel Stack · 三端工程骨架</p>

        <ul class="visual-points">
          <li><Icons.CheckCircleFilled /> 路由、登录守卫、请求层开箱即用</li>
          <li><Icons.CheckCircleFilled /> 实时事件自动刷新列表，不用轮询</li>
          <li><Icons.CheckCircleFilled /> 媒体库、消息推送、版本管理已备好</li>
        </ul>

        <div class="visual-foot">Vue 3 + TypeScript + Ant Design Vue</div>
      </div>
      <div class="visual-glow" />
      <div class="visual-glow two" />
    </div>

    <div class="login-form-wrap">
      <div class="login-form">
        <h2 class="form-title">登录控制台</h2>
        <p class="form-sub">管理条目、媒体与消息推送</p>

        <a-form layout="vertical" @submit.prevent="handleSubmit">
          <a-form-item label="账号">
            <a-input v-model:value="form.username" size="large" placeholder="请输入账号" allow-clear>
              <template #prefix><Icons.UserOutlined style="color: #b3b3c0" /></template>
            </a-input>
          </a-form-item>

          <a-form-item label="密码">
            <a-input-password
              v-model:value="form.password"
              size="large"
              placeholder="请输入密码"
              @press-enter="handleSubmit"
            >
              <template #prefix><Icons.LockOutlined style="color: #b3b3c0" /></template>
            </a-input-password>
          </a-form-item>

          <a-button
            type="primary"
            size="large"
            block
            :loading="loading"
            style="margin-top: 6px; height: 46px"
            @click="handleSubmit"
          >
            登 录
          </a-button>
        </a-form>

        <div class="form-tip">
          <Icons.InfoCircleOutlined />
          默认账号 <code>admin</code> / <code>admin123</code>，登录后可在系统设置里修改
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  min-height: 100vh;
}

.login-visual {
  position: relative;
  overflow: hidden;
  background: linear-gradient(150deg, #ff8a58 0%, #ff6b35 42%, #e5461a 100%);
  color: #fff;
  display: flex;
  align-items: center;
  padding: 60px 64px;
}

.visual-inner {
  position: relative;
  z-index: 2;
  max-width: 420px;
}

.visual-logo {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  font-size: 26px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.34);
}

.visual-title {
  font-size: 40px;
  font-weight: 700;
  margin: 26px 0 6px;
  letter-spacing: -1px;
}

.visual-sub {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
}

.visual-points {
  list-style: none;
  padding: 0;
  margin: 40px 0 0;
  display: grid;
  gap: 14px;
  font-size: 14px;
}

.visual-points li {
  display: flex;
  align-items: center;
  gap: 10px;
  opacity: 0.95;
}

.visual-points svg {
  opacity: 0.9;
}

.visual-foot {
  margin-top: 48px;
  font-size: 12px;
  opacity: 0.66;
}

.visual-glow {
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  filter: blur(70px);
  right: -120px;
  top: -80px;
  z-index: 1;
}

.visual-glow.two {
  right: auto;
  left: -140px;
  top: auto;
  bottom: -160px;
  width: 360px;
  height: 360px;
  background: rgba(255, 190, 63, 0.3);
}

.login-form-wrap {
  display: grid;
  place-items: center;
  padding: 40px;
  background: #fff;
}

.login-form {
  width: 100%;
  max-width: 360px;
}

.form-title {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 6px;
  letter-spacing: -0.5px;
}

.form-sub {
  color: #8a8a99;
  font-size: 13px;
  margin: 0 0 28px;
}

.form-tip {
  margin-top: 22px;
  font-size: 12px;
  color: #9a9aa8;
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.7;
}

.form-tip code {
  background: rgba(23, 23, 37, 0.05);
  padding: 1px 6px;
  border-radius: 5px;
  font-size: 12px;
}

@media (max-width: 900px) {
  .login {
    grid-template-columns: 1fr;
  }

  .login-visual {
    display: none;
  }
}
</style>
