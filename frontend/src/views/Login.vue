<template>
  <div class="login-page">
    <!-- 背景 -->
    <div class="bg-pattern"></div>

    <!-- 登录卡片 -->
    <div class="login-card glass-card">
      <div class="card-header">
        <div class="logo">🎰</div>
        <h1>幸运大转盘</h1>
        <p>登录账户，开启幸运之旅</p>
      </div>

      <!-- 登录/注册切换 -->
      <div class="tab-switch">
        <button 
          :class="{ active: activeTab === 'login' }"
          @click="activeTab = 'login'"
        >
          登录
        </button>
        <button 
          :class="{ active: activeTab === 'register' }"
          @click="activeTab = 'register'"
        >
          注册
        </button>
        <div class="tab-indicator" :class="activeTab"></div>
      </div>

      <!-- 登录表单 -->
      <el-form 
        v-if="activeTab === 'login'"
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            class="submit-btn"
            :loading="loading"
            native-type="submit"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 注册表单 -->
      <el-form 
        v-else
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        size="large"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input 
            v-model="registerForm.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input 
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input 
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input 
            v-model="registerForm.nickname"
            placeholder="请输入昵称（选填）"
            :prefix-icon="UserFilled"
          />
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            class="submit-btn"
            :loading="loading"
            native-type="submit"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 快捷登录提示 -->
      <div class="quick-login">
        <p>测试账号: admin / admin123</p>
      </div>

      <!-- 返回首页 -->
      <div class="back-home">
        <router-link to="/">← 返回首页</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)

// 登录表单
const loginFormRef = ref()
const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

// 注册表单
const registerFormRef = ref()
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: ''
})

// 密码确认验证
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度在 3-50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 登录
const handleLogin = async () => {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(loginForm.username, loginForm.password)
    ElMessage.success('登录成功！')
    router.push('/')
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

// 注册
const handleRegister = async () => {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.register(
      registerForm.username,
      registerForm.password,
      registerForm.nickname || undefined,
      undefined
    )
    ElMessage.success('注册成功！')
    router.push('/')
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
}

.bg-pattern {
  position: fixed;
  inset: 0;
  background: 
    radial-gradient(circle at 20% 80%, rgba(120, 119, 198, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(102, 126, 234, 0.3) 0%, transparent 50%);
  pointer-events: none;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  position: relative;
  z-index: 1;
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
  
  .logo {
    font-size: 48px;
    margin-bottom: 16px;
  }
  
  h1 {
    font-size: 28px;
    margin-bottom: 8px;
    background: var(--gradient-gold);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  
  p {
    color: var(--text-secondary);
  }
}

.tab-switch {
  display: flex;
  position: relative;
  margin-bottom: 32px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-full);
  padding: 4px;
  
  button {
    flex: 1;
    padding: 12px;
    background: none;
    border: none;
    color: var(--text-secondary);
    font-size: 16px;
    cursor: pointer;
    position: relative;
    z-index: 1;
    transition: color 0.3s;
    
    &.active {
      color: white;
    }
  }
  
  .tab-indicator {
    position: absolute;
    top: 4px;
    left: 4px;
    width: calc(50% - 4px);
    height: calc(100% - 8px);
    background: var(--gradient-primary);
    border-radius: var(--radius-full);
    transition: transform 0.3s ease;
    
    &.register {
      transform: translateX(100%);
    }
  }
}

.el-form-item {
  margin-bottom: 20px;
}

:deep(.el-input__wrapper) {
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: none !important;
  padding: 8px 16px;
  border-radius: 12px;
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba(0, 0, 0, 0.3);
    border-color: rgba(255, 255, 255, 0.2);
  }

  &.is-focus {
    background: rgba(0, 0, 0, 0.4);
    border-color: var(--primary-color);
    box-shadow: 0 0 0 3px rgba(6, 182, 212, 0.1) !important;
  }
  
  .el-input__inner {
    color: white;
    height: 32px;
    font-size: 15px;
    
    &::placeholder {
      color: rgba(255, 255, 255, 0.3);
    }
  }
  
  .el-input__prefix {
    color: var(--text-muted);
    font-size: 18px;
    margin-right: 8px;
  }
}

.submit-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  margin-top: 8px;
  letter-spacing: 1px;
}

.quick-login {
  text-align: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
  
  p {
    color: var(--text-muted);
    font-size: 13px;
  }
}

.back-home {
  text-align: center;
  margin-top: 20px;
  
  a {
    color: var(--text-secondary);
    text-decoration: none;
    transition: color 0.3s;
    
    &:hover {
      color: var(--text-primary);
    }
  }
}
</style>
