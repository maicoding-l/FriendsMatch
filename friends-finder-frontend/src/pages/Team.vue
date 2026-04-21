<template>
  <div class="team-page nexus-page">
    <div class="app-shell">
      <div class="page-title">
        <p class="eyebrow">Communities</p>
        <div class="title-row">
          <span>兴趣小组</span>
          <span class="dot">•</span>
          <span class="subtitle">找到同好，加入讨论</span>
        </div>
      </div>

      <van-search
        v-model="value"
        placeholder="搜索兴趣关键字"
        shape="round"
        @search="onSearch"
      />

      <div class="custom-tabs">
        <div 
          class="tab-pill" 
          :class="{ active: active === 0 }"
          @click="onTabChange(0)"
        >
          公开小组
        </div>
        <div 
          class="tab-pill" 
          :class="{ active: active === 1 }"
          @click="onTabChange(1)"
        >
          加密小组
        </div>
      </div>

      <div class="communities">
        <template v-if="loading">
          <van-skeleton v-for="n in 3" :key="n" title avatar :row="3" class="mt-4" />
        </template>
        <template v-else>
          <div v-if="teamsList?.length === 0" class="empty-state">
            <van-empty description="暂无符合的小组" />
          </div>
          <div
            v-for="group in teamsList"
            :key="group.id"
            class="community-card glass-card"
            @click="goToTeamDetail(group)"
          >
            <img :src="coverFor(group)" class="community-cover" alt="" />
            <div class="community-body">
              <div class="community-name">{{ group.teamName }}</div>
              <div class="community-desc">{{ group.description }}</div>
              <div class="community-meta">
                <span><i class="ri-user-3-line"></i> {{ group.hasJoinNum }} / {{ group.maxNum }}</span>
                <span><i class="ri-timer-line"></i> {{ group.status === 0 ? '公开' : '加密' }}</span>
              </div>
            </div>
            <van-button
              v-if="isUserJoined(group)"
              size="mini"
              disabled
              plain
              @click.stop
            >
              已加入
            </van-button>
            <van-button
              v-else-if="group.hasJoinNum >= group.maxNum"
              size="mini"
              disabled
              plain
              @click.stop
            >
              已满
            </van-button>
            <van-button
              v-else
              size="mini"
              :color="getComputedColor()"
              plain
              @click.stop="doJoinTeam(group)"
            >
              加入
            </van-button>
          </div>
        </template>
      </div>

      <div class="fab-btn" @click="toTeamAddPage">
        <van-icon name="plus" />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useRouter } from 'vue-router'
import { ref, onMounted } from 'vue'
import { showToast, showDialog } from 'vant'
import type { TeamType } from '../models/team'
import myAxios from '../request'
import { getCurrentUser } from '../api/user'
import type { UserType } from '../models/user'

const router = useRouter()
const teamsList = ref<TeamType[]>([])
const value = ref('')
const loading = ref(true)
const active = ref(0) // 0: public, 1: private (encryption)
const currentUser = ref<UserType | null>(null)

const coverFallbacks = [
  'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400&q=80',
  'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400&q=80',
  'https://images.unsplash.com/photo-1493225255756-d9584f8606e9?w=400&q=80',
]

const coverFor = (item: any) => {
  if (item.coverUrl) return item.coverUrl
  // random fallback based on id
  const idx = (item.id || 0) % coverFallbacks.length
  return coverFallbacks[idx]
}

const toTeamAddPage = () => {
  router.push({
    path: '/team/add',
  })
}

const goToTeamDetail = (team: TeamType) => {
  router.push({
    path: '/team/detail',
    query: { teamId: team.id }
  })
}

const isUserJoined = (team: TeamType) => {
  if (!currentUser.value || !team.membersList) {
    return false
  }
  return team.membersList.includes(currentUser.value.id)
}

const doJoinTeam = async (team: TeamType) => {
  // 检查是否已满
  if (team.hasJoinNum >= team.maxNum) {
    showToast('队伍已满')
    return
  }

  // 如果是加密小组，需要输入密码
  if (team.status === 2) {
    let password = ''
    showDialog({
      title: '加入加密小组',
      message: '<input type="password" id="team-password-input" placeholder="请输入小组密码" style="width: 100%; padding: 8px; margin-top: 12px; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box;" />',
      showCancelButton: true,
      confirmButtonText: '加入',
      cancelButtonText: '取消',
      allowHtml: true,
      beforeClose: async (action) => {
        if (action === 'confirm') {
          password = (document.getElementById('team-password-input') as HTMLInputElement)?.value || ''
          if (!password) {
            showToast('请输入密码')
            return false
          }
          await joinTeamRequest(team.id, password)
          return true
        }
        return true
      },
    }).catch(() => {
      // 用户取消
    })
  } else {
    // 公开小组直接加入
    await joinTeamRequest(team.id)
  }
}

