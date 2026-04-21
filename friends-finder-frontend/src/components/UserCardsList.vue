<template>
  <van-skeleton title avatar :row="3" v-for="user in props.usersList" :loading="loading" />
  <van-card
    v-for="user in props.usersList"
    :key="user.id"
    :desc="user.profile"
    :title="user.username"
    :thumb="user.avatarUrl"
    @click="goToUserProfile(user.id)"
    class="user-card"
  >
    <template #tags>
      <van-tag
        v-for="tag in user.tags"
        :key="tag"
        plain
        type="primary"
        style="margin-left: 8px; margin-top: 4px"
        >{{ tag }}</van-tag
      >
    </template>
    <template #footer>
      <van-button size="mini" @click.stop="handleContact(user)">联系我</van-button>
    </template>
  </van-card>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { showToast } from 'vant';
import type { UserType } from '../models/user';

interface UserCardListProps {
  usersList?: UserType[]
  loading: boolean
}

const props = withDefaults(defineProps<UserCardListProps>(), {
  usersList: () => [] as UserType[],
  loading: true,
})

const router = useRouter();

// 跳转到用户主页
const goToUserProfile = (userId: number) => {
  router.push(`/user/profile/${userId}`);
};

// 联系用户
const handleContact = (user: UserType) => {
  showToast(`联系 ${user.username}`);
  // TODO: 实现联系功能
};
</script>

<style scoped>
/* 用户卡片样式 */
.user-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  border: 1px solid transparent;
}

.user-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(162, 155, 254, 0.2), 0 0 0 1px oklch(55% 0.18 280 / 0.12);
  border-color: oklch(55% 0.18 280 / 0.15);
}

.user-card:active {
  transform: translateY(0);
  transition-duration: 0.1s;
}

/* 标签颜色 - 使用温暖的主题色 */
:deep(.van-tag--primary) {
  background: var(--color-primary-500, oklch(55% 0.18 280 / 0.15));
  color: var(--color-primary-300, oklch(75% 0.12 280));
  border-color: var(--color-primary-400, oklch(65% 0.15 280 / 0.3));
}

:deep(.van-tag--primary.van-tag--plain) {
  background: oklch(55% 0.18 280 / 0.08);
  color: var(--color-primary-300, oklch(75% 0.12 280));
  border-color: oklch(55% 0.18 280 / 0.25);
}

:deep(.van-tag--danger.van-tag--plain) {
  background: var(--color-accent-500, oklch(62% 0.20 30 / 0.08));
  color: var(--color-accent-300, oklch(75% 0.16 30));
  border-color: oklch(62% 0.20 30 / 0.25);
}

/* 按钮颜色增强 */
:deep(.van-button--mini) {
  background: linear-gradient(135deg, var(--color-primary-500, oklch(55% 0.18 280)), var(--color-accent-500, oklch(62% 0.20 30)));
  border: none;
  color: white;
  font-weight: 500;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

:deep(.van-button--mini:hover) {
  transform: scale(1.05);
  box-shadow: 0 4px 12px oklch(55% 0.18 280 / 0.3);
}

:deep(.van-button--mini:active) {
  transform: scale(0.98);
}
</style>
