import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/layout/AdminLayout.vue'
import PublicLayout from '@/layout/PublicLayout.vue'
import UserLayout from '@/layout/UserLayout.vue'
import { evaluateRouteGuard } from '@/router/guards'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/public/home',
    },
    {
      path: '/login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录后台' },
    },
    {
      path: '/register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { title: '用户注册' },
    },
    {
      path: '/public',
      component: PublicLayout,
      children: [
        {
          path: 'home',
          component: () => import('@/views/public/PublicHomeView.vue'),
          meta: { title: '平台首页' },
        },
        {
          path: 'activities',
          component: () => import('@/views/public/PublicActivitiesView.vue'),
          meta: { title: '活动展示' },
        },
        {
          path: 'activities/:id',
          component: () => import('@/views/public/PublicActivityDetailView.vue'),
          meta: { title: '活动详情' },
        },
      ],
    },
    {
      path: '/admin',
      component: AdminLayout,
      children: [
        {
          path: 'activities',
          component: () => import('@/views/admin/ActivityListView.vue'),
          meta: { title: '活动管理' },
        },
        {
          path: 'activities/create',
          component: () => import('@/views/admin/ActivityEditorView.vue'),
          meta: { title: '新建活动' },
        },
        {
          path: 'activities/:id',
          component: () => import('@/views/admin/ActivityDetailView.vue'),
          meta: { title: '活动详情' },
        },
        {
          path: 'activities/:id/edit',
          component: () => import('@/views/admin/ActivityEditorView.vue'),
          meta: { title: '编辑活动' },
        },
      ],
    },
    {
      path: '/user',
      component: UserLayout,
      children: [
        {
          path: '',
          redirect: '/user/orders',
        },
        {
          path: 'orders',
          component: () => import('@/views/user/UserOrderListView.vue'),
          meta: { title: '我的订单' },
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  const appStore = useAppStore()
  const title = typeof to.meta.title === 'string' ? to.meta.title : 'Flash Sale Service'
  appStore.setPageTitle(title)

  const decision = evaluateRouteGuard({
    path: to.path,
    accessToken: authStore.accessToken,
    currentUser: authStore.currentUser,
  })

  if (!decision.allow && decision.redirectTo) {
    return decision.redirectTo
  }

  return true
})

export default router
