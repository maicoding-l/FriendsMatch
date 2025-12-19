<template>
  <div class="item-detail-page">
    <van-nav-bar
      title="详情"
      left-arrow
      @click-left="onClickLeft"
      fixed
      placeholder
      :border="false"
      class="custom-nav"
    />

    <div class="content" v-if="item">
      <div class="header-section">
        <div class="cover-wrapper">
          <img :src="item.coverUrl || defaultCover" :alt="item.title" class="cover-image" />
          <div class="glass-overlay"></div>
        </div>
        
        <div class="info-card">
          <div class="meta-row">
            <span class="tag" :class="tagClass(item.itemType)">
              {{ typeLabel(item.itemType) }}
            </span>
            <span class="score" v-if="item.popularity">
              <i class="ri-star-fill"></i>
              {{ (item.popularity / 10).toFixed(1) }}
            </span>
          </div>
          <h1 class="title">{{ item.title }}</h1>
          <div class="subtitle-row">
            <span class="creator" v-if="item.creator">{{ item.creator }}</span>
            <span class="year" v-if="item.publishYear"> · {{ item.publishYear }}</span>
          </div>
          <div class="tags" v-if="item.tags">
            <span v-for="tag in item.tags.split(',')" :key="tag" class="small-tag">
              #{{ tag }}
            </span>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <h2 class="section-title">简介</h2>
        <p class="description">{{ item.description || '暂无详细介绍' }}</p>
      </div>

      <div class="action-bar">
        <van-button block round type="primary" color="linear-gradient(to right, #6c5ce7, #a29bfe)">
          标记兴趣
        </van-button>
      </div>
    </div>

    <div v-else-if="loading" class="loading-state">
      <van-loading vertical>加载中...</van-loading>
    </div>
    
    <van-empty v-else description="未找到相关信息" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getItemById } from '../api/item'
import type { ItemType } from '../models/item'

const route = useRoute()
const router = useRouter()
const item = ref<ItemType | null>(null)
const loading = ref(true)

const defaultCover = 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400&q=80'

const onClickLeft = () => {
  router.back()
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

// Mock data for demo if fetch fails
const mockItems: Record<string, any> = {
  '1': {
    id: 1,
    title: '三体全套',
    itemType: 1,
    creator: '刘慈欣',
    publishYear: 2008,
    popularity: 96,
    description: '《三体》是刘慈欣创作的系列长篇科幻小说，由《三体》、《三体2：黑暗森林》、《三体3：死神永生》组成，作品讲述了地球人类文明和三体文明的信息交流、生死搏杀及两个文明在宇宙中的兴衰历程。其第一部经过刘宇昆翻译后获得了第73届雨果奖最佳长篇小说奖。',
    tags: '科幻,经典,硬科学'
  },
  '2': {
    id: 2,
    title: '奥本海默',
    itemType: 2,
    creator: 'Christopher Nolan',
    publishYear: 2023,
    popularity: 92,
    description: '影片改编自凯·伯德、马丁·J·舍温所著的传记《奥本海默传：美国“原子弹之父”的胜利与悲剧》，讲述了美国“原子弹之父”罗伯特·奥本海默在二战期间领导曼哈顿计划，研制出原子弹的过程，以及他在战后由于对核武器的担忧而遭受政治审查的故事。',
    tags: '传记,历史,原子弹'
  },
  '3': {
     id: 3,
     title: 'Dark Side of the Moon',
     itemType: 3,
     creator: 'Pink Floyd',
     publishYear: 1973,
     popularity: 91,
     description: '《The Dark Side of the Moon》是英国摇滚乐队平克·弗洛伊德的第八张录音室专辑，发行于1973年3月1日。它是历史上最畅销的专辑之一，在公告牌二百强专辑榜上停留了创纪录的900多周。',
     tags: '摇滚,迷幻,经典'
  }
}

const fetchItemDetails = async () => {
  const id = route.params.id as string
  loading.value = true
  
  try {
    const res = await getItemById(id)
    const payload = res.data as any
    if (payload?.code === 0 && payload.data) {
      item.value = payload.data
    } else {
      showToast(payload?.description || '获取详情失败，展示演示数据')
      // Fallback to mock for demo
      item.value = mockItems[id] || {
        id: Number(id),
        title: '未知资源',
        itemType: 1,
        popularity: 80,
        description: '未从后端获取到数据，请检查服务连接。'
      }
    }
  } catch (error) {
    console.error(error)
    showToast('网络请求失败')
    item.value = mockItems[id]
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchItemDetails()
})
</script>

<style scoped>
@import url('https://cdn.jsdelivr.net/npm/remixicon@3.5.0/fonts/remixicon.css');

.item-detail-page {
  min-height: 100vh;
  background: #12121e;
  color: #ffffff;
  padding-bottom: 40px;
}

.custom-nav {
  --van-nav-bar-background: rgba(18, 18, 30, 0.8);
  --van-nav-bar-text-color: #fff;
  --van-nav-bar-icon-color: #fff;
  --van-nav-bar-title-text-color: #fff;
  backdrop-filter: blur(10px);
}

.header-section {
  position: relative;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.cover-wrapper {
  position: relative;
  width: 160px;
  aspect-ratio: 2 / 3;
  margin: 0 auto;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-card {
  text-align: center;
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 12px;
}

.tag {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
}

.tag-movie { background: rgba(231, 76, 60, 0.3); color: #ff7675; border: 1px solid rgba(231, 76, 60, 0.3); }
.tag-book { background: rgba(46, 204, 113, 0.3); color: #55efc4; border: 1px solid rgba(46, 204, 113, 0.3); }
.tag-music { background: rgba(52, 152, 219, 0.3); color: #74b9ff; border: 1px solid rgba(52, 152, 219, 0.3); }

.score {
  font-size: 14px;
  color: #f1c40f;
  display: flex;
  align-items: center;
  gap: 4px;
}

.title {
  font-size: 24px;
  font-weight: 800;
  margin: 0 0 8px;
  background: linear-gradient(120deg, #fff, #a29bfe);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-bottom: 16px;
  color: #9fa6b2;
}

.creator {
  font-size: 16px;
}

.year {
  font-size: 14px;
  opacity: 0.8;
}

.tags {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
}

.small-tag {
  font-size: 12px;
  color: #a29bfe;
  opacity: 0.8;
}

.detail-section {
  padding: 0 20px;
  margin-top: 10px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 12px;
  color: #fff;
  border-left: 4px solid #6c5ce7;
  padding-left: 12px;
}

.description {
  font-size: 15px;
  line-height: 1.6;
  color: #ced6e0;
  text-align: justify;
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 24px 32px;
  background: linear-gradient(0deg, #12121e 70%, transparent);
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 50vh;
}
</style>
