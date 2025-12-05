<template>
  <div class="layout">
    <div id="nav_bar">
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
    <div id="content">
      <router-view />
    </div>
    <div id="tab_bar">
      <van-tabbar v-model="active" @change="onChange">
        <van-tabbar-item to="/" icon="home-o" name="index">
          主页
        </van-tabbar-item>
        <van-tabbar-item to="/team" icon="friends-o" name="team">
          队伍
        </van-tabbar-item>
        <van-tabbar-item to="/user" icon="setting-o" name="user">
          个人
        </van-tabbar-item>
      </van-tabbar>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import router, { routes } from '../router/index.ts'
import { showToast } from 'vant'

const active = ref('index')
const title = ref('')

const onChange = () => {
  showToast(active.value)
}

const toSearch = () => {
  router.push({ path: '/search' })
}

const returnToPre = () => {
  router.back()
}

/**
 * 根据路由切换标题
 */
router.beforeEach((to) => {
  const toPath = to.path
  const route = routes.find((route) => {
    return toPath === route.path
  })
  title.value = route?.name ?? ''
})
</script>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  --tabbar-height: 50px; /* Vant 默认 50px，可按需要调整 */
}

#nav_bar,
#tab_bar {
  flex: 0 0 auto;
}

#content {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 50px;
}
</style>
