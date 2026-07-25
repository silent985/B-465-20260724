import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 可以在这里添加 token
    const user = localStorage.getItem('lottery_user')
    if (user) {
      const userData = JSON.parse(user)
      config.headers['X-User-Id'] = userData.id
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    console.error('响应错误:', error)
    const message = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

// ==================== 抽奖相关 API ====================

/**
 * 执行抽奖
 */
export const drawLottery = (userId) => {
  return api.post(`/lottery/draw/${userId}`)
}

/**
 * 获取用户抽奖记录
 */
export const getUserRecords = (userId, page = 0, size = 10) => {
  return api.get(`/lottery/records/${userId}`, { params: { page, size } })
}

/**
 * 获取最近中奖记录
 */
export const getRecentWinners = (limit = 10) => {
  return api.get('/lottery/recent-winners', { params: { limit } })
}

/**
 * 领取奖品
 */
export const claimPrize = (recordId, userId) => {
  return api.post(`/lottery/claim/${recordId}`, null, { params: { userId } })
}

/**
 * 获取抽奖统计
 */
export const getStats = () => {
  return api.get('/lottery/stats')
}

// ==================== 奖品相关 API ====================

/**
 * 获取所有奖品
 */
export const getAllPrizes = () => {
  return api.get('/prizes')
}

/**
 * 获取启用的奖品
 */
export const getEnabledPrizes = () => {
  return api.get('/prizes/enabled')
}

/**
 * 获取奖品详情
 */
export const getPrizeById = (id) => {
  return api.get(`/prizes/${id}`)
}

/**
 * 创建奖品
 */
export const createPrize = (data) => {
  return api.post('/prizes', data)
}

/**
 * 更新奖品
 */
export const updatePrize = (id, data) => {
  return api.put(`/prizes/${id}`, data)
}

/**
 * 删除奖品
 */
export const deletePrize = (id) => {
  return api.delete(`/prizes/${id}`)
}

/**
 * 切换奖品状态
 */
export const togglePrize = (id) => {
  return api.patch(`/prizes/${id}/toggle`)
}

// ==================== 用户相关 API ====================

/**
 * 用户登录
 */
export const login = (username, password) => {
  return api.post('/users/login', null, { params: { username, password } })
}

/**
 * 用户注册
 */
export const register = (username, password, nickname, phone) => {
  return api.post('/users/register', null, { 
    params: { username, password, nickname, phone } 
  })
}

/**
 * 获取用户信息
 */
export const getUserById = (id) => {
  return api.get(`/users/${id}`)
}

/**
 * 获取所有用户
 */
export const getAllUsers = (page = 0, size = 10) => {
  return api.get('/users', { params: { page, size } })
}

/**
 * 更新用户信息
 */
export const updateUser = (id, data) => {
  return api.put(`/users/${id}`, data)
}

/**
 * 增加抽奖次数
 */
export const addChances = (userId, count) => {
  return api.post(`/users/${userId}/add-chances`, null, { params: { count } })
}

/**
 * 切换用户状态
 */
export const toggleUser = (id) => {
  return api.patch(`/users/${id}/toggle`)
}

/**
 * 删除用户
 */
export const deleteUser = (id) => {
  return api.delete(`/users/${id}`)
}

/**
 * 每日签到
 */
export const dailyCheckin = (userId) => {
  return api.post(`/users/${userId}/checkin`)
}

/**
 * 获取签到状态
 */
export const getCheckinStatus = (userId) => {
  return api.get(`/users/${userId}/checkin-status`)
}

export default api
