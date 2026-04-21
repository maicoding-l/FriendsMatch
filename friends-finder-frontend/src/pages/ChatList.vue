<template>
  <div class="chat-list-page nexus-page">
    <div class="app-shell">
      <div class="page-title">
        <p class="eyebrow">Messages</p>
        <div class="title-row">
          <span>消息</span>
          <span class="dot">•</span>
          <span class="subtitle">最近联系人</span>
        </div>
      </div>

      <div v-if="loading" class="loading-container">
        <van-loading size="24px" vertical>加载中...</van-loading>
      </div>

      <div v-else-if="chatList.length === 0" class="empty-container">
        <van-empty description="暂无聊天记录" />
      </div>

      <div v-else class="chat-list">
        <div
          v-for="chat in chatList"
          :key="chat.id"
          class="glass-card chat-item"
          @click="openChat(getOtherUserId(chat))"
        >
          <img
            class="chat-avatar"
            :src="getOtherUserAvatar(chat)"
            alt="avatar"
          />
          <div class="chat-info">
            <div class="chat-header">
              <span class="chat-username">{{ getOtherUsername(chat) }}</span>
              <span class="chat-time">{{ formatTime(chat.createTime) }}</span>
            </div>
            <div class="chat-preview">
              {{ chat.content }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getCurrentUser } from '../api/user'
import { getChatList } from '../api/message'
import type { ChatMessage } from '../composables/useWebSocket'
import type { UserType } from '../models/user'

const router = useRouter()

const currentUser = ref<UserType>()
const chatList = ref<ChatMessage[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    currentUser.value = await getCurrentUser()
    chatList.value = await getChatList()
  } catch (error) {
    showToast('加载失败')
    console.error('加载聊天列表失败:', error)
  } finally {
    loading.value = false
  }
})

const getOtherUserId = (chat: ChatMessage) => {
  return chat.fromUserId === currentUser.value?.id ? chat.toUserId : chat.fromUserId
}

const getOtherUsername = (chat: ChatMessage) => {
  return chat.fromUserId === currentUser.value?.id ? chat.toUsername : chat.fromUsername
}

const getOtherUserAvatar = (chat: ChatMessage) => {
  return chat.fromUserId === currentUser.value?.id
    ? chat.toUserAvatar
    : chat.fromUserAvatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'
}

const formatTime = (time: Date | undefined) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`

  return `${date.getMonth() + 1}/${date.getDate()}`
}

const openChat = (userId: number | undefined) => {
  if (userId) {
    router.push(`/chat/${userId}`)
  }
}
</script>

<style scoped>
.chat-list-page {
  min-height: 100vh;
  background: var(--bg-primary, #0a0a0f);
}

.loading-container,
.empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.chat-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 80px;
}

.chat-item {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.chat-item:hover {
  background: rgba(162, 155, 254, 0.05);
  transform: translateY(-2px);
}

.chat-item:active {
  transform: translateY(0);
}

.chat-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  flex-shrink: 0;
}

.chat-info {
  flex: 1;
  min-width: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.chat-username {
  font-size: 16px;
  font-weight: 600;
  color: #e1e1e6;
}

.chat-time {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
  margin-left: 8px;
}

.chat-preview {
  font-size: 14px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.page-title {
  margin-bottom: 24px;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #a29bfe;
  margin: 0 0 8px 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 32px;
  font-weight: 700;
  color: #e1e1e6;
}

.dot {
  color: #a29bfe;
  font-size: 20px;
}

.subtitle {
  font-size: 18px;
  color: #999;
  font-weight: 400;
}
</style>
