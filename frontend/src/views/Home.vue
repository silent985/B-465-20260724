<template>
  <div class="home-page">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="floating-star" v-for="n in 20" :key="n"></div>
    </div>

    <!-- 头部导航 -->
    <header class="header">
      <div class="logo">
        <span class="logo-icon">🎰</span>
        <span class="logo-text">幸运大转盘</span>
      </div>
      <nav class="nav-links">
        <router-link to="/records" class="nav-link">
          <el-icon><List /></el-icon>
          抽奖记录
        </router-link>
        <template v-if="userStore.isLoggedIn">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32">{{ userStore.user?.nickname?.[0] || 'U' }}</el-avatar>
              <span class="username">{{ userStore.user?.nickname }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="userStore.isAdmin" command="admin">
                  <el-icon><Setting /></el-icon>管理后台
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login" class="nav-link login-btn">
            <el-icon><User /></el-icon>
            登录
          </router-link>
        </template>
      </nav>
    </header>

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 标题区域 -->
      <div class="title-section">
        <h1 class="main-title glow-text">🎉 幸运大抽奖 🎉</h1>
        <p class="sub-title">每一次转动都是一次惊喜</p>
      </div>

      <!-- 每日签到（仅普通用户可见） -->
      <div class="sign-in-card glass-card" v-if="canSignIn">
        <div class="sign-in-info">
          <span class="sign-in-icon">📅</span>
          <div class="sign-in-text">
            <div class="sign-in-status">
              今日签到：
              <span :class="['status-tag', signedInToday ? 'done' : 'todo']">
                {{ signedInToday ? '已签到' : '未签到' }}
              </span>
            </div>
            <div class="sign-in-chances">
              剩余抽奖次数：<span class="chances-num">{{ userStore.remainingChances }}</span>
            </div>
          </div>
        </div>
        <el-button
          type="primary"
          round
          :loading="signInLoading"
          :disabled="signedInToday"
          @click="handleSignIn"
        >
          {{ signedInToday ? '今日已签到' : '签到领取次数' }}
        </el-button>
      </div>

      <!-- 中奖滚动公告 -->
      <div class="winner-marquee glass-card" v-if="recentWinners.length > 0">
        <div class="marquee-content">
          <span v-for="(winner, index) in recentWinners" :key="index" class="winner-item">
            🎊 恭喜 <span class="winner-name">{{ winner.username }}</span> 
            获得 <span class="prize-name">{{ winner.prizeName }}</span>
          </span>
        </div>
      </div>

      <!-- 抽奖模式切换 -->
      <div class="mode-switch">
        <el-segmented v-model="lotteryMode" :options="modeOptions" size="large" />
      </div>

      <!-- 抽奖区域 -->
      <div class="lottery-area glass-card">
        <!-- 转盘模式 -->
        <LotteryWheel 
          v-if="lotteryMode === 'wheel'"
          ref="wheelRef"
          :prizes="prizes"
          :remaining-chances="userStore.remainingChances"
          :is-spinning="isSpinning"
          @spin="handleSpin"
        />

        <!-- 九宫格模式 -->
        <LotteryGrid
          v-else
          ref="gridRef"
          :prizes="prizes"
          :remaining-chances="userStore.remainingChances"
          :is-drawing="isSpinning"
          @draw="handleSpin"
        />
      </div>

      <!-- 奖品展示区 -->
      <div class="prizes-section">
        <h2 class="section-title">🏆 奖品列表</h2>
        <div class="prizes-grid">
          <div 
            v-for="prize in displayPrizes" 
            :key="prize.id" 
            class="prize-card glass-card"
            :class="{ 'out-of-stock': prize.stock <= 0 }"
          >
            <div class="prize-badge" :class="`level-${prize.prizeLevel}`">
              {{ getLevelName(prize.prizeLevel) }}
            </div>
            <div class="prize-icon">{{ getPrizeIcon(prize.prizeLevel) }}</div>
            <div class="prize-name">{{ prize.name }}</div>
            <div class="prize-stock">
              库存: {{ prize.stock > 0 ? prize.stock : '已抽完' }}
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 页脚 -->
    <footer class="footer">
      <p>© 2024 幸运大转盘 - 公平透明的抽奖平台</p>
    </footer>

    <!-- 中奖弹窗 -->
    <el-dialog
      v-model="showResultDialog"
      :title="resultData?.win ? '🎉 恭喜中奖！' : '😊 再接再厉'"
      width="400px"
      center
      :close-on-click-modal="false"
    >
      <div class="result-content">
        <div class="result-icon" :class="{ win: resultData?.win }">
          {{ resultData?.win ? '🎊' : '💪' }}
        </div>
        <div class="result-prize">{{ resultData?.prizeName }}</div>
        <div class="result-message">{{ resultData?.message }}</div>
      </div>
      <template #footer>
        <el-button type="primary" @click="showResultDialog = false">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { List, User, Setting, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getEnabledPrizes, getRecentWinners, drawLottery, signIn, getSignInStatus } from '@/api'
import LotteryWheel from '@/components/LotteryWheel.vue'
import LotteryGrid from '@/components/LotteryGrid.vue'

const router = useRouter()
const userStore = useUserStore()

// 状态
const prizes = ref([])
const recentWinners = ref([])
const lotteryMode = ref('wheel')
const isSpinning = ref(false)
const showResultDialog = ref(false)
const resultData = ref(null)

// 签到状态
const signedInToday = ref(false)
const signInLoading = ref(false)

// 仅登录的普通用户（非管理员）可参与签到
const canSignIn = computed(() => userStore.isLoggedIn && !userStore.isAdmin)

// 组件引用
const wheelRef = ref(null)
const gridRef = ref(null)

// 模式选项
const modeOptions = [
  { label: '🎡 幸运转盘', value: 'wheel' },
  { label: '🎯 九宫格', value: 'grid' }
]

// 展示用的奖品（排除最后一个填充奖品）
const displayPrizes = computed(() => prizes.value.slice(0, -1))

// 获取等级名称
const getLevelName = (level) => {
  const names = {
    1: '特等奖',
    2: '一等奖',
    3: '二等奖',
    4: '三等奖',
    5: '参与奖'
  }
  return names[level] || '参与奖'
}

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

// 加载奖品
const loadPrizes = async () => {
  try {
    const res = await getEnabledPrizes()
    prizes.value = res.data
  } catch (e) {
    console.error('加载奖品失败:', e)
  }
}

// 加载最近中奖
const loadRecentWinners = async () => {
  try {
    const res = await getRecentWinners(10)
    recentWinners.value = res.data
  } catch (e) {
    console.error('加载中奖记录失败:', e)
  }
}

// 加载签到状态
const loadSignInStatus = async () => {
  if (!canSignIn.value) return
  try {
    const res = await getSignInStatus(userStore.user.id)
    signedInToday.value = res.data.signedInToday
    userStore.updateChances(res.data.remainingChances)
  } catch (e) {
    console.error('加载签到状态失败:', e)
  }
}

// 处理签到
const handleSignIn = async () => {
  if (!canSignIn.value || signedInToday.value) return

  signInLoading.value = true
  try {
    const res = await signIn(userStore.user.id)
    signedInToday.value = res.data.signedInToday
    userStore.updateChances(res.data.remainingChances)
    ElMessage.success(res.message || '签到成功')
  } catch (e) {
    // 重复签到等业务异常已由响应拦截器提示
    console.error('签到失败:', e)
  } finally {
    signInLoading.value = false
  }
}

// 处理抽奖
const handleSpin = async () => {
  // 未登录用户弹窗提醒
  if (!userStore.isLoggedIn) {
    ElMessageBox.confirm(
      '您还未登录，请先登录后再参与抽奖！',
      '温馨提示',
      {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'warning',
      }
    ).then(() => {
      router.push('/login')
    }).catch(() => {})
    return
  }

  // 修复：抽奖次数 < 1 时无法抽奖
  if (userStore.remainingChances < 1) {
    ElMessage.warning('您的抽奖次数已用完，请联系管理员充值')
    return
  }

  isSpinning.value = true

  try {
    const res = await drawLottery(userStore.user.id)
    const result = res.data

    // 更新剩余次数
    userStore.updateChances(result.remainingChances)

    // 执行动画
    if (lotteryMode.value === 'wheel') {
      wheelRef.value?.spinToIndex(result.prizeIndex, () => {
        showResult(result)
      })
    } else {
      gridRef.value?.runAnimation(result.prizeIndex, () => {
        showResult(result)
      })
    }
  } catch (e) {
    isSpinning.value = false
    console.error('抽奖失败:', e)
  }
}

// 显示结果
const showResult = (result) => {
  isSpinning.value = false
  resultData.value = result
  showResultDialog.value = true
  
  // 刷新中奖记录
  loadRecentWinners()
}

// 处理下拉菜单命令
const handleCommand = (command) => {
  if (command === 'admin') {
    router.push('/admin')
  } else if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
  }
}

