<template>
  <div class="record-manager">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card glass-card">
        <div class="stat-icon">🎰</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalDraws || 0 }}</div>
          <div class="stat-label">总抽奖次数</div>
        </div>
      </div>
    </div>

    <!-- 最近中奖 -->
    <div class="section glass-card">
      <h2>🏆 最近中奖记录</h2>
      <el-table :data="stats.recentWinners || []" style="width: 100%">
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="prizeName" label="奖品" min-width="150" />
        <el-table-column prop="prizeLevel" label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.prizeLevel)" size="small">
              {{ getLevelName(row.prizeLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="drawTime" label="抽奖时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.drawTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="claimed" label="领取状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.claimed ? 'success' : 'warning'" size="small">
              {{ row.claimed ? '已领取' : '未领取' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getStats } from '@/api'

const stats = ref({})

const getLevelName = (level) => {
  const names = { 1: '特等奖', 2: '一等奖', 3: '二等奖', 4: '三等奖', 5: '参与奖' }
  return names[level] || '参与奖'
}

const getLevelType = (level) => {
  const types = { 1: 'danger', 2: 'warning', 3: '', 4: 'success', 5: 'info' }
  return types[level] || 'info'
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return new Date(timeStr).toLocaleString('zh-CN')
}

const loadStats = async () => {
  try {
    const res = await getStats()
    stats.value = res.data
  } catch (e) {
    console.error('加载统计失败:', e)
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style lang="scss" scoped>
.record-manager {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
}

.stat-icon {
  font-size: 40px;
}

.stat-info {
  .stat-value {
    font-size: 28px;
    font-weight: 700;
    background: var(--gradient-gold);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  
  .stat-label {
    color: var(--text-secondary);
    font-size: 14px;
  }
}

.section {
  padding: 20px;
  
  h2 {
    font-size: 18px;
    margin-bottom: 16px;
  }
}

:deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(255, 255, 255, 0.05);
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.05);
  --el-table-border-color: var(--border-color);
  --el-table-text-color: var(--text-primary);
  --el-table-header-text-color: var(--text-secondary);
}
</style>
