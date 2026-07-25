<template>
  <div class="prize-manager">
    <!-- 工具栏 -->
    <div class="toolbar glass-card">
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>
        添加奖品
      </el-button>
      <el-button @click="loadPrizes">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 奖品表格 -->
    <div class="table-container glass-card">
      <el-table 
        :data="prizes" 
        v-loading="loading"
        style="width: 100%"
        row-key="id"
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="奖品" min-width="180">
          <template #default="{ row }">
            <div class="prize-cell">
              <div 
                class="prize-color" 
                :style="{ backgroundColor: row.color }"
              ></div>
              <div class="prize-info">
                <div class="prize-name">{{ row.name }}</div>
                <div class="prize-desc">{{ row.description }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="prizeLevel" label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.prizeLevel)" size="small">
              {{ getLevelName(row.prizeLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="probability" label="概率" width="100">
          <template #default="{ row }">
            {{ (row.probability / 10).toFixed(1) }}%
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="drawnCount" label="已抽" width="80" />
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <el-switch 
              v-model="row.enabled" 
              @change="handleToggle(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑奖品' : '添加奖品'"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入奖品名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="formData.description" 
            type="textarea"
            placeholder="请输入奖品描述" 
          />
        </el-form-item>
        <el-form-item label="等级" prop="prizeLevel">
          <el-select v-model="formData.prizeLevel" placeholder="请选择奖品等级">
            <el-option label="特等奖" :value="1" />
            <el-option label="一等奖" :value="2" />
            <el-option label="二等奖" :value="3" />
            <el-option label="三等奖" :value="4" />
            <el-option label="参与奖" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="概率" prop="probability">
          <el-slider 
            v-model="formData.probability" 
            :min="1" 
            :max="500"
            :format-tooltip="(val) => `${(val/10).toFixed(1)}%`"
          />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="formData.stock" :min="0" :max="99999" />
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <el-color-picker v-model="formData.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { getAllPrizes, createPrize, updatePrize, deletePrize, togglePrize } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const prizes = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref()

const formData = reactive({
  name: '',
  description: '',
  prizeLevel: 5,
  probability: 100,
  stock: 100,
  color: '#FF6B6B'
})

const formRules = {
  name: [{ required: true, message: '请输入奖品名称', trigger: 'blur' }],
  prizeLevel: [{ required: true, message: '请选择奖品等级', trigger: 'change' }]
}

const getLevelName = (level) => {
  const names = { 1: '特等奖', 2: '一等奖', 3: '二等奖', 4: '三等奖', 5: '参与奖' }
  return names[level] || '参与奖'
}

const getLevelType = (level) => {
  const types = { 1: 'danger', 2: 'warning', 3: '', 4: 'success', 5: 'info' }
  return types[level] || 'info'
}

const loadPrizes = async () => {
  loading.value = true
  try {
    const res = await getAllPrizes()
    prizes.value = res.data
  } catch (e) {
    console.error('加载奖品失败:', e)
  } finally {
    loading.value = false
  }
}

const openDialog = (prize) => {
  if (prize) {
    isEdit.value = true
    editId.value = prize.id
    Object.assign(formData, prize)
  } else {
    isEdit.value = false
    editId.value = null
    resetForm()
  }
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(formData, {
    name: '',
    description: '',
    prizeLevel: 5,
    probability: 100,
    stock: 100,
    color: '#FF6B6B'
  })
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updatePrize(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await createPrize(formData)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadPrizes()
  } catch (e) {
    console.error('保存失败:', e)
  } finally {
    submitting.value = false
  }
}

const handleToggle = async (prize) => {
  try {
    await togglePrize(prize.id)
    ElMessage.success('状态已更新')
  } catch (e) {
    prize.enabled = !prize.enabled
  }
}

const handleDelete = async (prize) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${prize.name}"吗？`, '确认删除', {
      type: 'warning'
    })
    await deletePrize(prize.id)
    ElMessage.success('删除成功')
    loadPrizes()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败:', e)
    }
  }
}

onMounted(() => {
  loadPrizes()
})
</script>

<style lang="scss" scoped>
.prize-manager {
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

.prize-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.prize-color {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  flex-shrink: 0;
}

.prize-info {
  .prize-name {
    font-weight: 600;
    margin-bottom: 2px;
  }
  
  .prize-desc {
    font-size: 12px;
    color: var(--text-muted);
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
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
