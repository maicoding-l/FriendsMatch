<template>
  <div class="nexus-page">
    <div class="app-shell">
      <transition name="fade">
        <section class="panel" key="explore">
          <div class="page-title">
            <p class="eyebrow">Discovery</p>
            <div class="title-row">
              <span>Interest Nexus</span>
              <span class="dot">•</span>
              <span class="subtitle">策展你的兴趣与灵感</span>
            </div>
          </div>

          <van-search
            v-model="searchValue"
            placeholder="搜索书籍 / 电影 / 音乐..."
            shape="round"
            background="rgba(255,255,255,0.02)"
            input-align="left"
          />

          <div class="section-header">
            <div>
              <p class="section-kicker">猜你会喜欢</p>
              <p class="section-sub">基于随机游走算法的个性化推荐</p>
            </div>
            <van-button
              size="small"
              plain
              color="#a29bfe"
              icon="replay"
              :loading="itemLoading"
              @click="refreshItems"
            >
              刷新
            </van-button>
          </div>

          <van-list
            v-model:loading="loadingMore"
            :finished="finished"
            finished-text="- 滑到底啦，去标记更多兴趣吧 -"
            @load="onLoadMore"
            :immediate-check="false"
          >
            <div class="media-grid">
              <div
                v-for="(item, index) in filteredItems"
                :key="item.id ?? index"
                class="media-card"
                @click="toItemDetail(item.id)"
              >
                <img :src="coverFor(item, index)" :alt="item.title" />
                <div class="media-info">
                  <div class="meta-row">
                    <span class="tag" :class="tagClass(item.itemType)">{{
                      typeLabel(item.itemType)
                    }}</span>
                    <span class="score">
                      <i class="ri-star-fill"></i>
                      {{ scoreFor(item.popularity).toFixed(1) }}
                    </span>
                  </div>
                  <div class="media-title">{{ item.title }}</div>
                  <div v-if="item.creator" class="media-sub">{{ item.creator }}</div>
                </div>
              </div>
            </div>
          </van-list>
        </section>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, onActivated, ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { getItemRecommendationsByRandomWalk } from '../api/item'
import { getCurrentUser } from '../api/user'
import type { ItemType } from '../models/item'
import type { UserType } from '../models/user'

const router = useRouter()
const route = useRoute()
const searchValue = ref('')
const items = ref<ItemType[]>([])
const itemLoading = ref(false)
const currentUser = ref<UserType>()
const hasLoadedOnce = ref(false)

// 加载更多相关状态
const loadingMore = ref(false)
const finished = ref(false)
const currentTopN = ref(12)  // 初始加载12个
const LOAD_MORE_SIZE = 12  // 每次加载更多12个
const MAX_CACHE_SIZE = 100  // 缓存最多100个

const toItemDetail = (id: number) => {
  router.push(`/item/${id}`)
}



const mockItems = [
  {
    id: 1,
    title: '三体全套',
    itemType: 1,
    creator: '刘慈欣',
    popularity: 96,
  },
  {
    id: 2,
    title: '奥本海默',
    itemType: 2,
    creator: 'Christopher Nolan',
    popularity: 92,
  },
  {
    id: 3,
    title: 'Dark Side of the Moon',
    itemType: 3,
    creator: 'Pink Floyd',
    popularity: 91,
  },
  {
    id: 4,
    title: '赛博朋克：边缘行者',
    itemType: 2,
    popularity: 89,
  },
  {
    id: 5,
    title: 'Vue.js 设计与实现',
    itemType: 1,
    popularity: 94,
  },
  {
    id: 6,
    title: 'Interstellar OST',
    itemType: 3,
    creator: 'Hans Zimmer',
    popularity: 90,
  },
]

const coverFallbacks = [
  'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400&q=80',
  'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400&q=80',
  'https://images.unsplash.com/photo-1493225255756-d9584f8606e9?w=400&q=80',
  'https://images.unsplash.com/photo-1459749411177-0473ef71607b?w=400&q=80',
  'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=400&q=80',
  'https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=400&q=80',
]

const coverFor = (item: ItemType, index: number) => {
  if (item.coverUrl) return item.coverUrl
  return coverFallbacks[index % coverFallbacks.length]
}

const typeLabel = (type?: number) => {
  if (type === 2) return '电影'
  if (type === 3) return '音乐'
  return '书籍'
}

const tagClass = (type?: number) => {
  if (type === 2) return 'tag-movie'
  if (type === 3) return 'tag-music'
  return 'tag-book'
}

const scoreFor = (popularity?: number) => {
  const normalized = Math.min(99, Math.max(60, popularity ?? 86))
  return normalized / 10
}

const filteredItems = computed(() => {
  const pool = items.value.length ? items.value : mockItems
  if (!searchValue.value) return pool
  const keyword = searchValue.value.toLowerCase()
  return pool.filter(
    (item: any) =>
      item.title?.toLowerCase().includes(keyword) ||
      item.creator?.toLowerCase().includes(keyword) ||
      (item.tags && item.tags.toLowerCase().includes(keyword))
  )
})

const fetchItems = async (topN?: number) => {
  itemLoading.value = true
  try {
    if (!currentUser.value) {
      showToast('请先登录')
      return
    }
    const requestTopN = topN || currentTopN.value
    const res = await getItemRecommendationsByRandomWalk(currentUser.value.id, requestTopN)
    const payload = (res as any)?.data
    if (payload?.code === 0 && Array.isArray(payload.data)) {
      items.value = payload.data
      // 检查是否已加载完所有缓存的物品
      if (payload.data.length < requestTopN || payload.data.length >= MAX_CACHE_SIZE) {
        finished.value = true
      } else {
        finished.value = false
      }
    } else if (payload?.code !== 0) {
      showToast(payload?.description || '获取推荐失败')
    }
  } catch (error) {
    console.error(error)
    showToast('获取推荐失败')
  } finally {
    itemLoading.value = false
  }
}



