<template>
  <div class="team-page">
    <van-search
      v-model="value"
      placeholder="请输入搜索关键词"
      @search="onSearch"
    />
    <van-tabs v-model:active="active" @change="onTabChange">
      <van-tab title="公开队伍"></van-tab>
      <van-tab title="加密队伍"></van-tab>
    </van-tabs>
    <teamCardList
      :teams-list="teamsList"
      :loading="loading"
      @refresh="onRefresh"
    ></teamCardList>
    <van-empty v-if="teamsList?.length === 0" description="没有符合的结果" />
    <van-button type="primary" class="add-btn" @click="toTeamAddPage">
      <span style="line-height: 1">+</span>
    </van-button>
  </div>
</template>

<script lang="ts" setup>
import { useRouter } from 'vue-router'
import teamCardList from '../components/TeamCardsList.vue'
import type { TeamType } from '../models/team'
import myAxios from '../request'
import { showToast } from 'vant'
import { ref, onMounted } from 'vue'
const router = useRouter()
const teamsList = ref<TeamType[]>()
const value = ref('')
const loading = ref(true)
const active = ref(0)
const toTeamAddPage = () => {
  router.push({
    path: '/team/add',
  })
}

/**
 * 搜索队伍
 */
const listTeam = async (val: string) => {
  loading.value = true
  const params: any = { searchText: val }

  // 如果当前选择的是加密队伍标签，添加 status 参数
  if (active.value === 1) {
    params.status = 2
  }

  const teamsListData = await myAxios
    .get('/team/list', { params })
    .then((response: any) => {
      console.log('/team/list success', response)
      showToast('请求成功')
      return response?.data?.data
    })
    .catch((error: any) => {
      console.log('/team/list error', error)
      showToast('请求失败')
    })
  loading.value = false
  teamsList.value = teamsListData
}

/**
 * 搜索提交
 */
const onSearch = (val: string) => {
  listTeam(val)
}

/**
 * 标签切换
 */
const onTabChange = () => {
  listTeam(value.value)
}

/**
 * 刷新列表
 */
const onRefresh = () => {
  listTeam(value.value)
}

onMounted(async () => {
  listTeam('')
})
</script>

<style scoped>
.team-page {
}
</style>
