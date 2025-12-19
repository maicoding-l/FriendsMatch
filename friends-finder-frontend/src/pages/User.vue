<template>
  <div class="user-page nexus-page">
    <div class="app-shell">
      <div class="page-title">
        <p class="eyebrow">Profile</p>
        <div class="title-row">
          <span>我的</span>
          <span class="dot">•</span>
          <span class="subtitle">兴趣清单与偏好</span>
        </div>
      </div>

      <template v-if="currentUser">
        <div class="glass-card profile-card">
          <img
            class="profile-avatar"
            :src="currentUser.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'"
            alt="avatar"
          />
          <div>
            <div class="profile-name">{{ currentUser.username }}</div>
            <div class="profile-sub">{{ currentUser.profile  }}</div>
          </div>
          <van-button size="small" round plain color="#a29bfe" to="/user/update">编辑</van-button>
        </div>

        <div class="glass-card profile-actions">
           <van-cell title="我创建的小组" is-link to="/user/create" />
           <van-cell title="我加入的小组" is-link to="/user/join" />
           <van-cell title="账号与安全" is-link arrow-direction="right" />
        </div>
      </template>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { getCurrentUser } from '../api/user';
import type { UserType } from '../models/user';

const currentUser = ref<UserType>();

onMounted(async ()=>{
  currentUser.value = await getCurrentUser();
})
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

.glass-card {
  background: var(--card-bg);
  border: 1px solid var(--glass-stroke);
  border-radius: 16px;
  backdrop-filter: blur(10px);
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  margin-top: 24px;
}

.profile-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.1);
}

.profile-name {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 4px;
}

.profile-sub {
  font-size: 13px;
  color: var(--text-sub);
}

.profile-actions {
  margin-top: 16px;
  overflow: hidden;
}

:deep(.van-cell) {
  background: transparent;
  color: #fff;
  padding: 16px 20px;
}

:deep(.van-cell::after) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  left: 20px;
  right: 20px;
}

:deep(.van-cell:last-child::after) {
  display: none;
}
</style>