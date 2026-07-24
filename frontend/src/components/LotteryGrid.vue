<template>
  <div class="lottery-grid-container">
    <!-- 九宫格 -->
    <div class="grid-wrapper">
      <div 
        v-for="(item, index) in gridItems" 
        :key="index"
        class="grid-item"
        :class="{ 
          active: currentIndex === index,
          center: index === 4,
          winner: isWinnerItem(index)
        }"
        :style="{ backgroundColor: item.color }"
      >
        <template v-if="index === 4">
          <!-- 中心抽奖按钮 -->
          <button 
            class="draw-button"
            @click="startDraw"
            :disabled="!canDraw"
          >
            <div class="button-content">
              <span v-if="!isDrawing">开始</span>
              <span v-else>抽奖中</span>
            </div>
            <div class="chances-badge">
              {{ remainingChances }}次
            </div>
          </button>
        </template>
        <template v-else>
          <!-- 奖品项 -->
          <div class="prize-content">
            <div class="prize-icon">{{ getPrizeIcon(item.prizeLevel) }}</div>
            <div class="prize-name">{{ item.name }}</div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  prizes: {
    type: Array,
    default: () => []
  },
  remainingChances: {
    type: Number,
    default: 0
  },
  isDrawing: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['draw'])

const currentIndex = ref(-1)
const winnerIndex = ref(-1)

// 九宫格位置映射（跳过中心位置4）
const positionMap = [0, 1, 2, 5, 8, 7, 6, 3] // 顺时针顺序

// 九宫格项目（8个奖品 + 1个中心按钮）
const gridItems = computed(() => {
  const items = []
  let prizeIndex = 0
  
  for (let i = 0; i < 9; i++) {
    if (i === 4) {
      items.push({ isCenter: true })
    } else {
      items.push(props.prizes[prizeIndex] || {
        name: '神秘奖品',
        prizeLevel: 5,
        color: '#87CEEB'
      })
      prizeIndex++
    }
  }
  return items
})

// 是否可以抽奖
const canDraw = computed(() => !props.isDrawing && props.remainingChances > 0)

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

// 判断是否是中奖项
const isWinnerItem = (index) => {
  return winnerIndex.value !== -1 && index === winnerIndex.value
}

// 开始抽奖
const startDraw = () => {
  if (!canDraw.value) return
  emit('draw')
}

// 运行跑马灯动画
const runAnimation = (targetPrizeIndex, callback) => {
  winnerIndex.value = -1
  currentIndex.value = 0
  
  let round = 0
  const totalRounds = 3 // 转3圈
  let currentPos = 0
  let speed = 100
  
  // 奖品数组索引 (0-7) 到九宫格位置 (0-8, 跳过中心4) 的映射
  // prizes[0] -> gridItems[0] -> 位置0
  // prizes[1] -> gridItems[1] -> 位置1
  // prizes[2] -> gridItems[2] -> 位置2
  // prizes[3] -> gridItems[3] -> 位置3
  // prizes[4] -> gridItems[5] -> 位置5 (跳过中心4)
  // prizes[5] -> gridItems[6] -> 位置6
  // prizes[6] -> gridItems[7] -> 位置7
  // prizes[7] -> gridItems[8] -> 位置8
  const prizeIndexToGridPosition = [0, 1, 2, 3, 5, 6, 7, 8]
  
  // positionMap 是跑马灯顺时针转动的顺序：0->1->2->5->8->7->6->3->0...
  // positionMap[step] = 九宫格位置
  // 我们需要反向映射：九宫格位置 -> 跑马灯步数
  const gridPositionToStep = {
    0: 0, // 位置0是第0步
    1: 1, // 位置1是第1步
    2: 2, // 位置2是第2步
    5: 3, // 位置5是第3步
    8: 4, // 位置8是第4步
    7: 5, // 位置7是第5步
    6: 6, // 位置6是第6步
    3: 7  // 位置3是第7步
  }
  
  // 计算目标奖品在九宫格中的实际位置
  const targetGridPosition = prizeIndexToGridPosition[targetPrizeIndex % 8]
  // 计算该位置在跑马灯顺序中的步数
  const targetStep = gridPositionToStep[targetGridPosition]
  
  const animate = () => {
    // 获取当前格子的实际索引
    const actualIndex = positionMap[currentPos % 8]
    currentIndex.value = actualIndex
    
    currentPos++
    
    // 检查是否完成：需要转完圈数并停在目标步数
    const totalSteps = totalRounds * 8 + targetStep + 1
    if (currentPos >= totalSteps) {
      // 停在目标位置
      winnerIndex.value = targetGridPosition
      setTimeout(callback, 500)
      return
    }
    
    // 逐渐减速
    if (currentPos > totalSteps - 8) {
      speed = Math.min(speed + 30, 300)
    }
    
    setTimeout(animate, speed)
  }
  
  animate()
}

// 暴露方法给父组件
defineExpose({
  runAnimation
})
</script>

<style lang="scss" scoped>
.lottery-grid-container {
  display: flex;
  justify-content: center;
  padding: 20px;
}

.grid-wrapper {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  width: 320px;
  
  @media (min-width: 768px) {
    width: 380px;
    gap: 10px;
  }
}

.grid-item {
  aspect-ratio: 1;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.2s ease;
  box-shadow: var(--shadow-sm);
  
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background: linear-gradient(135deg, rgba(255,255,255,0.3) 0%, transparent 50%);
    pointer-events: none;
  }
  
  &.active {
    transform: scale(1.05);
    box-shadow: 
      0 0 20px rgba(255, 215, 0, 0.8),
      0 0 40px rgba(255, 215, 0, 0.4);
    z-index: 10;
    
    &::after {
      content: '';
      position: absolute;
      inset: -4px;
      border-radius: inherit;
      border: 3px solid #FFD700;
      animation: pulse 0.5s ease-in-out infinite;
    }
  }
  
  &.winner {
    animation: winner-glow 0.5s ease-in-out infinite alternate;
  }
  
  &.center {
    background: transparent !important;
    box-shadow: none;
    
    &::before {
      display: none;
    }
  }
}

.prize-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px;
  color: white;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);
}

.prize-icon {
  font-size: 32px;
  margin-bottom: 4px;
}

.prize-name {
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.draw-button {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: var(--radius-md);
  background: var(--gradient-gold);
  cursor: pointer;
  position: relative;
  transition: all 0.3s ease;
  box-shadow: var(--shadow-md);
  
  &:hover:not(:disabled) {
    transform: scale(1.02);
    box-shadow: var(--shadow-lg);
  }
  
  &:active:not(:disabled) {
    transform: scale(0.98);
  }
  
  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.button-content {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.chances-badge {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
}

@keyframes winner-glow {
  from {
    box-shadow: 
      0 0 20px rgba(255, 215, 0, 0.8),
      0 0 40px rgba(255, 215, 0, 0.4);
  }
  to {
    box-shadow: 
      0 0 30px rgba(255, 215, 0, 1),
      0 0 60px rgba(255, 215, 0, 0.6);
  }
}
</style>