// 初始化
onMounted(async () => {
  loadPrizes()
  loadRecentWinners()
  if (userStore.isLoggedIn) {
    await userStore.refreshUser()
    loadSignInStatus()
  }
})
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

// 背景装饰
.bg-decoration {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.floating-star {
  position: absolute;
  width: 4px;
  height: 4px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 50%;
  animation: float 6s ease-in-out infinite;
  
  @for $i from 1 through 20 {
    &:nth-child(#{$i}) {
      left: random(100) * 1%;
      top: random(100) * 1%;
      animation-delay: random(5) * 1s;
      animation-duration: (4 + random(4)) * 1s;
    }
  }
}

// 头部
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  position: sticky;
  top: 0;
  z-index: 100;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .logo-icon {
    font-size: 28px;
  }
  
  .logo-text {
    font-size: 20px;
    font-weight: 700;
    background: var(--gradient-gold);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-secondary);
  text-decoration: none;
  padding: 8px 16px;
  border-radius: var(--radius-full);
  transition: all 0.3s ease;
  
  &:hover {
    color: var(--text-primary);
    background: rgba(255, 255, 255, 0.1);
  }
  
  &.login-btn {
    background: var(--gradient-accent);
    color: white;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  
  .username {
    color: var(--text-primary);
  }
}

// 主内容
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.title-section {
  text-align: center;
  margin-bottom: 24px;
}

.main-title {
  font-size: 36px;
  margin-bottom: 8px;
  
  @media (min-width: 768px) {
    font-size: 48px;
  }
}

.sub-title {
  color: var(--text-secondary);
  font-size: 16px;
}

// 每日签到
.sign-in-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.sign-in-info {
  display: flex;
  align-items: center;
  gap: 12px;

  .sign-in-icon {
    font-size: 32px;
  }
}

.sign-in-text {
  .sign-in-status {
    color: var(--text-secondary);
    margin-bottom: 4px;
  }

  .sign-in-chances {
    color: var(--text-secondary);
    font-size: 14px;

    .chances-num {
      color: #FFD700;
      font-weight: 700;
      font-size: 18px;
    }
  }
}

.status-tag {
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;

  &.done {
    background: rgba(46, 204, 113, 0.2);
    color: #2ecc71;
  }

  &.todo {
    background: rgba(255, 215, 0, 0.2);
    color: #FFD700;
  }
}

// 中奖滚动
.winner-marquee {
  padding: 12px 20px;
  margin-bottom: 24px;
  overflow: hidden;
}

.marquee-content {
  display: flex;
  gap: 40px;
  animation: scroll 20s linear infinite;
  white-space: nowrap;
}

.winner-item {
  color: var(--text-secondary);
  
  .winner-name {
    color: #FFD700;
    font-weight: 600;
  }
  
  .prize-name {
    color: #4ECDC4;
    font-weight: 600;
  }
}

@keyframes scroll {
  from { transform: translateX(0); }
  to { transform: translateX(-50%); }
}

// 模式切换
.mode-switch {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

// 抽奖区域
.lottery-area {
  padding: 40px 20px;
  margin-bottom: 40px;
  display: flex;
  justify-content: center;
}

// 奖品展示
.prizes-section {
  margin-bottom: 40px;
}

.section-title {
  text-align: center;
  font-size: 24px;
  margin-bottom: 24px;
}

.prizes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.prize-card {
  padding: 20px 12px;
  text-align: center;
  position: relative;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-4px);
  }
  
  &.out-of-stock {
    opacity: 0.5;
  }
}

