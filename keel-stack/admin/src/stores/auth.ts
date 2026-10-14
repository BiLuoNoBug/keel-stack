import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '@/api'
import { clearToken, getToken, setToken } from '@/api/client'
import type { AdminUser } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const user = ref<AdminUser | null>(null)
  const loading = ref(false)

  const logged = computed(() => !!token.value)

  async function login(username: string, password: string) {
    loading.value = true
    try {
      const data = await authApi.login({ username, password })
      token.value = data.token
      setToken(data.token)
      user.value = data.user
      return data.user
    } finally {
      loading.value = false
    }
  }

  /** 刷新页面后 token 还在但内存里没有用户信息，用它补一次 */
  async function fetchProfile() {
    if (!token.value) return null
    user.value = await authApi.profile()
    return user.value
  }

  function logout() {
    token.value = ''
    user.value = null
    clearToken()
  }

  return { token, user, loading, logged, login, logout, fetchProfile }
})
