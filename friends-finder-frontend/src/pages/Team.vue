<template>
  <div class="team-page">
    <van-search
      v-model="value"
      placeholder="请输入搜索关键词"
      @search="onSearch"
    />
    <van-button type="primary" @click="toTeamAddPage">新建队伍</van-button>
    <teamCardList :teams-list="teamsList" @refresh="onRefresh"></teamCardList>
    <van-empty v-if="teamsList?.length === 0" description="没有符合的结果" />
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
const toTeamAddPage = () => {
  router.push({
    path: '/team/add',
  })
}

/**
 * 搜索队伍
 */
const listTeam = async (val: string) => {
  const teamsListData = await myAxios
    .get('/team/list', { params: { searchText: val } })
    .then((response: any) => {
      console.log('/team/list success', response)
      showToast('请求成功')
      return response?.data?.data
    })
    .catch((error: any) => {
      console.log('/team/list error', error)
      showToast('请求失败')
    })
  teamsList.value = teamsListData
}

/**
 * 搜索提交
 */
const onSearch = (val: string) => {
  listTeam(val)
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

<style scoped></style>
