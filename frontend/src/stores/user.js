import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, getUserById } from '@/api'

export const useUserStore = defineStore('user', () => {
  // 状态
  const user = ref(null)
  const loading = ref(false)

  // 计算属性
  const isLoggedIn = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const remainingChances = computed(() => user.value?.remainingChances || 0)

  // 初始化 - 从本地存储恢复
  const init = () => {
    const savedUser = localStorage.getItem('lottery_user')
    if (savedUser) {
      user.value = JSON.parse(savedUser)
      // 刷新用户信息
      refreshUser()
    }
  }

  // 登录
  const login = async (username, password) => {
    loading.value = true
    try {
      const res = await loginApi(username, password)
      user.value = res.data
      localStorage.setItem('lottery_user', JSON.stringify(res.data))
      return res.data
    } finally {
      loading.value = false
    }
  }

  // 注册
  const register = async (username, password, nickname, phone) => {
    loading.value = true
    try {
      const res = await registerApi(username, password, nickname, phone)
      user.value = res.data
      localStorage.setItem('lottery_user', JSON.stringify(res.data))
      return res.data
    } finally {
      loading.value = false
    }
  }

  // 刷新用户信息
  const refreshUser = async () => {
    if (!user.value?.id) return
    try {
      const res = await getUserById(user.value.id)
      user.value = res.data
      localStorage.setItem('lottery_user', JSON.stringify(res.data))
    } catch (e) {
      // 如果获取失败，可能是 token 过期，清除登录状态
      logout()
    }
  }

  // 更新剩余次数
  const updateChances = (chances) => {
    if (user.value) {
      user.value.remainingChances = chances
      localStorage.setItem('lottery_user', JSON.stringify(user.value))
    }
  }

  // 登出
  const logout = () => {
    user.value = null
    localStorage.removeItem('lottery_user')
  }

  // 初始化
  init()

  return {
    user,
    loading,
    isLoggedIn,
    isAdmin,
    remainingChances,
    login,
    register,
    logout,
    refreshUser,
    updateChances
  }
})
