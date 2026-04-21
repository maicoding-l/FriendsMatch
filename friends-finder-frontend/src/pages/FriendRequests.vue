<template>
  <div class="friend-requests-page nexus-page">
    <div class="app-shell">
      <!-- 返回按钮 -->
      <div class="back-button" @click="goBack">
        <van-icon name="arrow-left" size="20" />
      </div>

      <div class="page-title">
        <p class="eyebrow">Friend Requests</p>
        <div class="title-row">
          <span>好友申请</span>
          <span class="dot">•</span>
          <span class="subtitle">管理</span>
        </div>
      </div>

      <!-- 标签切换 -->
      <van-tabs v-model:active="activeTab" color="#a29bfe" title-active-color="#a29bfe" class="custom-tabs">
        <van-tab title="收到的申请">
          <van-loading v-if="loadingReceived" size="24px" vertical class="loading-center">加载中...</van-loading>

          <div v-else-if="receivedRequests.length === 0" class="empty-state">
            <van-empty description="暂无收到的好友申请" />
          </div>

          <div v-else class="request-list">
            <div
              v-for="request in receivedRequests"
              :key="request.id"
              class="glass-card request-card"
            >
              <div class="request-header" @click="goToUserProfile(request.fromUser.id)">
                <img
                  class="user-avatar"
                  :src="request.fromUser.avatarUrl || `https://api.dicebear.com/7.x/avataaars/svg?seed=${request.fromUser.username}`"
                  alt="avatar"
                />
                <div class="user-info">
                  <div class="username">{{ request.fromUser.username }}</div>
                  <div class="user-account">@{{ request.fromUser.userAccount }}</div>
                  <div class="request-time">{{ formatTime(request.createTime) }}</div>
                </div>
              </div>

              <div class="request-message" v-if="request.message">
                <van-icon name="chat-o" />
                <span>{{ request.message }}</span>
              </div>

              <div class="request-actions">
                <van-button
                  type="primary"
                  size="small"
                  round
                  color="linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
                  @click="handleAccept(request.id)"
                  :loading="processingRequest === request.id"
                >
                  同意
                </van-button>
                <van-button
                  type="default"
                  size="small"
                  round
                  @click="handleReject(request.id)"
                  :loading="processingRequest === request.id"
                >
                  拒绝
                </van-button>
              </div>
            </div>
          </div>
        </van-tab>

        <van-tab title="发送的申请">
          <van-loading v-if="loadingSent" size="24px" vertical class="loading-center">加载中...</van-loading>

          <div v-else-if="sentRequests.length === 0" class="empty-state">
            <van-empty description="暂无发送的好友申请" />
          </div>

          <div v-else class="request-list">
            <div
              v-for="request in sentRequests"
              :key="request.id"
              class="glass-card request-card"
            >
              <div class="request-header" @click="goToUserProfile(request.toUser.id)">
                <img
                  class="user-avatar"
                  :src="request.toUser.avatarUrl || `https://api.dicebear.com/7.x/avataaars/svg?seed=${request.toUser.username}`"
                  alt="avatar"
                />
                <div class="user-info">
                  <div class="username">{{ request.toUser.username }}</div>
                  <div class="user-account">@{{ request.toUser.userAccount }}</div>
                  <div class="request-time">{{ formatTime(request.createTime) }}</div>
                </div>
              </div>

              <div class="request-message" v-if="request.message">
                <van-icon name="chat-o" />
                <span>{{ request.message }}</span>
              </div>

              <div class="request-status">
                <van-tag
                  v-if="request.status === 0"
                  type="warning"
                  size="medium"
                >
                  待处理
                </van-tag>
                <van-tag
                  v-else-if="request.status === 1"
                  type="success"
                  size="medium"
                >
                  已同意
                </van-tag>
                <van-tag
                  v-else-if="request.status === 2"
                  type="danger"
                  size="medium"
                >
                  已拒绝
                </van-tag>
              </div>
            </div>
          </div>
        </van-tab>
      </van-tabs>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getReceivedRequests, getSentRequests, handleFriendRequest } from '../api/friend';
import { showToast } from 'vant';

const router = useRouter();

const activeTab = ref(0);
const receivedRequests = ref<any[]>([]);
const sentRequests = ref<any[]>([]);
const loadingReceived = ref(true);
const loadingSent = ref(true);
const processingRequest = ref<number | null>(null);

