import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('@/views/HomeView.vue') },
        {
          path: 'questions',
          name: 'questions',
          component: () => import('@/views/QuestionsView.vue'),
        },
        {
          path: 'questions/:id',
          name: 'question-detail',
          component: () => import('@/views/QuestionDetailView.vue'),
        },
        {
          path: 'database',
          name: 'database',
          component: () => import('@/views/ComingSoonView.vue'),
          meta: { title: '数据库' },
        },
        {
          path: 'frontend',
          name: 'frontend',
          component: () => import('@/views/ComingSoonView.vue'),
          meta: { title: '前端' },
        },
        {
          path: 'objective',
          name: 'objective',
          component: () => import('@/views/ObjectiveQuestionsView.vue'),
        },
        {
          path: 'objective/:id',
          name: 'objective-detail',
          component: () => import('@/views/ObjectiveAnswerView.vue'),
        },
      ],
    },
    { path: '/login', name: 'login', component: () => import('@/views/AuthView.vue') },
    { path: '/register', name: 'register', component: () => import('@/views/AuthView.vue') },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

// 已登录用户访问登录/注册页时跳回首页
router.beforeEach((to) => {
  const userStore = useUserStore()
  if ((to.name === 'login' || to.name === 'register') && userStore.isLoggedIn) {
    return { name: 'home' }
  }
  return true
})

export default router
