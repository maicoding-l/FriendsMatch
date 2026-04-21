<template>
  <div class="post-detail-page nexus-page">
    <div class="app-shell">
      <!-- 帖子详情 -->
      <div v-if="postDetail" class="post-detail glass-card">
        <div class="post-header">
          <van-image
            round
            width="40"
            height="40"
            :src="postDetail.user?.avatarUrl || 'https://via.placeholder.com/40'"
          />
          <div class="post-author">
            <div class="author-name">{{ postDetail.user?.username || '未知用户' }}</div>
            <div class="post-time">{{ formatTime(postDetail.createTime) }}</div>
          </div>
        </div>
        <h2 class="post-title">{{ postDetail.title }}</h2>
        <div class="post-content">{{ postDetail.content }}</div>
        <div class="post-footer">
          <span><i class="ri-eye-line"></i> {{ postDetail.viewCount }}</span>
          <span><i class="ri-chat-3-line"></i> {{ postDetail.commentCount }}</span>
          <span><i class="ri-heart-line"></i> {{ postDetail.likeCount }}</span>
        </div>
      </div>

      <!-- 评论列表 -->
      <div class="comment-section">
        <div class="section-title">评论 ({{ commentList.length }})</div>
        <template v-if="commentLoading">
          <van-skeleton v-for="n in 3" :key="n" title :row="2" class="mt-3" />
        </template>
        <template v-else>
          <van-empty v-if="commentList.length === 0" description="暂无评论" />
          <div v-for="comment in commentList" :key="comment.id" class="comment-item">
            <!-- 一级评论 -->
            <div class="comment-main">
              <van-image
                round
                width="32"
                height="32"
                :src="comment.user?.avatarUrl || 'https://via.placeholder.com/32'"
              />
              <div class="comment-body">
                <div class="comment-user">{{ comment.user?.username || '未知用户' }}</div>
                <div class="comment-content">{{ comment.content }}</div>
                <div class="comment-footer">
                  <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                  <span class="comment-reply" @click="showReplyInput(comment)">回复</span>
                </div>
              </div>
            </div>

            <!-- 二级回复 -->
            <div v-if="comment.replies && comment.replies.length > 0" class="reply-list">
              <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                <van-image
                  round
                  width="28"
                  height="28"
                  :src="reply.user?.avatarUrl || 'https://via.placeholder.com/28'"
                />
                <div class="reply-body">
                  <div class="reply-user">
                    <span>{{ reply.user?.username }}</span>
                    <span v-if="reply.replyToUser" class="reply-to">
                      回复 @{{ reply.replyToUser.username }}
                    </span>
                  </div>
                  <div class="reply-content">{{ reply.content }}</div>
                  <div class="reply-footer">
                    <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                    <span class="reply-reply" @click="showReplyInput(comment, reply)">回复</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 底部评论输入框 -->
    <div class="comment-input-bar">
      <van-field
        v-model="commentContent"
        center
        clearable
        :placeholder="replyPlaceholder"
      >
        <template #button>
          <van-button size="small" type="primary" @click="submitComment">发送</van-button>
        </template>
      </van-field>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import myAxios from '../request'

const route = useRoute()

const postId = ref<number>(Number(route.query.postId))
const postDetail = ref<any>(null)
const commentList = ref<any[]>([])
const commentLoading = ref(true)
const commentContent = ref('')
const replyToComment = ref<any>(null)
const replyToReply = ref<any>(null)

const replyPlaceholder = computed(() => {
  if (replyToReply.value) {
    return `回复 @${replyToReply.value.user?.username}`
  }
  if (replyToComment.value) {
    return `回复 @${replyToComment.value.user?.username}`
  }
  return '写下你的评论...'
})

// 获取帖子详情
const getPostDetail = async () => {
  try {
    const res = await myAxios.get(`/post/${postId.value}`)
    if (res?.data?.code === 0) {
      postDetail.value = res.data.data
    } else {
      showToast('加载帖子失败')
    }
  } catch (error) {
    console.error('获取帖子详情失败:', error)
    showToast('加载失败')
  }
}

