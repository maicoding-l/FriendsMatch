<template>
  <div class="layout" :class="{ 'layout-immersive': !showChrome }">
    <div v-if="showChrome" id="nav_bar">
      <van-nav-bar
        :title="title"
        left-text="返回"
        left-arrow
        @click-left="returnToPre"
        @click-right="toSearch"
      >
        <template #right>
          <van-icon name="search" size="18" />
        </template>
      </van-nav-bar>
    </div>
    <div id="content" :class="{ 'content-immersive': !showChrome }">
      <router-view />
    </div>
    <van-tabbar v-model="active" @change="onChange">
      <van-tabbar-item to="/" icon="compass-3-line" name="index">
        <template #icon>
            <i class="ri-compass-3-line"></i>
        </template>
        发现
      </van-tabbar-item>
      <van-tabbar-item to="/team" icon="group-line" name="team">
          <template #icon>
            <i class="ri-group-line"></i>
        </template>
        Communities
      </van-tabbar-item>
      <van-tabbar-item to="/graph" icon="mind-map" name="graph">
          <template #icon>
            <i class="ri-mind-map"></i>
        </template>
        图谱
      </van-tabbar-item>
      <van-tabbar-item to="/user" icon="user-smile-line" name="user">
          <template #icon>
            <i class="ri-user-smile-line"></i>
        </template>
        我的
      </van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import router, { routes } from '../router/index.ts'

const active = ref('index')
const title = ref('')
const route = useRoute()

const tabPages = ['/', '/team', '/graph', '/user']
const showChrome = computed(() => !tabPages.includes(route.path))

const onChange = () => {
}

const toSearch = () => {
  router.push({ path: '/search' })
}

const returnToPre = () => {
  router.back()
}

watch(
  () => route.path,
  (path) => {
    if (path.startsWith('/team')) {
      active.value = 'team'
    } else if (path.startsWith('/user')) {
      active.value = 'user'
    } else if (path.startsWith('/graph')) {
      active.value = 'graph'
    } else {
      active.value = 'index'
    }
  },
  { immediate: true }
)

/**
 * 根据路由切换标题
 */
router.beforeEach((to) => {
  const toPath = to.path
  const matchedRoute = routes.find((item) => toPath === item.path)
  title.value = matchedRoute?.name?.toString() ?? ''
})
</script>

<style scoped>
@import url('https://cdn.jsdelivr.net/npm/remixicon@3.5.0/fonts/remixicon.css');

.layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  --tabbar-height: 50px;
}

#nav_bar {
  flex: 0 0 auto;
}

#content {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 50px;
}

.layout-immersive #content {
  padding: 0;
}

.content-immersive {
  min-height: 100vh;
}

/* BasicLayout Floating Tabbar Styles */
:deep(.van-tabbar) {
  background: rgba(20, 20, 30, 0.65); /* More transparent */
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid rgba(255, 255, 255, 0.08);

  /* Optional: Floating style (uncomment if you want it floating above bottom) */
  /*
  width: 92%;
  left: 4%;
  bottom: 20px;
  border-radius: 24px;
  overflow: hidden;
  */
}

:deep(.van-tabbar-item) {
  background: transparent;
  color: #8e9aaf;
}

:deep(.van-tabbar-item--active) {
  background: transparent;
  color: #a29bfe; /* Matches theme accent */
}

:deep(.van-hairline--top-bottom:after) {
  border-width: 0;
}
</style>
