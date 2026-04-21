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

      <!-- 收藏和评分区域 -->
      <div class="interaction-section">
        <div class="rate-container">
          <div class="rate-label">评分</div>
          <van-rate
            v-model="currentRating"
            :size="28"
            color="oklch(80% 0.14 85)"
            void-color="rgba(255,255,255,0.08)"
            gutter="8px"
            allow-half
            @change="handleRateChange"
          />
          <div class="rate-value" v-if="currentRating > 0">{{ currentRating.toFixed(1) }}</div>
        </div>

        <div class="favorite-container">
          <van-button
            round
            :icon="isFavorited ? 'star' : 'star-o'"
            :type="isFavorited ? 'warning' : 'default'"
            :loading="favoriteLoading"
            @click="handleToggleFavorite"
            class="favorite-btn"
          >
            {{ isFavorited ? '已收藏' : '收藏' }}
          </van-button>
        </div>
      </div>

      <div class="detail-section">
        <h2 class="section-title">简介</h2>
        <p class="description">{{ item.description || '暂无详细介绍' }}</p>
      </div>

      <!-- 短评区域 -->
      <div class="review-section">
        <div class="section-header">
          <h2 class="section-title">短评 ({{ reviews.length }})</h2>
          <van-button
            round
            size="small"
            type="primary"
            icon="edit"
            @click="showReviewDialog = true"
          >
            写短评
          </van-button>
        </div>

        <!-- 短评列表 -->
        <div v-if="reviews.length > 0" class="review-list">
          <div v-for="review in reviews" :key="review.id" class="review-card">
            <div class="review-header">
              <img
                :src="review.user.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + review.user.username"
                class="user-avatar"
              />
              <div class="review-user-info">
                <div class="username">{{ review.user.username }}</div>
                <div class="review-meta">
                  <van-rate
                    v-if="review.rating"
                    :model-value="review.rating"
                    :size="12"
                    readonly
                    color="oklch(80% 0.14 85)"
                    void-color="rgba(255,255,255,0.08)"
                  />
                  <span class="review-time">{{ formatTime(review.createTime) }}</span>
                </div>
              </div>
            </div>
            <p class="review-content">{{ review.content }}</p>
            <div class="review-actions">
              <van-button
                plain
                size="small"
                :type="review.isLiked ? 'primary' : 'default'"
                :icon="review.isLiked ? 'good-job' : 'good-job-o'"
                @click="handleToggleLike(review.id)"
              >
                {{ review.likeCount }}
              </van-button>
            </div>
          </div>
        </div>
        <van-empty v-else description="还没有短评，快来发表第一条吧" />
      </div>
    </div>

    <div v-else-if="loading" class="loading-state">
      <van-loading vertical>加载中...</van-loading>
    </div>

    <van-empty v-else description="未找到相关信息" />

    <!-- 写短评对话框 -->
    <van-dialog
      v-model:show="showReviewDialog"
      title="写短评"
      show-cancel-button
      :before-close="handleReviewSubmit"
    >
      <div class="review-form">
        <van-field
          v-model="reviewContent"
          rows="4"
          type="textarea"
          maxlength="500"
          placeholder="分享你的看法..."
          show-word-limit
        />
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { getItemById, toggleFavorite, rateItem, getUserItemAction } from '../api/item'
import { addReview, getReviewList, toggleLikeReview, type ItemReviewVO } from '../api/review'
import type { ItemType } from '../models/item'

const route = useRoute()
const router = useRouter()
const item = ref<ItemType | null>(null)
const loading = ref(true)

// 收藏和评分状态
const isFavorited = ref(false)
const currentRating = ref(0)
const favoriteLoading = ref(false)

// 短评相关状态
const reviews = ref<ItemReviewVO[]>([])
const showReviewDialog = ref(false)
const reviewContent = ref('')

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

    // 加载用户的交互状态（收藏和评分）
    await fetchUserAction()
  } catch (error) {
    console.error(error)
    showToast('网络请求失败')
    item.value = mockItems[id]
  } finally {
    loading.value = false
  }
}

