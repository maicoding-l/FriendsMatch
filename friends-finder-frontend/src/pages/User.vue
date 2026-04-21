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
           <van-cell title="我的消息" is-link to="/chat">
             <template #icon>
               <i class="ri-message-3-line action-icon"></i>
             </template>
           </van-cell>
           <van-cell title="我的好友" is-link to="/friend/list">
             <template #icon>
               <i class="ri-user-heart-line action-icon"></i>
             </template>
           </van-cell>
           <van-cell title="好友申请" is-link to="/friend/requests">
             <template #icon>
               <i class="ri-user-add-line action-icon"></i>
             </template>
           </van-cell>
           <van-cell title="我的标记" is-link to="/my-marked">
             <template #icon>
               <i class="ri-bookmark-line action-icon"></i>
             </template>
           </van-cell>
           <van-cell title="我创建的小组" is-link to="/user/create" />
           <van-cell title="我加入的小组" is-link to="/user/join" />
           <van-cell title="账号与安全" is-link arrow-direction="right" />
           <van-cell title="设置" is-link to="/settings">
             <template #icon>
               <i class="ri-settings-3-line action-icon"></i>
             </template>
           </van-cell>
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
@import url('https://cdn.jsdelivr.net/npm/remixicon@3.5.0/fonts/remixicon.css');

/* Using design tokens - no need to import fonts here (loaded globally) */

.user-page {
  position: relative;
  min-height: 100vh;
}

.app-shell {
  position: relative;
  padding: var(--space-fluid-md) var(--space-4) var(--space-24);
}

/* Page Header - using standard component pattern */
.page-title {
  margin-bottom: var(--space-6);
}

.eyebrow {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-widest);
  text-transform: uppercase;
  color: var(--color-text-tertiary);
  margin-bottom: var(--space-2);
}

.title-row {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-primary);
}

.title-row .dot {
  color: var(--color-primary-400);
}

.subtitle {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-normal);
  color: var(--color-text-secondary);
}

/* Profile Card - using card component pattern */
.glass-card {
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  transition: all var(--duration-base) var(--ease-out);
}

.profile-card {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-5);
  margin-top: var(--space-6);
}

.profile-avatar {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-full);
  border: 2px solid var(--color-border-default);
}

.profile-name {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.profile-sub {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.profile-actions {
  margin-top: var(--space-4);
  overflow: hidden;
}

.action-icon {
  font-size: 18px;
  margin-right: var(--space-3);
  color: var(--color-text-secondary);
}

/* Van cell customization */
:deep(.van-cell) {
  background: transparent;
  color: var(--color-text-primary);
  padding: var(--space-4) var(--space-5);
}

:deep(.van-cell::after) {
  border-bottom: 1px solid var(--color-border-subtle);
  left: var(--space-5);
  right: var(--space-5);
}

:deep(.van-cell:last-child::after) {
  display: none;
}
</style>