.prize-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  
  &.level-1 { background: linear-gradient(135deg, #FFD700, #FFA500); color: #333; }
  &.level-2 { background: linear-gradient(135deg, #C0C0C0, #A9A9A9); color: #333; }
  &.level-3 { background: linear-gradient(135deg, #CD7F32, #8B4513); color: white; }
  &.level-4 { background: linear-gradient(135deg, #4ECDC4, #2ecc71); color: white; }
  &.level-5 { background: linear-gradient(135deg, #667eea, #764ba2); color: white; }
}

.prize-card .prize-icon {
  font-size: 40px;
  margin-bottom: 8px;
}

.prize-card .prize-name {
  font-weight: 600;
  margin-bottom: 4px;
}

.prize-stock {
  font-size: 12px;
  color: var(--text-muted);
}

// 页脚
.footer {
  text-align: center;
  padding: 24px;
  color: var(--text-muted);
  font-size: 14px;
}

// 中奖弹窗
.result-content {
  text-align: center;
  padding: 20px 0;
}

.result-icon {
  font-size: 64px;
  margin-bottom: 16px;
  animation: bounce 0.5s ease infinite;
  
  &.win {
    animation: winner-bounce 0.5s ease infinite;
  }
}

.result-prize {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin-bottom: 8px;
}

.result-message {
  color: #666;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@keyframes winner-bounce {
  0%, 100% { transform: scale(1) rotate(-5deg); }
  50% { transform: scale(1.1) rotate(5deg); }
}
</style>
