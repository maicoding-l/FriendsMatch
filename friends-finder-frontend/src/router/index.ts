import { createWebHistory, createRouter } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

import IndexPage from '../pages/Index.vue'
import TeamPage from '../pages/Team.vue'
import UserPage from '../pages/User.vue'
import SearchPage from '../pages/Search.vue'
import UserEditPage from '../pages/UserEdit.vue'
import SearchResultPage from '../pages/SearchResultPage.vue'
import UserLoginPage from '../pages/UserLogin.vue'
import TeamAddPage from '../pages/TeamAddPage.vue'
import TeamUpdatePage from '../pages/TeamUpdatePage.vue'
import UserUpdatePage from '../pages/UserUpdatePage.vue'
import UserTeamCreatePage from '../pages/UserTeamCreatePage.vue'
import UserTeamJoinPage from '../pages/UserTeamJoinPage.vue'

const routes: Array<RouteRecordRaw> = [
  { path: '/', name: '首页', component: IndexPage },
  { path: '/team', name: '找队伍', component: TeamPage },
  { path: '/user', name: '个人信息', component: UserPage },
  { path: '/search', name: '找伙伴', component: SearchPage },
  { path: '/user/edit', name: '编辑信息', component: UserEditPage },
  { path: '/user/list', name: '用户列表', component: SearchResultPage },
  { path: '/login', name: '登录', component: UserLoginPage },
  { path: '/team/add', name: '新增队伍', component: TeamAddPage },
  { path: '/team/update', name: '修改队伍信息', component: TeamUpdatePage },
  { path: '/user/update', name: '更新信息', component: UserUpdatePage },
  { path: '/user/create', name: '创建队伍', component: UserTeamCreatePage },
  { path: '/user/join', name: '加入队伍', component: UserTeamJoinPage },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
export { routes }
