<template>
  <div v-if="user">
  <van-cell
    title="昵称"
    is-link
    :value="user.username"
    @click="toEdit('username', '昵称', user.username)"
  />
  <van-cell title="账号" :value="user.userAccount" />
  <van-cell
    title="头像"
    is-link
    @click="toEdit('avatarUrl', '头像', user.avatarUrl)"
  >
    <img :src="user.avatarUrl" style="height: 90px" alt="头像图片" />
  </van-cell>
  <van-cell
    title="性别"
    is-link
    :value="user.gender"
    @click="toEdit('gender', '性别', user.gender)"
  />
  <van-cell
    title="手机号"
    is-link
    :value="user.phone"
    @click="toEdit('phone', '手机号', user.phone)"
  />
  <van-cell
    title="邮箱"
    is-link
    :value="user.email"
    @click="toEdit('email', '邮箱', user.email)"
  />
  <van-cell title="星球编号" :value="user.planetCode" />
  <van-cell title="标签" is-link :value="user.tags" />
  <van-cell 
    title="个人简介"
    is-link 
    :value="user.profile"
    @click="toEdit('profile', '个人简介', user.profile)"
   />
  <van-cell title="创建时间" :value="user.createTime ? formatDate(user.createTime) : ''" />
  </div>
</template>
<script setup lang="ts">
import router from '../router/index.ts'
import { showToast } from 'vant'
import { getCurrentUser } from '../api/user.ts'
import { onMounted } from 'vue'
import { ref } from 'vue'
import type { UserType } from '../models/user'

const user = ref<UserType | null>(null);

// const user = {
//   id: 1,
//   username: '小麦',
//   userAccount: 'xiaomai',
//   avatarUrl:
//     'https://q9.itc.cn/q_70/images03/20250110/e27c5429d0d945dc9bdb50cb00b21b2e.jpeg',
//   gender: '0',
//   phone: '13072189792',
//   email: '651732872@qq.com',
//   userState: 0,
//   userRole: 1,
//   planetCode: 'string',
//   tags: ['女', '大四'],
//   createTime: new Date(),
// };


onMounted(async () => {
    user.value = await getCurrentUser()
})

const formatDate = (date: string | number | Date | undefined): string => {
  if (!date) return ''
  const d = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date
  if (!d || Number.isNaN(d.getTime())) return ''
  return d.toLocaleString()
}

const toEdit = (editKey: string, editName: string, currentValue: string) => {
  router.push({
    path: '/user/edit',
    query: {
      editKey,
      currentValue,
      editName,
    },
  })
}
</script>