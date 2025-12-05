<template>
  <div class="search-result-page">
    匹配模式：<van-switch v-model="checked" />
    <UserCardsList :users-list="usersList" :loading="loading"> </UserCardsList>
  </div>
  <van-empty v-if="usersList?.length === 0" description="没有符合的结果" />
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { ref, onMounted, watch } from 'vue'
import type { UserType } from '../models/user'
import myAxios from '../request'
import { showToast } from 'vant'
import UserCardsList from '../components/UserCardsList.vue'
const route = useRoute()
const { tags } = route.query
const usersList = ref<UserType[]>()
//是否开启匹配模式
const checked = ref(false)
const loading = ref(true)
const MATCH_COUNT = 5

const fetchUsers = async () => {
  loading.value = true
  const endpoint = checked.value ? 'user/match' : 'user/recommend'

  const requestPromise = checked.value
    ? myAxios.get(endpoint, {
        params: {
          num: MATCH_COUNT,
        },
      })
    : myAxios.get(endpoint, {
        params: {
          pageNo: 1,
          pageSize: 10,
        },
      })

  const usersListData = await requestPromise
    .then((response: any) => {
      console.log(`${endpoint} success`, response)
      showToast('请求成功')
      const data = response.data?.data
      return data?.records ?? data
    })
    .catch((error: any) => {
      console.log(`${endpoint} fail`, error)
      showToast('请求失败')
    })
  if (usersListData) {
    usersListData.forEach((user: { tags: string }) => {
      if (user.tags) {
        user.tags = JSON.parse(user.tags)
      }
    })
  }
  usersList.value = usersListData
  loading.value = false
}

onMounted(async () => {
  await fetchUsers()
})

watch(checked, async () => {
  await fetchUsers()
})
</script>

<style scoped></style>