// 获取用户对当前物品的交互状态
const fetchUserAction = async () => {
  if (!item.value) return

  try {
    const res = await getUserItemAction(item.value.id)
    const payload = res.data as any

    if (payload?.code === 0) {
      const userAction = payload.data
      if (userAction) {
        // action: 1-喜欢 2-收藏 3-看过/读过/听过
        isFavorited.value = userAction.action === 2
        currentRating.value = userAction.weight || 0
      }
    }
  } catch (error) {
    console.error('获取用户交互状态失败', error)
    // 静默失败，不影响主流程
  }
}

// 切换收藏状态
const handleToggleFavorite = async () => {
  if (!item.value) return

  favoriteLoading.value = true
  try {
    const res = await toggleFavorite(item.value.id)
    const payload = res.data as any

    if (payload?.code === 0) {
      isFavorited.value = payload.data
      showToast(isFavorited.value ? '收藏成功' : '取消收藏')
    } else {
      showToast(payload?.description || '操作失败')
    }
  } catch (error) {
    console.error('收藏操作失败', error)
    showToast('操作失败，请稍后重试')
  } finally {
    favoriteLoading.value = false
  }
}

// 评分变化处理
const handleRateChange = async (value: number) => {
  if (!item.value) return

  const loadingToast = showLoadingToast({
    message: '提交评分中...',
    forbidClick: true,
    duration: 0,
  })

  try {
    const res = await rateItem(item.value.id, value)
    const payload = res.data as any

    closeToast()

    if (payload?.code === 0) {
      showToast('评分成功')
      // 重新获取用户状态，因为评分可能影响action状态
      await fetchUserAction()
    } else {
      showToast(payload?.description || '评分失败')
      // 恢复之前的评分
      await fetchUserAction()
    }
  } catch (error) {
    console.error('评分失败', error)
    closeToast()
    showToast('评分失败，请稍后重试')
    // 恢复之前的评分
    await fetchUserAction()
  }
}

// 获取短评列表
const fetchReviews = async () => {
  if (!item.value) return

  try {
    const res = await getReviewList(item.value.id)
    const payload = res.data as any

    if (payload?.code === 0) {
      reviews.value = payload.data || []
    }
  } catch (error) {
    console.error('获取短评失败', error)
    // 静默失败，不影响主流程
  }
}

// 提交短评
const handleReviewSubmit = async (action: string) => {
  if (action === 'confirm') {
    if (!reviewContent.value.trim()) {
      showToast('请输入短评内容')
      return false
    }

    if (!item.value) return false

    const loadingToast = showLoadingToast({
      message: '提交中...',
      forbidClick: true,
      duration: 0,
    })

    try {
      const res = await addReview({
        itemId: item.value.id,
        content: reviewContent.value.trim(),
        rating: currentRating.value > 0 ? currentRating.value : undefined
      })

      closeToast()

      const payload = res.data as any
      if (payload?.code === 0) {
        showToast('发表成功')
        reviewContent.value = ''
        // 重新加载短评列表
        await fetchReviews()
        return true
      } else {
        showToast(payload?.description || '发表失败')
        return false
      }
    } catch (error) {
      console.error('发表短评失败', error)
      closeToast()
      showToast('发表失败，请稍后重试')
      return false
    }
  }
  return true
}

