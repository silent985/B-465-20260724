<template>
  <div class="records-page">
    <!-- 头部 -->
    <header class="header glass-card">
      <router-link to="/" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        返回抽奖
      </router-link>
      <h1>🎊 我的抽奖记录</h1>
      <div class="placeholder"></div>
    </header>

    <!-- 内容区 -->
    <main class="main-content">
      <!-- 未登录提示 -->
      <div v-if="!userStore.isLoggedIn" class="not-logged-in glass-card">
        <el-empty description="请先登录查看抽奖记录">
          <el-button type="primary" @click="$router.push('/login')">
            去登录
          </el-button>
        </el-empty>
      </div>

      <!-- 记录列表 -->
      <template v-else>
        <div class="records-list" v-loading="loading">
          <div 
            v-for="record in records" 
            :key="record.id"
            class="record-card glass-card"
          >
            <div class="record-icon">
              {{ getPrizeIcon(record.prizeLevel) }}
            </div>
            <div class="record-info">
              <div class="prize-name">{{ record.prizeName }}</div>
              <div class="draw-time">{{ formatTime(record.drawTime) }}</div>
            </div>
            <div class="record-actions">
              <el-tag 
                v-if="record.claimed" 
                type="success" 
                size="small"
              >
                已领取
              </el-tag>
              <el-button 
                v-else-if="record.prizeLevel <= 4"
                type="primary" 
                size="small"
                @click="handleClaim(record)"
              >
                领取奖品
              </el-button>
            </div>
          </div>

          <!-- 空状态 -->
          <el-empty 
            v-if="!loading && records.length === 0" 
            description="暂无抽奖记录"
            class="glass-card"
          >
            <el-button type="primary" @click="$router.push('/')">
              去抽奖
            </el-button>
          </el-empty>
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="total > pageSize">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            background
            @current-change="loadRecords"
          />
        </div>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUserRecords, claimPrize } from '@/api'

const userStore = useUserStore()

const loading = ref(false)
const records = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 获取奖品图标
const getPrizeIcon = (level) => {
  const icons = {
    1: '👑',
    2: '🥇',
    3: '🥈',
    4: '🥉',
    5: '🎁'
  }
  return icons[level] || '🎁'
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN')
}

// 加载记录
const loadRecords = async () => {
  if (!userStore.user?.id) return

  loading.value = true
  try {
    const res = await getUserRecords(
      userStore.user.id, 
      currentPage.value - 1, 
      pageSize.value
    )
    records.value = res.data.content || []
    total.value = res.data.totalElements || 0
  } catch (e) {
    console.error('加载记录失败:', e)
  } finally {
    loading.value = false
  }
}

// 领取奖品
const handleClaim = async (record) => {
  try {
    await claimPrize(record.id, userStore.user.id)
    ElMessage.success('领取成功！')
    record.claimed = true
  } catch (e) {
    console.error('领取失败:', e)
  }
}

// 监听登录状态
watch(() => userStore.isLoggedIn, (val) => {
  if (val) {
    loadRecords()
  }
}, { immediate: true })

onMounted(() => {
  if (userStore.isLoggedIn) {
    loadRecords()
  }
})
</script>

<style lang="scss" scoped>
.records-page {
  min-height: 100vh;
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  margin-bottom: 24px;
  
  h1 {
    font-size: 20px;
  }
  
  .back-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    color: var(--text-secondary);
    text-decoration: none;
    transition: color 0.3s;
    
    &:hover {
      color: var(--text-primary);
    }
  }
  
  .placeholder {
    width: 80px;
  }
}

.main-content {
  max-width: 800px;
  margin: 0 auto;
}

.not-logged-in {
  padding: 60px 20px;
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  transition: transform 0.2s;
  
  &:hover {
    transform: translateX(4px);
  }
}

.record-icon {
  font-size: 36px;
  width: 50px;
  text-align: center;
}

.record-info {
  flex: 1;
  
  .prize-name {
    font-weight: 600;
    font-size: 16px;
    margin-bottom: 4px;
  }
  
  .draw-time {
    color: var(--text-muted);
    font-size: 13px;
  }
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-button-bg-color: rgba(255, 255, 255, 0.1);
  --el-pagination-button-color: var(--text-primary);
  --el-pagination-hover-color: var(--primary-color);
}
</style>
