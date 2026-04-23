import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import UserList from '../views/UserList.vue'
import GoalView from '../views/GoalView.vue'
import UnitTestView from '../views/UnitTestView.vue'
import LoginView from '../views/homework/LoginView.vue'
import LayoutView from '../views/homework/LayoutView.vue'
import AssignmentListView from '../views/homework/AssignmentListView.vue'
import AssignmentDetailView from '../views/homework/AssignmentDetailView.vue'
import DoAssignmentView from '../views/homework/DoAssignmentView.vue'
import QuestionListView from '../views/homework/QuestionListView.vue'
import QuestionEditView from '../views/homework/QuestionEditView.vue'
import GradingView from '../views/homework/GradingView.vue'
import { useAuthStore } from '../store/auth'

const routes = [
  { path: '/', component: Home },
  { path: '/users', component: UserList },
  { path: '/math-goals', redirect: '/goals' },
  { path: '/goals', component: GoalView },
  { path: '/unit-test', component: UnitTestView },
  { path: '/login', component: LoginView },
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
      { path: 'grading', component: GradingView }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated()) {
    next('/login')
  } else {
    next()
  }
})

export default router
