<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar glass-card">
      <div class="sidebar-header">
        <router-link to="/" class="logo">
          <span class="logo-icon">🎰</span>
          <span class="logo-text">管理后台</span>
        </router-link>
      </div>
      
      <nav class="sidebar-nav">
        <router-link 
          v-for="item in menuItems" 
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: $route.path === item.path }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <router-link to="/" class="back-link">
          <el-icon><Back /></el-icon>
          返回前台
        </router-link>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-area">
      <header class="top-bar glass-card">
        <h1>{{ currentTitle }}</h1>
        <div class="user-area">
          <el-avatar :size="36">{{ userStore.user?.nickname?.[0] || 'A' }}</el-avatar>
          <span>{{ userStore.user?.nickname || 'Admin' }}</span>
        </div>
      </header>

      <div class="content-area">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Trophy, User, List, Back } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const menuItems = [
  { path: '/admin/prizes', title: '奖品管理', icon: Trophy },
  { path: '/admin/users', title: '用户管理', icon: User },
  { path: '/admin/records', title: '记录管理', icon: List }
]

const currentTitle = computed(() => {
  const item = menuItems.find(m => m.path === route.path)
  return item?.title || '管理后台'
})
</script>

<style lang="scss" scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 240px;
  display: flex;
  flex-direction: column;
  border-radius: 0;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid var(--border-color);
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  
  .logo-icon {
    font-size: 28px;
  }
  
  .logo-text {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
  }
}

.sidebar-nav {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: var(--text-primary);
  }
  
  &.active {
    background: var(--gradient-primary);
    color: white;
  }
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--border-color);
}

.back-link {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  text-decoration: none;
  padding: 12px;
  border-radius: var(--radius-md);
  transition: all 0.3s;
  
  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: var(--text-primary);
  }
}

.main-area {
  flex: 1;
  margin-left: 240px;
  display: flex;
  flex-direction: column;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  margin: 16px 16px 0;
  border-radius: var(--radius-lg);
  
  h1 {
    font-size: 20px;
  }
}

.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.content-area {
  flex: 1;
  padding: 16px;
}
</style>
