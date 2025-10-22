import { createWebHistory, createRouter } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

import IndexPage from '../pages/Index.vue'
import TeamPage from '../pages/Team.vue'
import UserPage from '../pages/User.vue'
import SearchPage from '../pages/Search.vue'
import UserEditPage from '../pages/UserEdit.vue'
import SearchResultPage from '../pages/SearchResultPage.vue'
import UserLoginPage from '../pages/UserLogin.vue'

const routes: Array<RouteRecordRaw> = [
  { path: '/', name: 'IndexPage', component: IndexPage },
  { path: '/team', name: 'TeamPage', component: TeamPage },
  { path: '/user', name: 'UserPage', component: UserPage },
  { path: '/search', name: 'SearchPage', component: SearchPage },
  { path: '/user/edit', name: 'UserEditPage', component: UserEditPage },
  { path: '/user/list', name: 'SearchResultPage', component: SearchResultPage },
  { path: '/login', name: 'UserLoginPage', component: UserLoginPage },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
