<template>
  <div class="chat-page nexus-page">
    <van-nav-bar
      :title="targetUser?.username || '聊天'"
      left-arrow
      @click-left="onBack"
    />

    <div class="chat-container">
      <div class="messages-list" ref="messagesList">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-item"
          :class="{ 'message-mine': msg.fromUserId === currentUserId }"
        >
          <img
            class="message-avatar"
            :src="msg.fromUserId === currentUserId ? currentUser?.avatarUrl : targetUser?.avatarUrl"
            alt="avatar"
          />
          <div class="message-content">
            <div class="message-username">
              {{ msg.fromUserId === currentUserId ? currentUser?.username : targetUser?.username }}
            </div>
            <div class="message-bubble">
              {{ msg.content }}
            </div>
            <div class="message-time">
              {{ formatTime(msg.createTime) }}
            </div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <van-field
          v-model="inputMessage"
          placeholder="输入消息..."
          @keyup.enter="sendMessage"
        />
        <van-button
          type="primary"
          size="small"
          @click="sendMessage"
          :disabled="!inputMessage.trim()"
        >
          发送
        </van-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { getCurrentUser, getUserById } from '../api/user'
import { getChatHistory } from '../api/message'
import { useWebSocket, type ChatMessage } from '../composables/useWebSocket'
import type { UserType } from '../models/user'

const route = useRoute()
const router = useRouter()

const targetUserId = ref<number>(Number(route.params.userId))
const targetUser = ref<UserType>()
const currentUser = ref<UserType>()
const currentUserId = ref<number>()

const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const messagesList = ref<HTMLDivElement>()

const { isConnected, connect, disconnect, sendMessage: wsSendMessage, onMessage } = useWebSocket()

onMounted(async () => {
  const loading = showLoadingToast({
    message: '加载中...',
    forbidClick: true,
  })

  try {
    // 获取当前用户
    currentUser.value = await getCurrentUser()
    currentUserId.value = currentUser.value?.id

    // 获取目标用户信息
    targetUser.value = await getUserById(targetUserId.value)

    // 加载聊天历史
    messages.value = await getChatHistory(targetUserId.value)

    // 连接WebSocket
    connect()

    // 监听新消息
    onMessage((message: ChatMessage) => {
      // 只接收与当前聊天对象相关的消息
      if (
        (message.fromUserId === targetUserId.value && message.toUserId === currentUserId.value) ||
        (message.fromUserId === currentUserId.value && message.toUserId === targetUserId.value)
      ) {
        messages.value.push(message)
        scrollToBottom()
      }
    })

    closeToast()
    scrollToBottom()
  } catch (error) {
    closeToast()
    showToast('加载失败')
    console.error('加载聊天页面失败:', error)
  }
})

onUnmounted(() => {
  disconnect()
})

const sendMessage = () => {
  if (!inputMessage.value.trim()) return
  if (!isConnected.value) {
    showToast('连接已断开，请刷新页面')
    return
  }

  try {
    wsSendMessage({
      toUserId: targetUserId.value,
      content: inputMessage.value,
      type: 0
    })

    inputMessage.value = ''
    scrollToBottom()
  } catch (error) {
    showToast('发送失败')
    console.error('发送消息失败:', error)
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesList.value) {
      messagesList.value.scrollTop = messagesList.value.scrollHeight
    }
  })
}

const formatTime = (time: Date | undefined) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`

  return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

const onBack = () => {
  router.back()
}
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg-primary, #0a0a0f);
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.messages-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message-mine {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 60%;
}

.message-mine .message-content {
  align-items: flex-end;
}

.message-username {
  font-size: 12px;
  color: #999;
}

.message-bubble {
  background: rgba(162, 155, 254, 0.1);
  border: 1px solid rgba(162, 155, 254, 0.2);
  padding: 12px 16px;
  border-radius: 16px;
  color: #e1e1e6;
  word-wrap: break-word;
}

.message-mine .message-bubble {
  background: rgba(162, 155, 254, 0.3);
  border-color: rgba(162, 155, 254, 0.4);
}

.message-time {
  font-size: 11px;
  color: #666;
}

.input-area {
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.02);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  gap: 12px;
  align-items: center;
}

.input-area .van-field {
  flex: 1;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  padding: 8px 16px;
}

.input-area .van-button {
  flex-shrink: 0;
}
</style>
