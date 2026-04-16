<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { LogOut, PanelLeft, Tickets } from 'lucide-vue-next'
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
  <div class="admin-shell">
    <aside class="admin-shell__aside" :class="{ 'admin-shell__aside--collapsed': !appStore.sidebarOpen }">
      <div class="admin-shell__brand">
        <AppBrand />
      </div>
      <RouterLink class="admin-shell__nav-item" to="/admin/activities">
        <Tickets :size="18" />
        <span v-if="appStore.sidebarOpen">活动管理</span>
      </RouterLink>
      <div class="admin-shell__poster">
        <span>Poster</span>
        <strong>Build publish-ready campaign blocks without shadows.</strong>
      </div>
    </aside>
    <div class="admin-shell__body">
      <header class="admin-shell__header">
        <button class="admin-shell__toggle" type="button" @click="appStore.toggleSidebar">
          <PanelLeft :size="18" />
        </button>
        <div>
          <div class="admin-shell__eyebrow">Control Surface</div>
          <div class="admin-shell__title">{{ appStore.pageTitle }}</div>
        </div>
        <div class="admin-shell__actions">
          <div class="admin-shell__user">
            <span>{{ userLabel }}</span>
            <small>{{ authStore.currentUser?.role ?? '游客' }}</small>
          </div>
          <button class="flat-button flat-button--ghost" type="button" @click="handleLogout">
            <LogOut :size="16" />
            退出
          </button>
        </div>
      </header>
      <main class="admin-shell__main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 264px minmax(0, 1fr);
  background: #f3f4f6;
}

.admin-shell__aside {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.25rem;
  border-right: 2px solid var(--fg);
  background: white;
}

.admin-shell__aside--collapsed {
  width: 96px;
}

.admin-shell__brand {
  padding-bottom: 0.75rem;
  border-bottom: 2px solid var(--fg);
}

.admin-shell__nav-item {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.95rem 1rem;
  border: 2px solid var(--fg);
  background: #dbeafe;
  font-weight: 800;
}

.admin-shell__poster {
  margin-top: auto;
  display: grid;
  gap: 0.5rem;
  padding: 1rem;
  border: 2px solid var(--fg);
  background: #ecfdf5;
  font-size: 0.9rem;
}

.admin-shell__poster span {
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.72rem;
  font-weight: 800;
}

.admin-shell__body {
  min-width: 0;
}

.admin-shell__header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.5rem;
  border-bottom: 2px solid var(--fg);
  background: white;
}

.admin-shell__toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  border: 2px solid var(--fg);
  background: #f3f4f6;
}

.admin-shell__eyebrow {
  color: var(--fg-soft);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-shell__title {
  margin-top: 0.2rem;
  font-size: 1.4rem;
  font-weight: 800;
}

.admin-shell__actions {
  display: inline-flex;
  align-items: center;
  gap: 1rem;
}

.admin-shell__user {
  display: grid;
  justify-items: end;
}

.admin-shell__user span {
  font-weight: 700;
}

.admin-shell__user small {
  color: var(--fg-soft);
}

.admin-shell__main {
  padding: 1.5rem;
}

@media (max-width: 960px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-shell__aside {
    display: none;
  }

  .admin-shell__header {
    grid-template-columns: auto 1fr;
  }

  .admin-shell__actions {
    grid-column: 1 / -1;
    justify-content: space-between;
  }

  .admin-shell__main {
    padding: 1rem;
  }
}
</style>
