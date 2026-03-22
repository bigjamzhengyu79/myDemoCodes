<template>
  <div class="layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <svg width="24" height="24" viewBox="0 0 32 32" fill="none">
          <rect width="32" height="32" rx="7" fill="#2563eb"/>
          <text x="16" y="22" text-anchor="middle" fill="white" font-size="16" font-weight="600" font-family="serif">∑</text>
        </svg>
        <span style="font-weight:600;font-size:13px">数学习题系统</span>
      </div>

      <div class="sidebar-user">
        <div class="user-avatar">{{ auth.user?.realName?.[0] || '?' }}</div>
        <div>
          <div style="font-size:13px;font-weight:500">{{ auth.user?.realName }}</div>
          <div style="font-size:11px;color:var(--c-text3)">
            {{ auth.isTeacher() ? '教师' : auth.user?.className }}
          </div>
        </div>
      </div>

      <nav style="flex:1;padding:8px 0">
        <router-link class="nav-item" to="/assignments" active-class="active">
          <svg class="nav-icon" viewBox="0 0 16 16" fill="currentColor">
            <path d="M2 3h12v2H2zm0 4h12v2H2zm0 4h8v2H2z"/>
          </svg>
          {{ auth.isTeacher() ? '作业管理' : '我的作业' }}
        </router-link>
        <template v-if="auth.isTeacher()">
          <router-link class="nav-item" to="/questions" active-class="active">
            <svg class="nav-icon" viewBox="0 0 16 16" fill="currentColor">
              <path d="M8 1a7 7 0 100 14A7 7 0 008 1zm.75 10.5h-1.5v-1.5h1.5v1.5zm1.27-5.13c-.2.38-.55.7-1.07.96L8.5 7.9V9h-1V7.3l.38-.18c.38-.18.65-.38.81-.6.16-.22.24-.46.24-.72a1 1 0 00-.3-.74 1.1 1.1 0 00-.79-.28 1.1 1.1 0 00-.82.32c-.2.22-.3.5-.3.86H5.75c0-.58.18-1.07.54-1.47.36-.4.87-.6 1.5-.6.6 0 1.08.17 1.45.5.37.33.56.77.56 1.3 0 .36-.1.69-.28 1.01z"/>
            </svg>
            题库管理
          </router-link>
          <router-link class="nav-item" to="/grading" active-class="active">
            <svg class="nav-icon" viewBox="0 0 16 16" fill="currentColor">
              <path d="M13.5 2h-11C1.67 2 1 2.67 1 3.5v9c0 .83.67 1.5 1.5 1.5h11c.83 0 1.5-.67 1.5-1.5v-9c0-.83-.67-1.5-1.5-1.5zm-6 9l-3-3 1.06-1.06L7.5 8.88l3.94-3.94L12.5 6l-5 5z"/>
            </svg>
            批改作业
          </router-link>
        </template>
      </nav>

      <div class="sidebar-footer">
        <button class="nav-item" style="width:100%;border:none;background:none;cursor:pointer" @click="handleLogout">
          <svg class="nav-icon" viewBox="0 0 16 16" fill="currentColor">
            <path d="M6 2H3a1 1 0 00-1 1v10a1 1 0 001 1h3v-1.5H3.5v-9H6V2zm7.7 6.7l-3 3-.7-.7 2.1-2.1H6V7.5h6.1L10 5.4l.7-.7 3 3a.5.5 0 010 .7z"/>
          </svg>
          退出登录
        </button>
      </div>
    </aside>

    <!-- Main -->
    <main class="main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/store/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.sidebar-header { display: flex; align-items: center; gap: 10px; padding: 16px; border-bottom: 1px solid var(--c-border); }
.sidebar-user { display: flex; align-items: center; gap: 10px; padding: 12px 16px; border-bottom: 1px solid var(--c-border); }
.user-avatar { width: 32px; height: 32px; border-radius: 50%; background: var(--c-primary-bg); color: var(--c-primary); display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 13px; flex-shrink: 0; }
.sidebar-footer { border-top: 1px solid var(--c-border); padding: 8px 0; }
</style>
