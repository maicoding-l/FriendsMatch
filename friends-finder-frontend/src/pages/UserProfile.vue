<template>
  <div class="user-profile-page nexus-page">
    <div class="app-shell">
      <!-- 返回按钮 -->
      <div class="back-button" @click="goBack">
        <van-icon name="arrow-left" size="20" />
      </div>

      <div class="page-title">
        <p class="eyebrow">User Profile</p>
        <div class="title-row">
          <span>用户</span>
          <span class="dot">•</span>
          <span class="subtitle">个人主页</span>
        </div>
      </div>

      <!-- 加载状态 -->
      <van-loading v-if="loading" size="24px" vertical class="loading-center">加载中...</van-loading>

      <!-- 用户信息 -->
      <template v-if="!loading && user">
        <!-- 头像和基本信息卡片 -->
        <div class="glass-card profile-card">
          <img
            class="profile-avatar"
            :src="user.avatarUrl || `https://api.dicebear.com/7.x/avataaars/svg?seed=${user.username}`"
            alt="avatar"
          />
          <div class="profile-info">
            <div class="profile-name">{{ user.username }}</div>
            <div class="profile-account">@{{ user.userAccount }}</div>
            <div class="profile-sub" v-if="user.profile">{{ user.profile }}</div>
          </div>
        </div>

        <!-- 用户标签 -->
        <div class="glass-card tags-card" v-if="userTags && userTags.length > 0">
          <div class="section-title">兴趣标签</div>
          <div class="tags-container">
            <van-tag
              v-for="tag in userTags"
              :key="tag"
              round
              plain
              type="primary"
              size="medium"
              class="user-tag"
            >
              {{ tag }}
            </van-tag>
          </div>
        </div>

        <!-- 详细信息 -->
        <div class="glass-card info-card">
          <div class="section-title">详细信息</div>
          <van-cell-group :border="false" class="info-cell-group">
            <van-cell title="性别" :value="user.gender === 0 ? '女' : user.gender === 1 ? '男' : '未知'" />
            <van-cell title="邮箱" :value="user.email || '未填写'" />
            <van-cell title="电话" :value="user.phone || '未填写'" />
            <van-cell title="星球编号" :value="user.planetCode || '未填写'" />
            <van-cell
              title="注册时间"
              :value="user.createTime ? new Date(user.createTime).toLocaleDateString() : '未知'"
            />
          </van-cell-group>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <van-button
            v-if="!isFriend && !isCurrentUser"
            type="primary"
            round
            block
            color="linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
            @click="handleAddFriend"
            :loading="addingFriend"
          >
            加好友
          </van-button>
          <van-button
            v-else-if="isFriend"
            type="success"
            round
            block
            disabled
          >
            已是好友
          </van-button>
          <van-button
            v-if="!isCurrentUser"
            type="default"
            round
            block
            @click="handleContact"
            style="margin-top: 12px"
          >
            联系TA
          </van-button>
        </div>
      </template>

      <!-- 用户不存在 -->
      <van-empty
        v-if="!loading && !user"
        description="用户不存在或已被删除"
        image="error"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getUserById, getCurrentUser } from '../api/user';
import { checkFriend, sendFriendRequest } from '../api/friend';
import type { UserType } from '../models/user';
import { showToast, showDialog } from 'vant';

const route = useRoute();
const router = useRouter();

const user = ref<UserType | null>(null);
const currentUser = ref<UserType | null>(null);
const loading = ref(true);
const isFriend = ref(false);
const addingFriend = ref(false);

// 解析用户标签（从 JSON 字符串转为数组）
const userTags = computed(() => {
  if (!user.value?.tags) return [];
  try {
    return typeof user.value.tags === 'string'
      ? JSON.parse(user.value.tags)
      : user.value.tags;
  } catch (error) {
    console.error('解析标签失败', error);
    return [];
  }
});

// 是否是当前用户自己
const isCurrentUser = computed(() => {
  return currentUser.value?.id === user.value?.id;
});

// 返回上一页
const goBack = () => {
  router.back();
};

// 联系用户
const handleContact = () => {
  showToast('联系功能开发中...');
};

// 加好友
const handleAddFriend = async () => {
  if (!user.value?.id) return;

  showDialog({
    title: '发送好友申请',
    message: '确定向 ' + user.value.username + ' 发送好友申请吗？',
    showCancelButton: true,
    confirmButtonText: '发送',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      addingFriend.value = true;
      await sendFriendRequest(user.value!.id, '你好，我想加你为好友');
      showToast('好友申请已发送');
      // 不立即更新isFriend状态，因为需要对方同意
    } catch (error: any) {
      console.error('发送好友申请失败', error);
      showToast(error.response?.data?.message || '发送失败');
    } finally {
      addingFriend.value = false;
    }
  }).catch(() => {
    // 用户取消
  });
};

// 加载用户信息
onMounted(async () => {
  const userId = route.params.id;

  if (!userId || isNaN(Number(userId))) {
    showToast('无效的用户ID');
    loading.value = false;
    return;
  }

  try {
    loading.value = true;
    // 并行加载用户信息和当前用户信息
    const [userData, currentUserData] = await Promise.all([
      getUserById(Number(userId)),
      getCurrentUser()
    ]);

    user.value = userData;
    currentUser.value = currentUserData;

    // 如果不是当前用户自己，检查好友关系
    if (currentUserData?.id !== userData?.id) {
      try {
        isFriend.value = await checkFriend(Number(userId));
      } catch (error) {
        console.error('检查好友关系失败', error);
      }
    }
  } catch (error) {
    console.error('获取用户信息失败', error);
    showToast('获取用户信息失败');
  } finally {
    loading.value = false;
  }
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

.glass-card {
  background: var(--card-bg);
  border: 1px solid var(--glass-stroke);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  margin-top: 16px;
}

/* 个人资料卡片 */
.profile-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 24px;
  text-align: center;
}

.profile-avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  border: 3px solid rgba(162, 155, 254, 0.3);
  box-shadow: 0 8px 24px rgba(162, 155, 254, 0.2);
  margin-bottom: 16px;
}

.profile-info {
  width: 100%;
}

.profile-name {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 6px;
}

.profile-account {
  font-size: 14px;
  color: var(--accent);
  margin-bottom: 8px;
}

.profile-sub {
  font-size: 14px;
  color: var(--text-sub);
  line-height: 1.6;
}

/* 标签卡片 */
.tags-card {
  padding: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 12px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.user-tag {
  background: rgba(162, 155, 254, 0.1);
  border-color: rgba(162, 155, 254, 0.3);
  color: #a29bfe;
}

/* 详细信息卡片 */
.info-card {
  padding: 20px;
  overflow: hidden;
}

.info-cell-group {
  background: transparent;
}

:deep(.van-cell) {
  background: transparent;
  color: #fff;
  padding: 12px 0;
}

:deep(.van-cell__title) {
  color: var(--text-sub);
}

:deep(.van-cell__value) {
  color: var(--text-main);
}

:deep(.van-cell::after) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

:deep(.van-cell:last-child::after) {
  display: none;
}

/* 操作按钮 */
.action-buttons {
  margin-top: 24px;
  padding: 0 4px;
}

/* 加载状态 */
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

/* Empty 状态 */
:deep(.van-empty__description) {
  color: var(--text-sub);
}
</style>