const goBack = () => {
  router.back();
};

const goToUserProfile = (userId: number) => {
  router.push(`/user/profile/${userId}`);
};

const formatTime = (time: string | Date) => {
  const date = new Date(time);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));

  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60));
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60));
      return minutes === 0 ? '刚刚' : `${minutes}分钟前`;
    }
    return `${hours}小时前`;
  } else if (days < 7) {
    return `${days}天前`;
  } else {
    return date.toLocaleDateString();
  }
};

const loadReceivedRequests = async () => {
  try {
    loadingReceived.value = true;
    receivedRequests.value = await getReceivedRequests();
  } catch (error) {
    console.error('加载收到的申请失败', error);
    showToast('加载失败');
  } finally {
    loadingReceived.value = false;
  }
};

const loadSentRequests = async () => {
  try {
    loadingSent.value = true;
    sentRequests.value = await getSentRequests();
  } catch (error) {
    console.error('加载发送的申请失败', error);
    showToast('加载失败');
  } finally {
    loadingSent.value = false;
  }
};

const handleAccept = async (requestId: number) => {
  try {
    processingRequest.value = requestId;
    await handleFriendRequest(requestId, 1); // 1表示同意
    showToast('已同意好友申请');
    // 重新加载列表
    await loadReceivedRequests();
  } catch (error: any) {
    console.error('同意好友申请失败', error);
    showToast(error.response?.data?.message || '操作失败');
  } finally {
    processingRequest.value = null;
  }
};

const handleReject = async (requestId: number) => {
  try {
    processingRequest.value = requestId;
    await handleFriendRequest(requestId, 2); // 2表示拒绝
    showToast('已拒绝好友申请');
    // 重新加载列表
    await loadReceivedRequests();
  } catch (error: any) {
    console.error('拒绝好友申请失败', error);
    showToast(error.response?.data?.message || '操作失败');
  } finally {
    processingRequest.value = null;
  }
};

onMounted(() => {
  loadReceivedRequests();
  loadSentRequests();
});
</script>

<style scoped>
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

.app-shell {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding: 18px 16px 120px;
  box-sizing: border-box;
}

/* 返回按钮 */
.back-button {
  position: absolute;
  top: 18px;
  left: 16px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--card-bg);
  border: 1px solid var(--glass-stroke);
  border-radius: 50%;
  backdrop-filter: blur(10px);
  cursor: pointer;
  color: var(--text-main);
  transition: all 0.3s;
  z-index: 10;
}

.back-button:hover {
  background: rgba(162, 155, 254, 0.2);
  border-color: var(--accent);
}

.page-title {
  margin-top: 50px;
  margin-bottom: 20px;
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

/* Tabs */
.custom-tabs {
  margin-top: 16px;
}

:deep(.van-tabs__wrap) {
  background: var(--card-bg);
  border: 1px solid var(--glass-stroke);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  padding: 4px;
}

:deep(.van-tabs__nav) {
  background: transparent;
}

:deep(.van-tab) {
  color: var(--text-sub);
}

:deep(.van-tab--active) {
  color: var(--accent);
}

:deep(.van-tabs__content) {
  margin-top: 16px;
}

/* Request List */
.request-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.glass-card {
  background: var(--card-bg);
  border: 1px solid var(--glass-stroke);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  padding: 16px;
}

.request-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.request-header {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 2px solid rgba(162, 155, 254, 0.3);
  flex-shrink: 0;
}

.user-info {
  flex: 1;
}

.username {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 2px;
}

.user-account {
  font-size: 13px;
  color: var(--accent);
  margin-bottom: 4px;
}

.request-time {
  font-size: 12px;
  color: var(--text-sub);
}

.request-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: rgba(162, 155, 254, 0.1);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-main);
}

.request-message .van-icon {
  color: var(--accent);
  font-size: 16px;
}

.request-actions {
  display: flex;
  gap: 8px;
}

.request-actions .van-button {
  flex: 1;
}

.request-status {
  display: flex;
  justify-content: flex-end;
}

/* Empty State */
.empty-state {
  padding: 40px 0;
}

:deep(.van-empty__description) {
  color: var(--text-sub);
}

/* Loading */
.loading-center {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  color: var(--text-main);
}

:deep(.van-loading__text) {
  color: var(--text-main);
}
</style>
