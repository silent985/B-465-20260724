import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '幸运大转盘' }
  },
  {
    path: '/records',
    name: 'Records',
    component: () => import('@/views/Records.vue'),
    meta: { title: '抽奖记录' }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/admin/Dashboard.vue'),
    meta: { title: '管理后台', requiresAdmin: true },
    children: [
      {
        path: '',
        redirect: '/admin/prizes'
      },
      {
        path: 'prizes',
        name: 'PrizeManager',
        component: () => import('@/views/admin/PrizeManager.vue'),
        meta: { title: '奖品管理' }
      },
      {
        path: 'users',
        name: 'UserManager',
        component: () => import('@/views/admin/UserManager.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'records',
        name: 'RecordManager',
        component: () => import('@/views/admin/RecordManager.vue'),
        meta: { title: '记录管理' }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 幸运大转盘` : '幸运大转盘'
  next()
})

export default router
