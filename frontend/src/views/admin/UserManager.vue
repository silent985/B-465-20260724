<template>
  <div class="user-manager">
    <!-- 工具栏 -->
    <div class="toolbar glass-card">
      <el-button @click="loadUsers">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 用户表格 -->
    <div class="table-container glass-card">
      <el-table 
        :data="users" 
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="36">{{ row.nickname?.[0] || row.username?.[0] }}</el-avatar>
              <div class="user-info">
                <div class="user-name">{{ row.nickname || row.username }}</div>
                <div class="user-account">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">
            {{ row.phone || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : ''">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remainingChances" label="剩余次数" width="100" />
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <el-switch 
              v-model="row.enabled" 
              @change="handleToggle(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openAddChances(row)">
              增加次数
            </el-button>
            <el-button 
              size="small" 
              type="danger" 
              @click="handleDelete(row)"
              :disabled="row.role === 'ADMIN'"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadUsers"
        />
      </div>
    </div>

    <!-- 增加次数对话框 -->
    <el-dialog
      v-model="addChancesVisible"
      title="增加抽奖次数"
      width="420px"
      class="add-chances-dialog"
    >
      <div class="dialog-content">
        <!-- 用户信息展示 -->
        <div class="user-display">
          <el-avatar :size="52" class="user-avatar">
            {{ selectedUser?.nickname?.[0] || selectedUser?.username?.[0] }}
          </el-avatar>
          <div class="user-detail">
            <div class="user-name">{{ selectedUser?.nickname || selectedUser?.username }}</div>
            <div class="user-account">@{{ selectedUser?.username }}</div>
          </div>
        </div>
        
        <el-divider />
        
        <!-- 表单区域 -->
        <el-form label-width="100px" label-position="left" class="chances-form">
          <el-form-item label="当前次数">
            <div class="current-chances">
              <span class="chances-value">{{ selectedUser?.remainingChances }}</span>
              <span class="chances-unit">次</span>
            </div>
          </el-form-item>
          <el-form-item label="增加数量">
            <div class="add-chances-input">
              <el-input-number 
                v-model="addCount" 
                :min="1" 
                :max="999" 
                :step="5"
                controls-position="right"
              />
              <span class="chances-unit">次</span>
            </div>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="addChancesVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddChances" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getAllUsers, toggleUser, deleteUser, addChances } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const users = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const addChancesVisible = ref(false)
const selectedUser = ref(null)
const addCount = ref(10)

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await getAllUsers(currentPage.value - 1, pageSize.value)
    users.value = res.data.content || []
    total.value = res.data.totalElements || 0
  } catch (e) {
    console.error('加载用户失败:', e)
  } finally {
    loading.value = false
  }
}

const handleToggle = async (user) => {
  try {
    await toggleUser(user.id)
    ElMessage.success('状态已更新')
  } catch (e) {
    user.enabled = !user.enabled
  }
}

const handleDelete = async (user) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户"${user.username}"吗？`, '确认删除', {
      type: 'warning'
    })
    await deleteUser(user.id)
    ElMessage.success('删除成功')
    loadUsers()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败:', e)
    }
  }
}

const openAddChances = (user) => {
  selectedUser.value = user
  addCount.value = 10
  addChancesVisible.value = true
}

const handleAddChances = async () => {
  if (!selectedUser.value) return
  
  submitting.value = true
  try {
    await addChances(selectedUser.value.id, addCount.value)
    ElMessage.success(`已为用户增加 ${addCount.value} 次抽奖机会`)
    addChancesVisible.value = false
    loadUsers()
  } catch (e) {
    console.error('增加次数失败:', e)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style lang="scss" scoped>
.user-manager {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  padding: 16px;
  display: flex;
  gap: 12px;
}

.table-container {
  padding: 16px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  .user-name {
    font-weight: 600;
  }
  
  .user-account {
    font-size: 12px;
    color: var(--text-muted);
  }
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
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

// 增加次数对话框样式
.dialog-content {
  padding: 8px 0;
}

.user-display {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(6, 182, 212, 0.15), rgba(16, 185, 129, 0.1));
  border-radius: 16px;
  border: 1px solid rgba(6, 182, 212, 0.2);
}

.user-avatar {
  background: linear-gradient(135deg, #06b6d4, #10b981);
  font-size: 20px;
  font-weight: 600;
}

.user-detail {
  .user-name {
    font-size: 18px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    margin-bottom: 4px;
  }
  
  .user-account {
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }
}

.chances-form {
  margin-top: 8px;
}

.current-chances {
  display: flex;
  align-items: baseline;
  gap: 4px;
  
  .chances-value {
    font-size: 28px;
    font-weight: 700;
    color: #06b6d4;
    line-height: 1;
  }
}

.add-chances-input {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chances-unit {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}
</style>
