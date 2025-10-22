<template>
        <van-card
        v-for="user in props.usersList"
  :key="user.id"
  :desc="user.profile"
  :title="user.username"
  :thumb="user.avatarUrl"
>
  <template #tags>
    <van-tag v-for="tag in user.tags" :key="tag" plain type="primary" style="margin-left: 8px;margin-top: 4px;">{{ tag }}</van-tag>
  </template>
  <template #footer>
    <van-button size="mini">联系我</van-button>
  </template>
</van-card>
</template>

<script setup lang="ts">
import type { UserType } from '../models/user';
interface UserCardListProps{
    usersList?: UserType[];
}

const props = withDefaults(defineProps<UserCardListProps>(),{
    usersList: () => ([] as UserType[])
})

</script>

<style>
  /* 标签颜色*/
  .van-tag--danger.van-tag--plain {
    color: #002fff;
  }
</style>