const joinTeamRequest = async (teamId: number, password?: string) => {
  try {
    const postData: any = { teamId }
    if (password) {
      postData.password = password
    }

    const res = await myAxios.post('/team/join', postData)

    if (res?.data?.code === 0) {
      showToast('加入成功')
      // 刷新列表
      listTeam(value.value)
    } else {
      showToast(res?.data?.message || res?.data?.description || '加入失败')
    }
  } catch (error) {
    console.error('加入小组失败:', error)
    showToast('加入失败，请重试')
  }
}

const getComputedColor = () => {
  // 从 CSS 变量中获取主色
  const primaryColor = getComputedStyle(document.documentElement)
    .getPropertyValue('--color-primary-500')
    .trim()
  return primaryColor || '#5B61EA'
}


/**
 * 搜索小组
 */
const listTeam = async (val: string = '') => {
  loading.value = true
  const params: any = { searchText: val, pageNum: 1, pageSize: 10 }
  
  if (active.value === 1) {
    params.status = 2 // encryption
  } else {
    params.status = 0 // public
  }

  try {
    const res = await myAxios.get('/team/list', { params })
    if (res?.data?.code === 0) {
      teamsList.value = res.data.data
    } else {
      showToast('加载失败')
    }
  } catch (error) {
    console.error('/team/list error', error)
    showToast('请求失败')
  } finally {
    loading.value = false
  }
}

const onSearch = (val: string) => {
  listTeam(val)
}

const onTabChange = (index: number) => {
  active.value = index
  listTeam(value.value)
}

const onRefresh = () => {
  listTeam(value.value)
}

onMounted(async () => {
  // 获取当前用户信息
  currentUser.value = await getCurrentUser()
  // 加载小组列表
  listTeam('')
})
</script>

<style scoped>
@import url('https://cdn.jsdelivr.net/npm/remixicon@3.5.0/fonts/remixicon.css');

.nexus-page {
  position: relative;
  min-height: 100vh;
  background: transparent;
  color: var(--color-text-primary);
  overflow-y: auto;
  font-family: var(--font-family-base);
}

.app-shell {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding: 18px 16px 120px;
  box-sizing: border-box;
}

.page-title {
  margin-top: 6px;
  margin-bottom: 16px;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-text-secondary);
  margin: 0 0 4px;
}

.title-row {
  font-size: 24px;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(120deg, var(--color-text-primary), var(--color-primary-400));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.title-row .dot {
  color: var(--color-primary-500);
}

.subtitle {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  -webkit-text-fill-color: currentColor;
}

.custom-tabs {
  display: flex;
  gap: 12px;
  margin: 16px 0;
}

.tab-pill {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border-subtle);
  cursor: pointer;
  transition: all 0.2s;
}

.tab-pill.active {
  background: var(--color-primary-500);
  color: var(--color-text-inverse);
  border-color: var(--color-primary-500);
}

.glass-card {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: 16px;
  backdrop-filter: blur(var(--glass-blur));
}

.communities {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.community-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
}

.community-cover {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  object-fit: cover;
  flex-shrink: 0;
}

.community-body {
  flex: 1;
  min-width: 0;
}

.community-name {
  font-weight: 700;
  font-size: 15px;
  margin-bottom: 2px;
  color: var(--color-text-primary);
}

.community-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.community-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: var(--color-text-tertiary);
  opacity: 0.8;
}

.community-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.fab-btn {
  position: fixed;
  bottom: 80px;
  right: 24px;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: var(--color-primary-500);
  color: var(--color-text-inverse);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  box-shadow: 0 4px 12px var(--shadow-lg);
  z-index: 10;
  cursor: pointer;
}

.empty-state {
  margin-top: 40px;
  opacity: 0.6;
}

/* 覆盖 Vant Search 的样式以适配主题 */
:deep(.van-search) {
  background: var(--color-surface-base);
}

:deep(.van-search__content) {
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border-subtle);
}

:deep(.van-field__control) {
  color: var(--color-text-primary);
}

:deep(.van-field__control::placeholder) {
  color: var(--color-text-tertiary);
}
</style>
