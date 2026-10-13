import { createRouter, createWebHistory } from 'vue-router';
import { useUserStore } from '@/stores/user';

// 路由元信息类型声明：meta.roles 是后台路由的角色门槛，meta.title 是页面标题。
declare module 'vue-router' {
  interface RouteMeta {
    roles?: string[];
    title?: string;
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 前台考生端主站路由（使用 DefaultLayout）
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
          path: 'questions/:id/submit',
          name: 'question-submit',
          component: () => import('@/views/QuestionSubmitView.vue'),
        },
        {
          path: 'questions/:id/result/:submissionId',
          name: 'question-result',
          component: () => import('@/views/QuestionResultView.vue'),
        },
        {
          path: 'submissions',
          name: 'submissions',
          component: () => import('@/views/SubmissionsView.vue'),
        },
        {
          path: 'submissions/:submissionId',
          name: 'submission-detail',
          component: () => import('@/views/QuestionResultView.vue'),
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
          path: 'database/:id/submit',
          name: 'database-submit',
          component: () => import('@/views/DatabaseQuestionSubmitView.vue'),
        },
        {
          path: 'database/:id/result/:submissionId',
          name: 'database-result',
          component: () => import('@/views/DatabaseQuestionResultView.vue'),
        },
        {
          path: 'database-submissions',
          redirect: '/submissions',
        },
        {
          path: 'database-submissions/:submissionId',
          name: 'database-submission-detail',
          component: () => import('@/views/DatabaseQuestionResultView.vue'),
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
          path: 'exams',
          name: 'exams',
          component: () => import('@/views/ExamListView.vue'),
        },
        {
          path: 'exams/:id',
          name: 'exam-session',
          component: () => import('@/views/ExamSessionView.vue'),
        },
        {
          path: 'exams/:id/result',
          name: 'exam-result',
          component: () => import('@/views/ExamResultView.vue'),
        },
      ],
    },
    // 独立后台管理端路由（使用独立侧边栏 AdminLayout）
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/admin/questions',
      children: [
        {
          path: 'questions',
          name: 'admin-questions',
          component: () => import('@/views/admin/AdminQuestionsView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'], title: '编程题管理' },
        },
        {
          path: 'questions/new',
          name: 'admin-question-new',
          component: () => import('@/views/admin/QuestionEditView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'], title: '新建编程题' },
        },
        {
          path: 'questions/:id/edit',
          name: 'admin-question-edit',
          component: () => import('@/views/admin/QuestionEditView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'], title: '编辑编程题' },
        },
        {
          path: 'db-questions',
          name: 'admin-db-questions',
          component: () => import('@/views/admin/AdminDbQuestionsView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'], title: '数据库题管理' },
        },
        {
          path: 'db-questions/new',
          name: 'admin-db-question-new',
          component: () => import('@/views/admin/DbQuestionEditView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'], title: '新建数据库题' },
        },
        {
          path: 'db-questions/:id/edit',
          name: 'admin-db-question-edit',
          component: () => import('@/views/admin/DbQuestionEditView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'], title: '编辑数据库题' },
        },
        {
          path: 'objective-questions',
          name: 'admin-objective-questions',
          component: () => import('@/views/admin/AdminObjectiveQuestionsView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'], title: '客观题管理' },
        },
        {
          path: 'objective-questions/new',
          name: 'admin-objective-question-new',
          component: () => import('@/views/admin/ObjectiveQuestionEditView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'], title: '新建客观题' },
        },
        {
          path: 'objective-questions/:id/edit',
          name: 'admin-objective-question-edit',
          component: () => import('@/views/admin/ObjectiveQuestionEditView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'], title: '编辑客观题' },
        },
        {
          path: 'exams',
          name: 'admin-exams',
          component: () => import('@/views/admin/AdminExamsView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'], title: '考试管理' },
        },
        {
          path: 'exams/new',
          name: 'admin-exam-new',
          component: () => import('@/views/admin/ExamEditView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'], title: '新建试卷' },
        },
        {
          path: 'exams/:id/edit',
          name: 'admin-exam-edit',
          component: () => import('@/views/admin/ExamEditView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'], title: '编辑试卷' },
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/admin/AdminUsersView.vue'),
          meta: { roles: ['ADMIN'], title: '用户管理' },
        },
        {
          path: 'submissions',
          name: 'admin-submissions',
          component: () => import('@/views/admin/AdminSubmissionsView.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'], title: '提交监控' },
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
});

// 已登录用户访问登录/注册页时跳回首页；带 roles 的页面做角色校验
router.beforeEach((to) => {
  const userStore = useUserStore();
  if ((to.name === 'login' || to.name === 'register') && userStore.isLoggedIn) {
    return { name: 'home' };
  }
  const roles = to.meta.roles;
  if (roles && roles.length > 0) {
    if (!userStore.isLoggedIn) {
      return { name: 'login', query: { redirect: to.fullPath } };
    }
    // 角色不符时按不存在处理，不暴露页面是否存在
    if (!roles.some((role) => userStore.hasRole(role))) {
      return { name: 'not-found' };
    }
  }
  return true;
});

export default router;
