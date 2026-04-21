<template>
  <div class="my-marked-page nexus-page">
    <div class="app-shell">
      <div class="page-title">
        <p class="eyebrow">My Collection</p>
        <div class="title-row">
          <span>我的标记</span>
          <span class="dot">•</span>
          <span class="subtitle">书影音收藏</span>
        </div>
      </div>

      <!-- 分类标签 -->
      <div class="custom-tabs">
        <div
          class="tab-pill"
          :class="{ active: activeTab === 'all' }"
          @click="changeTab('all')"
        >
          全部 ({{ totalCount }})
        </div>
        <div
          class="tab-pill"
          :class="{ active: activeTab === 1 }"
          @click="changeTab(1)"
        >
          书籍 ({{ bookCount }})
        </div>
        <div
          class="tab-pill"
          :class="{ active: activeTab === 2 }"
          @click="changeTab(2)"
        >
          电影 ({{ movieCount }})
        </div>
        <div
          class="tab-pill"
          :class="{ active: activeTab === 3 }"
          @click="changeTab(3)"
        >
          音乐 ({{ musicCount }})
        </div>
      </div>

      <!-- 物品列表 -->
      <div class="items-container">
        <template v-if="loading">
          <van-skeleton v-for="n in 3" :key="n" title avatar :row="3" class="mt-4" />
        </template>
        <template v-else>
          <van-empty v-if="displayedItems.length === 0" description="暂无标记" />
          <div
            v-for="item in displayedItems"
            :key="item.item.id"
            class="item-card glass-card"
            @click="goToItemDetail(item.item.id)"
          >
            <img
              :src="item.item.coverUrl || 'https://via.placeholder.com/80'"
              class="item-cover"
              alt=""
            />
            <div class="item-body">
              <div class="item-type-badge">
                {{ getItemTypeName(item.item.itemType) }}
              </div>
              <div class="item-title">{{ item.item.title }}</div>
              <div class="item-creator">{{ item.item.creator }}</div>
              <div class="item-meta">
                <span class="action-badge" :class="getActionClass(item.action)">
                  {{ getActionName(item.action) }}
                </span>
                <span v-if="item.weight" class="rating">
                  <i class="ri-star-fill"></i> {{ item.weight.toFixed(1) }}
                </span>
                <span class="time">{{ formatTime(item.createTime) }}</span>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import myAxios from '../request'

const router = useRouter()

const activeTab = ref<'all' | number>('all')
const allItems = ref<any[]>([])
const loading = ref(true)

// 按类型分类的物品
const displayedItems = computed(() => {
  if (activeTab.value === 'all') {
    return allItems.value
  }
  return allItems.value.filter(item => item.item.itemType === activeTab.value)
})

// 统计数量
const totalCount = computed(() => allItems.value.length)
const bookCount = computed(() => allItems.value.filter(item => item.item.itemType === 1).length)
const movieCount = computed(() => allItems.value.filter(item => item.item.itemType === 2).length)
const musicCount = computed(() => allItems.value.filter(item => item.item.itemType === 3).length)

// 获取已标记的物品
const getMarkedItems = async () => {
  loading.value = true
  try {
    const res = await myAxios.get('/item/myMarked')
    if (res?.data?.code === 0) {
      allItems.value = res.data.data
    } else {
      showToast('加载失败')
    }
  } catch (error) {
    console.error('获取已标记物品失败:', error)
    showToast('加载失败')
  } finally {
    loading.value = false
  }
}

// 切换标签
const changeTab = (tab: 'all' | number) => {
  activeTab.value = tab
}

// 跳转到物品详情
const goToItemDetail = (itemId: number) => {
  router.push({
    path: `/item/${itemId}`
  })
}

// 获取物品类型名称
const getItemTypeName = (type: number) => {
  const typeMap: Record<number, string> = {
    1: '书籍',
    2: '电影',
    3: '音乐'
  }
  return typeMap[type] || '未知'
}

// 获取动作名称
const getActionName = (action: number) => {
  const actionMap: Record<number, string> = {
    1: '喜欢',
    2: '收藏',
    3: '看过'
  }
  return actionMap[action] || '标记'
}

// 获取动作样式类
const getActionClass = (action: number) => {
  const classMap: Record<number, string> = {
    1: 'action-like',
    2: 'action-favorite',
    3: 'action-seen'
  }
  return classMap[action] || ''
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  getMarkedItems()
})
</script>

<style scoped>
@import url('https://cdn.jsdelivr.net/npm/remixicon@3.5.0/fonts/remixicon.css');

.nexus-page {
  min-height: 100vh;
  background: transparent;
  color: var(--color-text-primary);
}

.app-shell {
  padding: 18px 16px 120px;
}

.page-title {
  margin-top: 6px;
  margin-bottom: 16px;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-text-secondary);
  margin: 0 0 4px;
}

.title-row {
  font-size: 24px;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(120deg, var(--color-text-primary), var(--color-primary-400));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.title-row .dot {
  color: var(--color-primary-500);
}

.subtitle {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  -webkit-text-fill-color: currentColor;
}

.custom-tabs {
  display: flex;
  gap: 12px;
  margin: 16px 0;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.tab-pill {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border-subtle);
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  flex-shrink: 0;
}

.tab-pill.active {
  background: var(--color-primary-500);
  color: var(--color-text-inverse);
  border-color: var(--color-primary-500);
}

.glass-card {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: 16px;
  backdrop-filter: blur(var(--glass-blur));
}

.items-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.item-card:active {
  transform: scale(0.98);
}

.item-cover {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.item-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-type-badge {
  font-size: 10px;
  color: var(--color-primary-500);
  background: var(--color-primary-100);
  padding: 2px 8px;
  border-radius: 4px;
  width: fit-content;
  font-weight: 600;
}

.item-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-creator {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.item-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  margin-top: 4px;
}

.action-badge {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}

.action-like {
  background: #ffe5e5;
  color: #ff4d4f;
}

.action-favorite {
  background: #fff7e6;
  color: #fa8c16;
}

.action-seen {
  background: #e6f7ff;
  color: #1890ff;
}

.rating {
  color: var(--color-primary-500);
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 2px;
}

.time {
  color: var(--color-text-tertiary);
}

.mt-4 {
  margin-top: 16px;
}
</style>
