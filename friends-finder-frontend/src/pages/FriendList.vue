<template>
  <div class="friend-list-page nexus-page">
    <div class="app-shell">
      <!-- 返回按钮 -->
      <div class="back-button" @click="goBack">
        <van-icon name="arrow-left" size="20" />
      </div>

      <div class="page-title">
        <p class="eyebrow">My Friends</p>
        <div class="title-row">
          <span>我的好友</span>
          <span class="dot">•</span>
          <span class="subtitle">{{ friends.length }}位</span>
        </div>
      </div>

      <!-- 加载状态 -->
      <van-loading v-if="loading" size="24px" vertical class="loading-center">加载中...</van-loading>

      <!-- 好友列表 -->
      <template v-else>
        <div v-if="friends.length === 0" class="empty-state">
          <van-empty description="暂无好友，快去添加吧~" />
        </div>

        <div v-else class="friends-container">
          <div
            v-for="friend in friends"
            :key="friend.id"
            class="glass-card friend-card"
            @click="goToUserProfile(friend.id)"
          >
            <div class="friend-info">
              <img
                class="friend-avatar"
                :src="friend.avatarUrl || `https://api.dicebear.com/7.x/avataaars/svg?seed=${friend.username}`"
                alt="avatar"
              />
              <div class="friend-details">
                <div class="friend-name">{{ friend.username }}</div>
                <div class="friend-account">@{{ friend.userAccount }}</div>
                <div class="friend-tags" v-if="parseTags(friend.tags).length > 0">
                  <van-tag
                    v-for="tag in parseTags(friend.tags).slice(0, 3)"
                    :key="tag"
                    plain
                    size="small"
                    type="primary"
                    class="tag"
                  >
                    {{ tag }}
                  </van-tag>
                </div>
              </div>
            </div>
            <div class="friend-actions" @click.stop>
              <van-button
                type="primary"
                size="small"
                plain
                round
                @click="goToChat(friend.id)"
                style="margin-right: 8px;"
              >
                聊天
              </van-button>
              <van-button
                type="danger"
                size="small"
                plain
                round
                @click="handleDeleteFriend(friend)"
              >
                删除
              </van-button>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getFriendList, deleteFriend } from '../api/friend';
import type { UserType } from '../models/user';
import { showToast, showDialog } from 'vant';

const router = useRouter();

const friends = ref<UserType[]>([]);
const loading = ref(true);

const goBack = () => {
  router.back();
};

const goToUserProfile = (userId: number) => {
  router.push(`/user/profile/${userId}`);
};

const goToChat = (userId: number) => {
  router.push(`/chat/${userId}`);
};

const parseTags = (tags: any): string[] => {
  if (!tags) return [];
  try {
    return typeof tags === 'string' ? JSON.parse(tags) : tags;
  } catch (error) {
    return [];
  }
};

const loadFriends = async () => {
  try {
    loading.value = true;
    friends.value = await getFriendList();
  } catch (error) {
    console.error('加载好友列表失败', error);
    showToast('加载失败');
  } finally {
    loading.value = false;
  }
};

const handleDeleteFriend = (friend: UserType) => {
  showDialog({
    title: '删除好友',
    message: `确定要删除好友 ${friend.username} 吗？`,
    showCancelButton: true,
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    confirmButtonColor: '#ee0a24'
  }).then(async () => {
    try {
      await deleteFriend(friend.id);
      showToast('已删除好友');
      // 重新加载好友列表
      await loadFriends();
    } catch (error: any) {
      console.error('删除好友失败', error);
      showToast(error.response?.data?.message || '删除失败');
    }
  }).catch(() => {
    // 用户取消
  });
};

onMounted(() => {
  loadFriends();
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

/* Friends Container */
.friends-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.glass-card {
  background: var(--card-bg);
  border: 1px solid var(--glass-stroke);
  border-radius: 16px;
  backdrop-filter: blur(10px);
}

.friend-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.friend-card:hover {
  background: rgba(162, 155, 254, 0.1);
  border-color: var(--accent);
}

.friend-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.friend-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: 2px solid rgba(162, 155, 254, 0.3);
  flex-shrink: 0;
}

.friend-details {
  flex: 1;
  min-width: 0;
}

.friend-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 4px;
}

.friend-account {
  font-size: 13px;
  color: var(--accent);
  margin-bottom: 6px;
}

.friend-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  background: rgba(162, 155, 254, 0.1);
  border-color: rgba(162, 155, 254, 0.3);
  color: #a29bfe;
  font-size: 11px;
}

.friend-actions {
  flex-shrink: 0;
}

/* Empty State */
.empty-state {
  padding: 60px 0;
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
