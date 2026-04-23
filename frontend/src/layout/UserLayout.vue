<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { House, LogOut, PanelLeft, ShoppingBag } from 'lucide-vue-next'
import AppBrand from '@/components/AppBrand.vue'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()

const userLabel = computed(() => authStore.currentUser?.nickname || authStore.currentUser?.username || '未登录')

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="user-shell" :class="{ 'user-shell--collapsed': !appStore.sidebarOpen }">
    <aside class="user-shell__aside" :class="{ 'user-shell__aside--collapsed': !appStore.sidebarOpen }">
      <div class="user-shell__brand">
        <AppBrand :compact="!appStore.sidebarOpen" />
      </div>
      <nav class="user-shell__nav">
        <RouterLink class="user-shell__nav-item" to="/user/orders">
          <ShoppingBag :size="18" />
          <span v-if="appStore.sidebarOpen">我的订单</span>
        </RouterLink>
      </nav>
    </aside>
    <div class="user-shell__body">
      <header class="user-shell__header">
        <button class="user-shell__toggle" type="button" @click="appStore.toggleSidebar">
          <PanelLeft :size="18" />
        </button>
        <div>
          <div class="user-shell__eyebrow">User Workspace</div>
          <div class="user-shell__title">用户后台</div>
        </div>
        <div class="user-shell__actions">
          <div class="user-shell__user">
            <span>{{ userLabel }}</span>
            <small>{{ authStore.currentUser?.role ?? '游客' }}</small>
          </div>
          <RouterLink class="flat-button flat-button--ghost" to="/public/home">
            <House :size="16" />
            首页
          </RouterLink>
          <button class="flat-button flat-button--ghost" type="button" @click="handleLogout">
            <LogOut :size="16" />
            退出
          </button>
        </div>
      </header>
      <main class="user-shell__main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.user-shell {
  --aside-width: 264px;
  min-height: 100vh;
  display: grid;
  grid-template-columns: var(--aside-width) minmax(0, 1fr);
  background: #f3f4f6;
  transition: grid-template-columns 0.28s ease;
}

.user-shell--collapsed {
  --aside-width: 96px;
}

.user-shell__aside {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.25rem;
  border-right: 2px solid var(--fg);
  background: white;
  overflow: hidden;
  transition:
    gap 0.28s ease,
    padding 0.28s ease;
}

.user-shell__aside--collapsed {
  gap: 0.75rem;
  padding-inline: 0.65rem;
  align-items: center;
}

.user-shell__brand {
  padding-bottom: 0.75rem;
  border-bottom: 2px solid var(--fg);
  transition: padding 0.28s ease;
}

.user-shell__aside--collapsed .user-shell__brand {
  width: 100%;
  display: flex;
  justify-content: center;
  padding-bottom: 0.5rem;
}

.user-shell__nav {
  display: grid;
  gap: 0.75rem;
}

.user-shell__nav-item {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.95rem 1rem;
  border: 2px solid var(--fg);
  background: #d1fae5;
  font-weight: 800;
  transition:
    padding 0.24s ease,
    gap 0.24s ease,
    justify-content 0.24s ease;
}

.user-shell__nav-item.router-link-active {
  background: #dbeafe;
}

.user-shell__aside--collapsed .user-shell__nav-item {
  width: 3.5rem;
  height: 3.5rem;
  justify-content: center;
  gap: 0;
  padding: 0;
}

.user-shell__body {
  min-width: 0;
}

.user-shell__header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.5rem;
  border-bottom: 2px solid var(--fg);
  background: white;
}

.user-shell__toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  border: 2px solid var(--fg);
  background: #f3f4f6;
}

.user-shell__eyebrow {
  color: var(--fg-soft);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.user-shell__title {
  margin-top: 0.2rem;
  font-size: 1.4rem;
  font-weight: 800;
}

.user-shell__actions {
  display: inline-flex;
  align-items: center;
  gap: 1rem;
}

.user-shell__user {
  display: grid;
  justify-items: end;
}

.user-shell__user span {
  font-weight: 700;
}

.user-shell__user small {
  color: var(--fg-soft);
}

.user-shell__main {
  padding: 1.5rem;
}

@media (max-width: 960px) {
  .user-shell {
    grid-template-columns: 1fr;
  }

  .user-shell__aside {
    display: none;
  }

  .user-shell__header {
    grid-template-columns: auto 1fr;
  }

  .user-shell__actions {
    grid-column: 1 / -1;
    justify-content: space-between;
  }

  .user-shell__main {
    padding: 1rem;
  }
}
</style>
