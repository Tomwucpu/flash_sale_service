<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { LayoutDashboard, LogOut } from 'lucide-vue-next'
import AppBrand from '@/components/AppBrand.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const dashboardTarget = computed(() =>
  authStore.isAdminLike ? '/admin/activities' : '/public/home',
)

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="public-shell">
    <header class="public-shell__header">
      <AppBrand />
      <nav class="public-shell__nav">
        <RouterLink to="/public/home">首页</RouterLink>
        <RouterLink to="/public/activities">活动展示</RouterLink>
        <RouterLink to="/register">用户注册</RouterLink>
        <RouterLink to="/login">后台登录</RouterLink>
      </nav>
      <div class="public-shell__actions">
        <RouterLink v-if="authStore.isAuthenticated" class="public-shell__cta" :to="dashboardTarget">
          <LayoutDashboard :size="16" />
          返回工作台
        </RouterLink>
        <button
          v-if="authStore.isAuthenticated"
          data-testid="public-logout"
          class="flat-button flat-button--ghost public-shell__logout"
          type="button"
          @click="handleLogout"
        >
          <LogOut :size="16" />
          退出
        </button>
      </div>
    </header>
    <main class="public-shell__main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.public-shell {
  min-height: 100vh;
  background:
    linear-gradient(180deg, #ffffff 0 20%, transparent 20% 100%),
    linear-gradient(90deg, #dbeafe 0 34%, #ecfdf5 34% 68%, #f3f4f6 68% 100%);
}

.public-shell__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 1.25rem 2rem;
  border-bottom: 2px solid var(--fg);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(0);
}

.public-shell__nav {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  font-weight: 700;
}

.public-shell__nav a {
  padding-bottom: 0.25rem;
  border-bottom: 2px solid transparent;
}

.public-shell__nav a.router-link-active {
  border-color: var(--fg);
}

.public-shell__cta {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.9rem 1.2rem;
  border: 2px solid var(--fg);
  background: var(--primary);
  color: white;
  font-weight: 700;
}

.public-shell__actions {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
}

.public-shell__logout {
  min-height: auto;
  padding-block: 0.9rem;
}

.public-shell__main {
  width: min(1280px, calc(100vw - 2rem));
  margin: 0 auto;
  padding: 2rem 0 4rem;
}

@media (max-width: 900px) {
  .public-shell__header {
    flex-wrap: wrap;
    padding: 1rem;
  }

  .public-shell__nav {
    order: 3;
    width: 100%;
    justify-content: space-between;
  }

  .public-shell__actions {
    width: 100%;
    justify-content: flex-end;
  }

  .public-shell__main {
    width: min(1280px, calc(100vw - 1rem));
    padding-top: 1rem;
  }
}
</style>
