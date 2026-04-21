<template>
  <div class="team-detail-page nexus-page">
    <div class="app-shell">
      <!-- 小组信息头部 -->
      <div class="team-header glass-card">
        <div class="team-info">
          <h2 class="team-name">{{ teamInfo?.teamName || '加载中...' }}</h2>
          <p class="team-desc">{{ teamInfo?.description }}</p>
          <div class="team-meta">
            <span><i class="ri-user-3-line"></i> {{ teamInfo?.hasJoinNum }} / {{ teamInfo?.maxNum }}</span>
            <span><i class="ri-file-list-3-line"></i> {{ postList.length }} 帖子</span>
          </div>
        </div>
      </div>

      <!-- 帖子列表 -->
      <div class="post-list">
        <template v-if="loading">
          <van-skeleton v-for="n in 3" :key="n" title :row="3" class="mt-4" />
        </template>
        <template v-else>
          <van-empty v-if="postList.length === 0" description="暂无帖子" />
          <div
            v-for="post in postList"
            :key="post.id"
            class="post-card glass-card"
            @click="goToPostDetail(post.id)"
          >
            <div class="post-header">
              <van-image
                round
                width="32"
                height="32"
                :src="post.user?.avatarUrl || 'https://via.placeholder.com/32'"
              />
              <div class="post-author">
                <div class="author-name">{{ post.user?.username || '未知用户' }}</div>
                <div class="post-time">{{ formatTime(post.createTime) }}</div>
              </div>
            </div>
            <div class="post-title">{{ post.title }}</div>
            <div class="post-content">{{ truncateContent(post.content) }}</div>
            <div class="post-footer">
              <span><i class="ri-eye-line"></i> {{ post.viewCount }}</span>
              <span><i class="ri-chat-3-line"></i> {{ post.commentCount }}</span>
              <span><i class="ri-heart-line"></i> {{ post.likeCount }}</span>
            </div>
          </div>
        </template>
      </div>

      <!-- 发帖按钮 -->
      <div class="fab-btn" @click="goToPostAdd">
        <van-icon name="plus" />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import myAxios from '../request'

const route = useRoute()
const router = useRouter()

const teamId = ref<number>(Number(route.query.teamId))
const teamInfo = ref<any>(null)
const postList = ref<any[]>([])
const loading = ref(true)

// 获取小组信息
const getTeamInfo = async () => {
  try {
    const res = await myAxios.get('/team', {
      params: { id: teamId.value }
    })
    if (res?.data?.code === 0) {
      teamInfo.value = res.data.data
    }
  } catch (error) {
    console.error('获取小组信息失败:', error)
  }
}

// 获取帖子列表
const getPostList = async () => {
  loading.value = true
  try {
    const res = await myAxios.get('/post/list', {
      params: { teamId: teamId.value }
    })
    if (res?.data?.code === 0) {
      postList.value = res.data.data
    } else {
      showToast('加载帖子失败')
    }
  } catch (error) {
    console.error('获取帖子列表失败:', error)
    showToast('加载失败')
  } finally {
    loading.value = false
  }
}

// 跳转到帖子详情
const goToPostDetail = (postId: number) => {
  router.push({
    path: '/post/detail',
    query: { postId }
  })
}

// 跳转到发帖页面
const goToPostAdd = () => {
  router.push({
    path: '/post/add',
    query: { teamId: teamId.value }
  })
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (days > 0) return `${days}天前`
  if (hours > 0) return `${hours}小时前`
  if (minutes > 0) return `${minutes}分钟前`
  return '刚刚'
}

// 截断内容
const truncateContent = (content: string) => {
  if (!content) return ''
  return content.length > 100 ? content.substring(0, 100) + '...' : content
}

onMounted(async () => {
  await getTeamInfo()
  await getPostList()
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
  padding: 16px;
  padding-bottom: 100px;
}

.glass-card {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: 16px;
  backdrop-filter: blur(var(--glass-blur));
  padding: 16px;
}

.team-header {
  margin-bottom: 20px;
}

.team-name {
  font-size: 24px;
  font-weight: 800;
  margin: 0 0 8px;
  color: var(--color-text-primary);
}

.team-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0 0 12px;
}

.team-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.team-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-card {
  cursor: pointer;
  transition: all 0.2s;
}

.post-card:active {
  transform: scale(0.98);
}

.post-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.post-author {
  flex: 1;
}

.author-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.post-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.post-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--color-text-primary);
}

.post-content {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
  line-height: 1.6;
}

.post-footer {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.post-footer span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.fab-btn {
  position: fixed;
  bottom: 80px;
  right: 24px;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: var(--color-primary-500);
  color: var(--color-text-inverse);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  box-shadow: 0 4px 12px var(--shadow-lg);
  z-index: 10;
  cursor: pointer;
}

.mt-4 {
  margin-top: 16px;
}
</style>