// 获取评论列表
const getCommentList = async () => {
  commentLoading.value = true
  try {
    const res = await myAxios.get('/comment/list', {
      params: { postId: postId.value }
    })
    if (res?.data?.code === 0) {
      commentList.value = res.data.data
    } else {
      showToast('加载评论失败')
    }
  } catch (error) {
    console.error('获取评论列表失败:', error)
    showToast('加载失败')
  } finally {
    commentLoading.value = false
  }
}

// 显示回复输入框
const showReplyInput = (comment: any, reply?: any) => {
  replyToComment.value = comment
  replyToReply.value = reply || null
  commentContent.value = ''
}

// 提交评论/回复
const submitComment = async () => {
  if (!commentContent.value.trim()) {
    showToast('请输入评论内容')
    return
  }

  try {
    const data: any = {
      postId: postId.value,
      content: commentContent.value.trim()
    }

    // 如果是回复评论
    if (replyToReply.value) {
      data.parentId = replyToComment.value.id
      data.replyToUserId = replyToReply.value.userId
    } else if (replyToComment.value) {
      data.parentId = replyToComment.value.id
      data.replyToUserId = replyToComment.value.userId
    }

    const res = await myAxios.post('/comment/add', data)

    if (res?.data?.code === 0) {
      showToast('评论成功')
      commentContent.value = ''
      replyToComment.value = null
      replyToReply.value = null
      // 刷新评论列表
      await getCommentList()
      // 刷新帖子详情（更新评论数）
      await getPostDetail()
    } else {
      showToast(res?.data?.message || '评论失败')
    }
  } catch (error) {
    console.error('提交评论失败:', error)
    showToast('评论失败，请重试')
  }
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

onMounted(async () => {
  await getPostDetail()
  await getCommentList()
})
</script>

<style scoped>
@import url('https://cdn.jsdelivr.net/npm/remixicon@3.5.0/fonts/remixicon.css');

.nexus-page {
  min-height: 100vh;
  background: transparent;
  color: var(--color-text-primary);
  padding-bottom: 60px;
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
  margin-bottom: 20px;
}

.post-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.post-author {
  flex: 1;
}

.author-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.post-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.post-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 16px;
  color: var(--color-text-primary);
}

.post-content {
  font-size: 15px;
  color: var(--color-text-primary);
  line-height: 1.8;
  margin-bottom: 16px;
  white-space: pre-wrap;
}

.post-footer {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: var(--color-text-tertiary);
  padding-top: 16px;
  border-top: 1px solid var(--glass-border);
}

.post-footer span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.comment-section {
  margin-top: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 16px;
  color: var(--color-text-primary);
}

.comment-item {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;
}

.comment-main {
  display: flex;
  gap: 12px;
}

.comment-body {
  flex: 1;
}

.comment-user {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--color-text-primary);
}

.comment-content {
  font-size: 14px;
  color: var(--color-text-primary);
  line-height: 1.6;
  margin-bottom: 8px;
}

.comment-footer {
  display: flex;
  gap: 16px;
  align-items: center;
  font-size: 12px;
}

.comment-time {
  color: var(--color-text-tertiary);
}

.comment-reply {
  color: var(--color-primary-500);
  cursor: pointer;
}

.reply-list {
  margin-top: 12px;
  padding-left: 44px;
}

.reply-item {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.reply-body {
  flex: 1;
}

.reply-user {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 4px;
  color: var(--color-text-primary);
}

.reply-to {
  color: var(--color-primary-500);
  margin-left: 4px;
}

.reply-content {
  font-size: 13px;
  color: var(--color-text-primary);
  line-height: 1.6;
  margin-bottom: 6px;
}

.reply-footer {
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 11px;
}

.reply-time {
  color: var(--color-text-tertiary);
}

.reply-reply {
  color: var(--color-primary-500);
  cursor: pointer;
}

.comment-input-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--color-surface-base);
  border-top: 1px solid var(--color-border-subtle);
  padding: 8px 16px;
  z-index: 100;
}

.mt-3 {
  margin-top: 12px;
}
</style>
