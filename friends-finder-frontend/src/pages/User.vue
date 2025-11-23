<template>
  <template v-if="currentUser">
    <van-cell title="当前用户" :value="currentUser?.username" />
    <van-cell title="修改信息" is-link to="/user/update" />
    <van-cell title="我创建的队伍" is-link to="/user/create" />
    <van-cell title="我加入的队伍" is-link to="/user/join" />
  </template>
</template>

<script lang="ts" setup>
import { ref,onMounted } from 'vue';
import { getCurrentUser } from '../api/user';
import type { UserType } from '../models/user';

const currentUser = ref<UserType>();
onMounted(async ()=>{
  currentUser.value = await getCurrentUser();
})
</script>

<style scoped>

</style>