// 点赞/取消点赞短评
const handleToggleLike = async (reviewId: number) => {
  try {
    const res = await toggleLikeReview(reviewId)
    const payload = res.data as any

    if (payload?.code === 0) {
      const isLiked = payload.data
      // 更新本地状态
      const review = reviews.value.find(r => r.id === reviewId)
      if (review) {
        review.isLiked = isLiked
        review.likeCount += isLiked ? 1 : -1
      }
      showToast(isLiked ? '已点赞' : '取消点赞')
    } else {
      showToast(payload?.description || '操作失败')
    }
  } catch (error) {
    console.error('点赞操作失败', error)
    showToast('操作失败，请稍后重试')
  }
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

onMounted(async () => {
  await fetchItemDetails()
  await fetchReviews()
})
</script>

<style scoped>
@import url('https://cdn.jsdelivr.net/npm/remixicon@3.5.0/fonts/remixicon.css');

.item-detail-page {
  min-height: 100vh;
  background: var(--color-surface-base);
  color: var(--color-text-primary);
  padding-bottom: 40px;
}

.custom-nav {
  --van-nav-bar-background: var(--glass-bg);
  --van-nav-bar-text-color: var(--color-text-primary);
  --van-nav-bar-icon-color: var(--color-text-primary);
  --van-nav-bar-title-text-color: var(--color-text-primary);
  backdrop-filter: blur(var(--glass-blur));
  border-bottom: 1px solid var(--color-border-subtle);
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
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border-subtle);
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
  font-size: 14px;
  color: var(--color-rating, oklch(80% 0.14 85));
  display: flex;
  align-items: center;
  gap: 4px;
  filter: drop-shadow(0 0 6px var(--color-rating-subtle, oklch(80% 0.14 85 / 0.25)));
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
  color: var(--color-text-secondary);
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
  color: var(--color-primary-400);
  opacity: 0.9;
}

.interaction-section {
  padding: 24px 20px;
  background: var(--color-surface-overlay);
  border-radius: 16px;
  margin: 16px 16px 0;
  border: 1px solid var(--color-border-subtle);
  backdrop-filter: blur(10px);
}

.rate-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--color-border-subtle);
}

.rate-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.rate-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-rating, oklch(80% 0.14 85));
  min-width: 40px;
  text-align: center;
  text-shadow: 0 0 8px var(--color-rating-subtle, oklch(80% 0.14 85 / 0.4));
}

.favorite-container {
  display: flex;
  justify-content: center;
}

.favorite-btn {
  min-width: 140px;
  height: 44px;
  font-weight: 600;
  font-size: 15px;
  --van-button-default-background: var(--color-surface-overlay);
  --van-button-default-border-color: var(--color-border-default);
  --van-button-default-color: var(--color-text-primary);
  --van-button-warning-background: linear-gradient(135deg, var(--color-warning), var(--color-accent-500));
  --van-button-warning-border-color: transparent;
}

.detail-section {
  padding: 0 20px;
  margin-top: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--color-text-primary);
  border-left: 4px solid var(--color-primary-500);
  padding-left: 12px;
  background: linear-gradient(90deg, var(--color-primary-400), var(--color-text-primary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.description {
  font-size: 15px;
  line-height: 1.6;
  color: var(--color-text-secondary);
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

/* 短评区域样式 */
.review-section {
  margin-top: 24px;
  padding: 0 20px 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-card {
  background: var(--color-surface-overlay);
  border: 1px solid var(--color-border-subtle);
  border-radius: 12px;
  padding: 16px;
  backdrop-filter: blur(10px);
  transition: all 0.2s;
}

.review-card:active {
  transform: scale(0.98);
}

.review-header {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid var(--color-border-default);
}

.review-user-info {
  flex: 1;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.review-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.review-content {
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-secondary);
  margin: 0 0 12px 0;
  word-break: break-word;
}

.review-actions {
  display: flex;
  justify-content: flex-end;
}

.review-form {
  padding: 16px;
}

:deep(.van-field__control) {
  color: var(--color-text-primary);
  background: var(--color-surface-base);
  border-radius: 8px;
  padding: 12px;
}

:deep(.van-dialog) {
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border-subtle);
}

:deep(.van-dialog__header) {
  color: var(--color-text-primary);
}

:deep(.van-dialog__message) {
  color: var(--color-text-secondary);
}
</style>
