<template>
  <div class="layout">
    <div id="nav_bar">
      <van-nav-bar
        title="标题"
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
import router from '../router/index.ts'

const active = ref('index')
const onChange = () => {
  showToast(active.value)
}
const toSearch = () => {
  router.push({ path: '/search' })
}
const returnToPre = () => {
  router.back() // 或者 router.go(-1)
}
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
