import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 路由元信息类型声明：meta.roles 是后台路由的角色门槛。
declare module 'vue-router' {
  interface RouteMeta {
    roles?: string[]
  }
}

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
          path: 'submissions',
          name: 'submissions',
          component: () => import('@/views/SubmissionsView.vue'),
        },
        {
          path: 'database',
          name: 'database',
          component: () => import('@/views/DatabaseQuestionsView.vue'),
        },
        {
          path: 'database/:id',
          name: 'database-detail',
          component: () => import('@/views/DatabaseQuestionDetailView.vue'),
        },
        {
          path: 'database-submissions',
          redirect: '/submissions',
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
        {
          path: 'admin',
          name: 'admin',
          redirect: '/admin/questions',
        },
        {
          path: 'admin/questions',
          name: 'admin-questions',
          component: () => import('@/views/admin/AdminQuestionsView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] },
        },
        {
          path: 'admin/questions/new',
          name: 'admin-question-new',
          component: () => import('@/views/admin/QuestionEditView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] },
        },
        {
          path: 'admin/questions/:id/edit',
          name: 'admin-question-edit',
          component: () => import('@/views/admin/QuestionEditView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] },
        },
        {
          path: 'admin/db-questions',
          name: 'admin-db-questions',
          component: () => import('@/views/admin/AdminDbQuestionsView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] },
        },
        {
          path: 'admin/db-questions/new',
          name: 'admin-db-question-new',
          component: () => import('@/views/admin/DbQuestionEditView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] },
        },
        {
          path: 'admin/db-questions/:id/edit',
          name: 'admin-db-question-edit',
          component: () => import('@/views/admin/DbQuestionEditView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] },
        },
        {
          path: 'admin/objective-questions',
          name: 'admin-objective-questions',
          component: () => import('@/views/admin/AdminObjectiveQuestionsView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] },
        },
        {
          path: 'admin/objective-questions/new',
          name: 'admin-objective-question-new',
          component: () => import('@/views/admin/ObjectiveQuestionEditView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] },
        },
        {
          path: 'admin/objective-questions/:id/edit',
          name: 'admin-objective-question-edit',
          component: () => import('@/views/admin/ObjectiveQuestionEditView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] },
        },
        {
          path: 'admin/users',
          name: 'admin-users',
          component: () => import('@/views/admin/AdminUsersView.vue'),
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'admin/submissions',
          name: 'admin-submissions',
          component: () => import('@/views/admin/AdminSubmissionsView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] },
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

// 已登录用户访问登录/注册页时跳回首页；带 roles 的页面做角色校验
router.beforeEach((to) => {
  const userStore = useUserStore()
  if ((to.name === 'login' || to.name === 'register') && userStore.isLoggedIn) {
    return { name: 'home' }
  }
  const roles = to.meta.roles
  if (roles && roles.length > 0) {
    if (!userStore.isLoggedIn) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    // 角色不符时按不存在处理，不暴露页面是否存在
    if (!roles.some((role) => userStore.hasRole(role))) {
      return { name: 'not-found' }
    }
  }
  return true
})

export default router
