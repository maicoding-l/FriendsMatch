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
        background="rgba(255,255,255,0.02)"
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
            <van-button size="mini" color="#6c5ce7" plain @click="doJoinTeam(group)">
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
import { showToast } from 'vant'
import type { TeamType } from '../models/team'
import myAxios from '../request'

const router = useRouter()
const teamsList = ref<TeamType[]>([])
const value = ref('')
const loading = ref(true)
const active = ref(0) // 0: public, 1: private (encryption)

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

const doJoinTeam = (team: TeamType) => {
    router.push({
      path: '/user/join',
      query: { teamId: team.id },
    })
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
  listTeam('')
})
</script>

<style scoped>
@import url('https://cdn.jsdelivr.net/npm/remixicon@3.5.0/fonts/remixicon.css');
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;700&display=swap');

:global(:root) {
  --bg-color: transparent;
  --card-bg: rgba(24, 24, 35, 0.6);
  --glass-stroke: rgba(255, 255, 255, 0.08);
  --text-main: #ffffff;
  --text-sub: #9fa6b2;
  --accent: #a29bfe;
}

.nexus-page {
  position: relative;
  min-height: 100vh;
  background: var(--bg-color);
  color: var(--text-main);
  overflow-y: auto;
  font-family: 'Space Grotesk', 'SF Pro Display', system-ui, -apple-system, sans-serif;
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
  color: var(--text-sub);
  margin: 0 0 4px;
}

.title-row {
  font-size: 24px;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(120deg, #fff, #a29bfe);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.title-row .dot {
  color: #6c5ce7;
}

.subtitle {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-sub);
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
  color: var(--text-sub);
  background: rgba(255,255,255,0.05);
  cursor: pointer;
  transition: all 0.2s;
}

.tab-pill.active {
  background: #6c5ce7;
  color: #fff;
}

.glass-card {
  background: var(--card-bg);
  border: 1px solid var(--glass-stroke);
  border-radius: 16px;
  backdrop-filter: blur(10px);
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
  color: #fff;
}

.community-desc {
  font-size: 12px;
  color: var(--text-sub);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.community-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: var(--text-sub);
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
  background: #6c5ce7;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  box-shadow: 0 4px 12px rgba(108, 92, 231, 0.4);
  z-index: 10;
  cursor: pointer;
}

.empty-state {
  margin-top: 40px;
  opacity: 0.6;
}
</style>