// 加载更多物品
const onLoadMore = async () => {
  if (finished.value) {
    loadingMore.value = false
    return
  }

  loadingMore.value = true
  try {
    // 增加要请求的物品数量
    currentTopN.value = Math.min(currentTopN.value + LOAD_MORE_SIZE, MAX_CACHE_SIZE)

    if (!currentUser.value) {
      loadingMore.value = false
      return
    }

    const res = await getItemRecommendationsByRandomWalk(currentUser.value.id, currentTopN.value)
    const payload = (res as any)?.data

    if (payload?.code === 0 && Array.isArray(payload.data)) {
      items.value = payload.data

      // 检查是否已加载完所有缓存的物品
      if (payload.data.length < currentTopN.value || payload.data.length >= MAX_CACHE_SIZE) {
        finished.value = true
      }
    }
  } catch (error) {
    console.error('加载更多失败', error)
  } finally {
    loadingMore.value = false
  }
}

// 刷新推荐列表
const refreshItems = () => {
  // 重置加载状态
  currentTopN.value = 12
  finished.value = false
  fetchItems()
}

onMounted(async () => {
  currentUser.value = await getCurrentUser()
  if (currentUser.value) {
    fetchItems()
    hasLoadedOnce.value = true
  }
})

// 监听路由变化，当用户返回首页时自动刷新推荐
watch(() => route.path, (newPath, oldPath) => {
  // 如果已经加载过一次，且现在回到首页，且之前不在首页
  if (hasLoadedOnce.value && newPath === '/' && oldPath && oldPath !== '/') {
    console.log('用户返回首页，自动刷新推荐')
    if (currentUser.value) {
      // 重置加载状态
      currentTopN.value = 12
      finished.value = false
      fetchItems()
    }
  }
})

onBeforeUnmount(() => {
})
</script>

<style scoped>
@import url('https://cdn.jsdelivr.net/npm/remixicon@3.5.0/fonts/remixicon.css');
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;700&display=swap');

:global(:root) {
  --bg-color: transparent;
  --card-bg: rgba(24, 24, 35, 0.6);
  --glass-stroke: rgba(255, 255, 255, 0.08);
  --text-main: #ffffff;
  --text-sub: #9fa6b2;
  --accent: #a29bfe;
  --accent-warm: oklch(68% 0.18 40);  /* Warm coral accent */
}

.nexus-page {
  position: relative;
  min-height: 100vh;
  background: var(--color-surface-base);
  color: var(--color-text-primary);
  overflow: hidden;
  font-family: 'Space Grotesk', 'SF Pro Display', system-ui, -apple-system, sans-serif;
}

.bg-canvas {
  position: fixed;
  inset: 0;
  z-index: 0;
}

.app-shell {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding: 18px 16px 120px;
  box-sizing: border-box;
}

.panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-title {
  margin-top: 6px;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-text-tertiary);
  margin: 0 0 4px;
}

.title-row {
  font-size: 24px;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(120deg, var(--color-text-primary), var(--color-primary-500));
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

.section-header {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.section-kicker {
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, var(--text-main), var(--color-category-music, oklch(78% 0.13 240)));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.section-sub {
  margin: 2px 0 0;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.media-card {
  position: relative;
  overflow: hidden;
  border-radius: 14px;
  aspect-ratio: 2 / 3;
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border-subtle);
  box-shadow: var(--shadow-lg);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  cursor: pointer;
}

.media-card:hover {
  border-color: var(--color-border-default);
  box-shadow: var(--shadow-xl), 0 0 0 1px var(--color-primary-500);
  transform: translateY(-4px);
}

.media-card:active {
  transform: translateY(-2px);
  transition-duration: 0.1s;
}

/* 浅色模式下的卡片特殊样式 */
[data-theme="light"] .media-card {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

[data-theme="light"] .media-card:hover {
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12), 0 0 0 2px var(--color-primary-500);
}

.media-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.media-info {
  position: absolute;
  inset: auto 0 0;
  padding: 10px;
  background: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.8));
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.tag {
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
}

.tag-movie {
  background: var(--color-category-movie-bg, oklch(35% 0.10 15 / 0.15));
  color: var(--color-category-movie, oklch(75% 0.14 15));
  border: 1px solid var(--color-category-movie-subtle, oklch(65% 0.14 15 / 0.25));
}

.tag-book {
  background: var(--color-category-book-bg, oklch(35% 0.08 160 / 0.15));
  color: var(--color-category-book, oklch(78% 0.12 160));
  border: 1px solid var(--color-category-book-subtle, oklch(68% 0.12 160 / 0.25));
}

.tag-music {
  background: var(--color-category-music-bg, oklch(35% 0.09 240 / 0.15));
  color: var(--color-category-music, oklch(78% 0.13 240));
  border: 1px solid var(--color-category-music-subtle, oklch(68% 0.13 240 / 0.25));
}

.score {
  font-size: 12px;
  color: var(--color-rating, oklch(80% 0.14 85));
  display: flex;
  align-items: center;
  gap: 2px;
}

.score i {
  filter: drop-shadow(0 0 4px var(--color-rating-subtle, oklch(80% 0.14 85 / 0.3)));
}

.media-title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 2px;
  line-height: 1.3;
}

.media-sub {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.list-end-hint {
  text-align: center;
  color: var(--text-sub);
  font-size: 12px;
  margin-top: 20px;
  opacity: 0.5;
}
</style>
