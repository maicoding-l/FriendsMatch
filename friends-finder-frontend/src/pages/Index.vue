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
              <p class="section-sub">简单按照热度取前几条，先看这里</p>
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

          <div class="list-end-hint">- 滑到底啦，去标记更多兴趣吧 -</div>
        </section>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getItemRecommendations } from '../api/item'
import type { ItemType } from '../models/item'

const router = useRouter()
const searchValue = ref('')
const items = ref<ItemType[]>([])
const itemLoading = ref(false)

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

const fetchItems = async () => {
  itemLoading.value = true
  try {
    const res = await getItemRecommendations(8)
    const payload = (res as any)?.data
    if (payload?.code === 0 && Array.isArray(payload.data)) {
      items.value = payload.data
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



const refreshItems = () => {
  fetchItems()
}

onMounted(() => {
  fetchItems()
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
}

.nexus-page {
  position: relative;
  min-height: 100vh;
  background: var(--bg-color);
  color: var(--text-main);
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
  color: var(--text-sub);
  margin: 0 0 4px;
}

.title-row {
  font-size: 24px;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(120deg, #fff, #a29bfe);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.title-row .dot {
  color: #6c5ce7;
}

.subtitle {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-sub);
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
}

.section-sub {
  margin: 2px 0 0;
  color: var(--text-sub);
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
  background: var(--card-bg);
  border: 1px solid var(--glass-stroke);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.35);
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
  background: rgba(231, 76, 60, 0.25);
}

.tag-book {
  background: rgba(46, 204, 113, 0.25);
}

.tag-music {
  background: rgba(52, 152, 219, 0.25);
}

.score {
  font-size: 12px;
  color: #f1c40f;
  display: flex;
  align-items: center;
  gap: 2px;
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
