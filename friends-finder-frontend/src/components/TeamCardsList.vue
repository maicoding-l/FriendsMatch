<template>
  <van-card
    v-for="team in props.teamsList"
    :key="team.id"
    :desc="team.description"
    :title="team.teamName"
    thumb="https://i.pinimg.com/736x/70/b7/9d/70b79d93ab3f362738dd48984ea01564.jpg"
  >
    <template #tag>
      <van-tag plain type="danger" style="margin-right: 8px; margin-top: 8px">
        {{ teamStatusEnum[team.status] }}
      </van-tag>
    </template>
    <template #bottom>
      <div>
        <p>队长：{{ team.createUser?.username }}</p>
      </div>
      <div>
        <p>队伍人数：{{ team.hasJoinNum }}/ {{ team.maxNum }}</p>
      </div>
      <div v-if="team.createTime">
        <p>创建时间：{{ team.createTime }}</p>
      </div>
      <div v-if="team.expireTime">
        <p>过期时间: {{ team.expireTime }}</p>
      </div>
    </template>
    <template #footer>
      <van-button
        size="mini"
        v-if="!team.membersList?.includes(currentUser?.id)"
        @click="joinTeam(team.id)"
        >加入队伍</van-button
      >
      <van-button
        size="mini"
        v-if="team.userId === currentUser?.id"
        @click="updateTeam(team.id)"
        >更新队伍</van-button
      >
      <van-button
        size="mini"
        v-if="
          team.membersList?.includes(currentUser?.id) &&
          team.userId !== currentUser?.id
        "
        @click="quitTeam(team.id)"
        >退出队伍</van-button
      >
      <van-button
        size="mini"
        v-if="team.userId === currentUser?.id"
        @click="deleteTeam(team.id)"
        >解散队伍</van-button
      >
    </template>
  </van-card>
</template>

<script setup lang="ts">
import type { TeamType } from '../models/team'
import { teamStatusEnum } from '../constants/team'
import { getCurrentUser } from '../api/user'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import myAxios from '../request'
import { showToast } from 'vant'
interface TeamCardListProps {
  teamsList?: TeamType[]
}

const router = useRouter()
const currentUser = ref()
const props = withDefaults(defineProps<TeamCardListProps>(), {
  teamsList: () => [] as TeamType[],
})

// 定义发射事件
const emit = defineEmits<{
  refresh: []
}>()

//加入队伍
const joinTeam = async (id: number) => {
  const res = await myAxios
    .post('team/join', {
      teamId: id,
    })
    .then((response: any) => {
      console.log('/team/join success', response)
      return response?.data
    })
    .catch((error: any) => {
      console.log('/team/join fail', error)
    })

  if (res.data === true) {
    showToast('加入队伍成功')
    emit('refresh')
  } else {
    showToast(res.msg)
  }
}

//更新队伍
const updateTeam = (id: number) => {
  router.push({
    path: '/team/update',
    query: {
      id: id,
    },
  })
}

//退出队伍
const quitTeam = async (id: number) => {
  const res = await myAxios
    .post('/team/quit', {
      teamId: id,
    })
    .then((response: any) => {
      console.log('/team/quit success', response)
      return response?.data
    })
    .catch((error: any) => {
      console.log('/team/quit fail', error)
    })

  if (res.data === true) {
    showToast('退出队伍成功')
    emit('refresh')
  } else {
    showToast(res.msg)
  }
}

//解散队伍
const deleteTeam = async (id: number) => {
  const res = await myAxios
    .post('/team/delete', {
      teamId: id,
    })
    .then((response: any) => {
      console.log('/team/delete success', response)
      return response?.data
    })
    .catch((error: any) => {
      console.log('/team/delete fail', error)
    })

  if (res.data === true) {
    showToast('解散队伍成功')
    emit('refresh')
  } else {
    showToast(res.msg)
  }
}

onMounted(async () => {
  currentUser.value = await getCurrentUser()
})
</script>

<style scoped>
/* 标签颜色*/
.van-tag--danger.van-tag--plain {
  color: #002fff;
}
</style>
