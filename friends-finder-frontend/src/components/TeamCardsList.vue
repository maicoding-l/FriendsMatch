<template>
  <van-skeleton title avatar :row="3" :loading="loading" />
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
        <p>发起人：{{ team.createUser?.username }}</p>
      </div>
      <div>
        <p>小组人数：{{ team.hasJoinNum }}/ {{ team.maxNum }}</p>
      </div>
      <div v-if="team.createTime">
        <p>创建时间：{{ team.createTime }}</p>
      </div>
      <div v-if="team.expireTime">
        <p>过期时间：{{ team.expireTime }}</p>
      </div>
    </template>
    <template #footer>
      <van-button
        size="mini"
        v-if="!team.membersList?.includes(currentUser?.id)"
        @click="joinTeam(team.id)"
        >加入小组</van-button
      >
      <van-button
        size="mini"
        v-if="team.userId === currentUser?.id"
        @click="updateTeam(team.id)"
        >更新小组</van-button
      >
      <van-button
        size="mini"
        v-if="
          team.membersList?.includes(currentUser?.id) &&
          team.userId !== currentUser?.id
        "
        @click="quitTeam(team.id)"
        >退出小组</van-button
      >
      <van-button
        size="mini"
        v-if="team.userId === currentUser?.id"
        @click="deleteTeam(team.id)"
        >解散小组</van-button
      >
    </template>
  </van-card>

  <van-dialog
    v-model:show="showPasswordDialog"
    title="加入加密小组"
    show-cancel-button
    confirm-button-text="确认"
    cancel-button-text="取消"
    @confirm="handlePasswordConfirm"
  >
    <van-field
      v-model="passwordInput"
      type="password"
      placeholder="请输入小组密码"
      clearable
    />
  </van-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showDialog } from 'vant'
import type { TeamType } from '../models/team'
import { teamStatusEnum } from '../constants/team'
import { getCurrentUser } from '../api/user'
import myAxios from '../request'

interface TeamCardListProps {
  teamsList?: TeamType[]
  loading: boolean
}

const router = useRouter()
const currentUser = ref()
const showPasswordDialog = ref(false)
const passwordInput = ref('')
const currentTeamId = ref<number | null>(null)

const props = withDefaults(defineProps<TeamCardListProps>(), {
  teamsList: () => [] as TeamType[],
  loading: true,
})

const emit = defineEmits<{
  refresh: []
}>()

const joinTeam = async (id: number) => {
  const team = props.teamsList?.find((t) => t.id === id)
  if (team?.status === 2) {
    currentTeamId.value = id
    passwordInput.value = ''
    showPasswordDialog.value = true
  } else {
    joinTeamWithPassword(id, '')
  }
}

const handlePasswordConfirm = () => {
  if (!passwordInput.value.trim()) {
    showToast('请输入小组密码')
    return
  }

  if (currentTeamId.value !== null) {
    joinTeamWithPassword(currentTeamId.value, passwordInput.value)
    currentTeamId.value = null
  }

  showPasswordDialog.value = false
}

const joinTeamWithPassword = async (id: number, password: string) => {
  const requestData: any = { teamId: id }
  if (password) {
    requestData.password = password
  }

  const res = await myAxios
    .post('team/join', requestData)
    .then((response: any) => {
      console.log('/team/join success', response)
      return response?.data
    })
    .catch((error: any) => {
      console.log('/team/join fail', error)
    })

  if (res?.code === 0) {
    showDialog({
      title: '操作成功',
      message: '成功加入小组！',
      confirmButtonText: '确定',
    }).then(() => {
      emit('refresh')
    })
  } else {
    showToast(res?.description || res?.msg || '加入失败')
  }
}

const updateTeam = (id: number) => {
  router.push({
    path: '/team/update',
    query: { id },
  })
}

const quitTeam = async (id: number) => {
  const res = await myAxios
    .post('/team/quit', { teamId: id })
    .then((response: any) => {
      console.log('/team/quit success', response)
      return response?.data
    })
    .catch((error: any) => {
      console.log('/team/quit fail', error)
    })

  if (res?.code === 0) {
    showDialog({
      title: '操作成功',
      message: '成功退出小组！',
      confirmButtonText: '确定',
    }).then(() => {
      emit('refresh')
    })
  } else {
    showToast(res?.description || res?.msg || '退出失败')
  }
}

const deleteTeam = async (id: number) => {
  try {
    const response = await myAxios.post('/team/delete', null, {
      params: { id },
    })
    const data = response?.data
    if (data?.code === 0) {
      await showDialog({
        title: '操作成功',
        message: '成功解散小组！',
        confirmButtonText: '确定',
      })
      emit('refresh')
    } else {
      showToast(data?.description || data?.message || '解散失败')
    }
  } catch (error: any) {
    console.log('/team/delete fail', error)
    showToast('解散失败')
  }
}

onMounted(async () => {
  currentUser.value = await getCurrentUser()
})
</script>

<style scoped>
.van-tag--danger.van-tag--plain {
  color: #002fff;
}
</style>
