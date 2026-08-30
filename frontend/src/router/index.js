import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import UserList from '../views/UserList.vue'
import ClassGroupList from '../views/ClassGroupList.vue'
import GoalView from '../views/GoalView.vue'
import GoalStudentProgressView from '../views/GoalStudentProgressView.vue'
import GoalTemplateLibraryView from '../views/GoalTemplateLibraryView.vue'
import UnitTestView from '../views/UnitTestView.vue'
import LoginView from '../views/homework/LoginView.vue'
import LayoutView from '../views/homework/LayoutView.vue'
import AssignmentListView from '../views/homework/AssignmentListView.vue'
import AssignmentDetailView from '../views/homework/AssignmentDetailView.vue'
import DoAssignmentView from '../views/homework/DoAssignmentView.vue'
import PrintablePaper from '../views/homework/PrintablePaper.vue'
import QuestionListView from '../views/homework/QuestionListView.vue'
import QuestionEditView from '../views/homework/QuestionEditView.vue'
import GradingView from '../views/homework/GradingView.vue'
import MistakeBookView from '../views/homework/MistakeBookView.vue'
import MistakeDetailView from '../views/homework/MistakeDetailView.vue'
import { useAuthStore } from '../store/auth'

const showUnitTest = import.meta.env.VITE_SHOW_UNIT_TEST !== 'false'

const routes = [
  { path: '/', component: Home },
  { path: '/users', component: UserList },
  { path: '/class-groups', component: ClassGroupList },
  { path: '/math-goals', redirect: '/goals' },
  { path: '/goals', component: GoalView },
  { path: '/goals/student-progress', component: GoalStudentProgressView, meta: { requiresAuth: true } },
  // 目标模板库：浏览所有老师共享的可复制目标（需登录才能看到作者信息与复制）
  { path: '/goals/templates', component: GoalTemplateLibraryView, meta: { requiresAuth: true } },
  ...(showUnitTest ? [{ path: '/unit-test', component: UnitTestView }] : []),
  { path: '/login', component: LoginView },
  // 打印/导出 PDF 专用页面：独立版式，不套 LayoutView 侧边栏
  { path: '/assignments/:id/print', component: PrintablePaper, meta: { requiresAuth: true } },
  {
    path: '/',
    component: LayoutView,
    meta: { requiresAuth: true },
    children: [
      { path: 'assignments', component: AssignmentListView },
      { path: 'assignments/:id', component: AssignmentDetailView },
      { path: 'assignments/:id/do', component: DoAssignmentView },
      { path: 'questions', component: QuestionListView },
      { path: 'questions/new', component: QuestionEditView },
      { path: 'questions/:id/edit', component: QuestionEditView },
      { path: 'grading', component: GradingView },
      // 错题本：学生视角。与其他作业页一样挂在 LayoutView 下共用侧边栏，
      // requiresAuth 由父路由的 meta 提供。
      // 刻意不加学生专用守卫：守卫只认 meta.requiresAuth + adminRoutes，
      // 教师访问这里会看到自己的空收藏本，无害，不值得为此新增守卫机制。
      { path: 'mistakes', component: MistakeBookView },
      { path: 'mistakes/:id', component: MistakeDetailView }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 需要管理员角色的路由
const adminRoutes = ['/users', '/class-groups']

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()

  // 需登录的路由
  if (to.meta.requiresAuth && !auth.isAuthenticated()) {
    return next('/login')
  }

  // 管理员专用路由
  if (adminRoutes.includes(to.path) && !auth.isAdmin()) {
    return next('/')
  }

  next()
})

export default router
