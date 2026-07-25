<template>
  <div class="lottery-wheel-container">
    <!-- 转盘外框 -->
    <div class="wheel-wrapper">
      <!-- 转盘主体 -->
      <div 
        class="wheel" 
        :style="wheelStyle"
      >
        <!-- 使用 Canvas 绘制扇区 -->
        <canvas 
          ref="canvasRef" 
          :width="canvasSize" 
          :height="canvasSize"
          class="wheel-canvas"
        ></canvas>
      </div>
      
      <!-- 中心按钮 -->
      <div class="wheel-center" @click="startSpin" :class="{ disabled: !canSpin }">
        <div class="center-button">
          <span v-if="!isSpinning">抽奖</span>
          <span v-else class="spinning-text">...</span>
        </div>
      </div>
      
      <!-- 指针（在顶部） -->
      <div class="wheel-pointer">
        <div class="pointer-arrow"></div>
      </div>
    </div>

    <!-- 抽奖次数 -->
    <div class="chances-display glass-pill">
      <div class="chance-icon-wrapper">
        <el-icon class="chance-icon"><Star /></el-icon>
      </div>
      <span class="chance-text">剩余次数: <span class="chance-count">{{ remainingChances }}</span></span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Star } from '@element-plus/icons-vue'

const props = defineProps({
  prizes: {
    type: Array,
    default: () => []
  },
  remainingChances: {
    type: Number,
    default: 0
  },
  isSpinning: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['spin'])

const canvasRef = ref(null)
const rotation = ref(0)
const isAnimating = ref(false)
const canvasSize = 400

// 计算每个扇区的角度
const segmentAngle = computed(() => {
  if (props.prizes.length === 0) return 0
  return 360 / props.prizes.length
})

// 是否可以抽奖
const canSpin = computed(() => !props.isSpinning && !isAnimating.value && props.remainingChances > 0)

// 转盘样式
const wheelStyle = computed(() => ({
  transform: `rotate(${rotation.value}deg)`,
  transition: isAnimating.value ? 'transform 5s cubic-bezier(0.17, 0.67, 0.12, 0.99)' : 'none'
}))

// 默认颜色
const getDefaultColor = (index) => {
  const colors = [
    '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4',
    '#FFEAA7', '#DDA0DD', '#98FB98', '#FFD700'
  ]
  return colors[index % colors.length]
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

// 绘制转盘
const drawWheel = () => {
  const canvas = canvasRef.value
  if (!canvas || props.prizes.length === 0) return
  
  const ctx = canvas.getContext('2d')
  const centerX = canvasSize / 2
  const centerY = canvasSize / 2
  const radius = canvasSize / 2 - 10
  
  ctx.clearRect(0, 0, canvasSize, canvasSize)
  
  const numPrizes = props.prizes.length
  const anglePerPrize = (2 * Math.PI) / numPrizes
  
  props.prizes.forEach((prize, index) => {
    // 扇区从12点钟方向（-90度）开始，顺时针排列
    const startAngle = -Math.PI / 2 + index * anglePerPrize
    const endAngle = startAngle + anglePerPrize
    
    // 绘制扇区
    ctx.beginPath()
    ctx.moveTo(centerX, centerY)
    ctx.arc(centerX, centerY, radius, startAngle, endAngle)
    ctx.closePath()
    ctx.fillStyle = prize.color || getDefaultColor(index)
    ctx.fill()
    
    // 绘制边框
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.3)'
    ctx.lineWidth = 2
    ctx.stroke()
    
    // 绘制文字
    ctx.save()
    ctx.translate(centerX, centerY)
    ctx.rotate(startAngle + anglePerPrize / 2)
    ctx.textAlign = 'center'
    ctx.fillStyle = '#fff'
    ctx.font = 'bold 14px Arial'
    ctx.shadowColor = 'rgba(0, 0, 0, 0.5)'
    ctx.shadowBlur = 3
    
    // 绘制图标
    ctx.font = '24px Arial'
    ctx.fillText(getPrizeIcon(prize.prizeLevel), radius * 0.6, 8)
    
    // 绘制名称
    ctx.font = 'bold 12px Arial'
    const name = prize.name.length > 5 ? prize.name.substring(0, 5) + '..' : prize.name
    ctx.fillText(name, radius * 0.6, 28)
    
    ctx.restore()
  })
  
  // 绘制中心圆
  ctx.beginPath()
  ctx.arc(centerX, centerY, 50, 0, 2 * Math.PI)
  ctx.fillStyle = '#1a1a2e'
  ctx.fill()
}

// 开始抽奖
const startSpin = () => {
  if (!canSpin.value) return
  emit('spin')
}

// 旋转到指定奖品
const spinToIndex = (prizeIndex, callback) => {
  const numPrizes = props.prizes.length
  if (numPrizes === 0) return
  
  const anglePerPrize = 360 / numPrizes
  
  // 扇区布局：从12点钟位置开始，顺时针排列
  // index=0 的扇区中心在 anglePerPrize/2 度（从12点钟顺时针计算）
  // 指针在12点钟位置（0度）
  
  // 目标：让 prizeIndex 的扇区中心对准12点钟位置的指针
  // prizeIndex 的扇区中心位置 = prizeIndex * anglePerPrize + anglePerPrize/2
  // 需要逆时针旋转这个角度（即顺时针旋转负角度）
  // 但 CSS rotate 正值是顺时针，所以我们需要 360 - 扇区位置
  
  const prizeCenterAngle = prizeIndex * anglePerPrize + anglePerPrize / 2
  // 旋转使得 prizeCenterAngle 移到 0 度位置
  // 即逆时针旋转 prizeCenterAngle 度 = 顺时针旋转 (360 - prizeCenterAngle) 度
  const targetAngle = 360 - prizeCenterAngle
  
  // 添加额外圈数（5-8圈）确保足够的旋转效果
  const extraRotations = (5 + Math.floor(Math.random() * 3)) * 360
  const totalRotation = rotation.value + extraRotations + targetAngle
  
  // 确保新的旋转角度大于当前角度（总是向前转）
  const finalRotation = Math.ceil(rotation.value / 360) * 360 + extraRotations + targetAngle
  
  isAnimating.value = true
  rotation.value = finalRotation
  
  // 动画结束后回调
  setTimeout(() => {
    isAnimating.value = false
    callback()
  }, 5000)
}

// 监听奖品变化重绘
watch(() => props.prizes, () => {
  drawWheel()
}, { deep: true })

// 初始化
onMounted(() => {
  drawWheel()
})

// 暴露方法给父组件
defineExpose({
  spinToIndex
})
</script>

<style lang="scss" scoped>
.lottery-wheel-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.wheel-wrapper {
  position: relative;
  width: 340px;
  height: 340px;
  
  @media (min-width: 768px) {
    width: 400px;
    height: 400px;
  }
}

.wheel {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  position: relative;
  box-shadow: 
    0 0 0 8px rgba(255, 255, 255, 0.3),
    0 0 0 12px var(--primary-color),
    0 0 30px rgba(0, 0, 0, 0.3);
}

.wheel-canvas {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.wheel-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
  cursor: pointer;
  
  &.disabled {
    cursor: not-allowed;
    opacity: 0.7;
  }
}

.center-button {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: var(--gradient-gold);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  color: #333;
  box-shadow: 
    0 4px 20px rgba(0, 0, 0, 0.3),
    inset 0 2px 10px rgba(255, 255, 255, 0.5);
  transition: all 0.3s ease;
  
  &:hover {
    transform: scale(1.05);
    box-shadow: 
      0 6px 25px rgba(0, 0, 0, 0.4),
      inset 0 2px 10px rgba(255, 255, 255, 0.5);
  }
  
  &:active {
    transform: scale(0.98);
  }
}

.spinning-text {
  animation: pulse 0.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.wheel-pointer {
  position: absolute;
  top: -15px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 20;
}

.pointer-arrow {
  width: 0;
  height: 0;
  border-left: 18px solid transparent;
  border-right: 18px solid transparent;
  border-top: 35px solid var(--primary-color);
  filter: drop-shadow(0 4px 6px rgba(0, 0, 0, 0.3));
  
  &::after {
    content: '';
    position: absolute;
    top: -35px;
    left: -12px;
    width: 0;
    height: 0;
    border-left: 12px solid transparent;
    border-right: 12px solid transparent;
    border-top: 25px solid #fff;
  }
}

.chances-display {
  margin-top: 20px;
}

.glass-pill {
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 6px 16px 6px 6px;
  border-radius: 40px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba(15, 23, 42, 0.8);
    border-color: rgba(6, 182, 212, 0.3);
    box-shadow: 0 0 20px rgba(6, 182, 212, 0.2);
    transform: translateY(-2px);
  }
}

.chance-icon-wrapper {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f6d365 0%, #fda085 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  box-shadow: 0 2px 10px rgba(246, 211, 101, 0.3);
}

.chance-text {
  color: var(--text-secondary);
  font-size: 14px;
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.chance-count {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  font-family: 'DIN Alternate', sans-serif;
  color: #67e8f9;
}
</style>
