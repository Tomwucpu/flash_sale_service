<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { House, LogOut, PanelLeft, ShoppingBag, Tickets, UserRound } from 'lucide-vue-next'
import AppBrand from '@/components/AppBrand.vue'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const authStore = useAuthStore()

const isAdmin = computed(() => route.path.startsWith('/admin'))

const userLabel = computed(() => authStore.currentUser?.nickname || authStore.currentUser?.username || '未登录')

const eyebrow = computed(() => (isAdmin.value ? 'Control Surface' : 'User Workspace'))

const title = computed(() => (isAdmin.value ? '管理员后台' : '用户后台'))

const navItems = computed(() =>
  isAdmin.value
    ? [
        { to: '/admin/activities', icon: Tickets, label: '活动管理' },
        { to: '/admin/profile', icon: UserRound, label: '我的' },
      ]
    : [
        { to: '/user/orders', icon: ShoppingBag, label: '我的订单' },
        { to: '/user/profile', icon: UserRound, label: '我的' },
      ],
)

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="shell" :class="{ 'shell--collapsed': !appStore.sidebarOpen }">
    <aside class="shell__aside" :class="{ 'shell__aside--collapsed': !appStore.sidebarOpen }">
      <div class="shell__brand">
        <AppBrand :compact="!appStore.sidebarOpen" />
      </div>
      <nav class="shell__nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          class="shell__nav-item"
          :to="item.to"
        >
          <component :is="item.icon" :size="18" />
          <span v-if="appStore.sidebarOpen">{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>
    <div class="shell__body">
      <header class="shell__header">
        <button class="shell__toggle" type="button" @click="appStore.toggleSidebar">
          <PanelLeft :size="18" />
        </button>
        <div>
          <div class="shell__eyebrow">{{ eyebrow }}</div>
          <div class="shell__title">{{ title }}</div>
        </div>
        <div class="shell__actions">
          <div class="shell__user">
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
      <main class="shell__main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell {
  --aside-width: 264px;
  min-height: 100vh;
  display: grid;
  grid-template-columns: var(--aside-width) minmax(0, 1fr);
  background: #f3f4f6;
  transition: grid-template-columns 0.28s ease;
}

.shell--collapsed {
  --aside-width: 96px;
}

.shell__aside {
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

.shell__aside--collapsed {
  gap: 0.75rem;
  padding-inline: 0.65rem;
  align-items: center;
}

.shell__brand {
  padding-bottom: 0.75rem;
  border-bottom: 2px solid var(--fg);
  transition: padding 0.28s ease;
}

.shell__aside--collapsed .shell__brand {
  width: 100%;
  display: flex;
  justify-content: center;
  padding-bottom: 0.5rem;
}

.shell__nav {
  display: grid;
  gap: 0.75rem;
}

.shell__nav-item {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.95rem 1rem;
  border: 2px solid var(--fg);
  background: #ffffff;
  font-weight: 800;
  transition:
    padding 0.24s ease,
    gap 0.24s ease,
    justify-content 0.24s ease;
}

.shell__nav-item.router-link-active {
  background: #dbeafe;
}

.shell__aside--collapsed .shell__nav-item {
  width: 3.5rem;
  height: 3.5rem;
  justify-content: center;
  gap: 0;
  padding: 0;
}

.shell__body {
  min-width: 0;
}

.shell__header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.5rem;
  border-bottom: 2px solid var(--fg);
  background: white;
}

.shell__toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  border: 2px solid var(--fg);
  background: #f3f4f6;
}

.shell__eyebrow {
  color: var(--fg-soft);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.shell__title {
  margin-top: 0.2rem;
  font-size: 1.4rem;
  font-weight: 800;
}

.shell__actions {
  display: inline-flex;
  align-items: center;
  gap: 1rem;
}

.shell__user {
  display: grid;
  justify-items: end;
}

.shell__user span {
  font-weight: 700;
}

.shell__user small {
  color: var(--fg-soft);
}

.shell__main {
  padding: 1.5rem;
}

@media (max-width: 960px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .shell__aside {
    display: none;
  }

  .shell__header {
    grid-template-columns: auto 1fr;
  }

  .shell__actions {
    grid-column: 1 / -1;
    justify-content: space-between;
  }

  .shell__main {
    padding: 1rem;
  }
}
</style>
