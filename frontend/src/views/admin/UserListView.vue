<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Ban, CircleCheck, Search } from 'lucide-vue-next'
import { userApi } from '@/api/user'
import { ApiClientError } from '@/api/request'
import { useAuthStore } from '@/stores/auth'
import StatusBadge from '@/components/StatusBadge.vue'
import type { UserPageParams, UserPageResponse, UserProfile } from '@/types'

const authStore = useAuthStore()
const loading = ref(false)
const users = ref<UserProfile[]>([])
const total = ref(0)

const filters = ref<UserPageParams>({
  keyword: '',
  role: '',
  status: '',
  page: 1,
  size: 10,
})

const summary = computed(() => ({
  total: total.value,
}))

const roleOptions = [
  { label: '管理员', value: 'ADMIN' },
  { label: '发行商', value: 'PUBLISHER' },
  { label: '普通用户', value: 'USER' },
]

function getStatusTone(status: string) {
  return status === 'ENABLED' ? 'green' : 'amber'
}

async function loadUsers() {
  loading.value = true
  try {
    const response: UserPageResponse = await userApi.list(filters.value)
    users.value = response.records
    total.value = response.total
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '用户列表加载失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

async function handleToggleStatus(user: UserProfile) {
  const isDisabling = user.status === 'ENABLED'
  const action = isDisabling ? '禁用' : '启用'

  await ElMessageBox.confirm(
    `确认${action}用户「${user.nickname || user.username}」？`,
    `${action}用户`,
    {
      type: 'warning',
      confirmButtonText: `确认${action}`,
    },
  )

  const newStatus = isDisabling ? 'DISABLED' : 'ENABLED'
  await userApi.updateStatus(user.id, { status: newStatus })
  ElMessage.success(`用户已${action}`)
  await loadUsers()
}

async function handleChangeRole(user: UserProfile, newRole: string) {
  if (newRole === user.role) return

  try {
    await userApi.updateRole(user.id, { role: newRole as 'ADMIN' | 'PUBLISHER' | 'USER' })
    ElMessage.success('角色已更新')
    await loadUsers()
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '角色更新失败'
    ElMessage.error(message)
  }
}

function handleSizeChange(size: number) {
  filters.value.size = size
  filters.value.page = 1
  loadUsers()
}

function handlePageChange(page: number) {
  filters.value.page = page
  loadUsers()
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    filters.value.page = 1
    loadUsers()
  }, 300)
}

watch(() => filters.value.role, () => {
  filters.value.page = 1
  loadUsers()
})

watch(() => filters.value.status, () => {
  filters.value.page = 1
  loadUsers()
})

onMounted(loadUsers)
</script>

<template>
  <div class="page-shell">
    <section class="page-header page-header--blue">
      <div class="eyebrow">User Console</div>
      <h1 class="poster-title">用户管理</h1>
      <div class="stat-grid">
        <article class="stat-block">
          <strong>{{ summary.total }}</strong>
          <span>用户总数</span>
        </article>
        <article class="stat-block" style="background: #dbeafe">
          <strong>{{ users.filter(u => u.role === 'ADMIN').length }}</strong>
          <span>管理员</span>
        </article>
        <article class="stat-block" style="background: #fef3c7">
          <strong>{{ users.filter(u => u.role === 'PUBLISHER').length }}</strong>
          <span>发行商</span>
        </article>
        <article class="stat-block" style="background: #e5e7eb">
          <strong>{{ users.filter(u => u.role === 'USER').length }}</strong>
          <span>普通用户</span>
        </article>
      </div>
    </section>

    <section class="flat-panel">
      <div class="list-toolbar">
        <div>
          <div class="eyebrow">Manage</div>
          <h2>用户列表</h2>
        </div>
      </div>

      <div class="filter-bar">
        <div class="filter-search">
          <Search :size="16" />
          <input
            v-model="filters.keyword"
            type="text"
            placeholder="搜索用户名 / 昵称 / 手机号..."
            @input="onSearchInput"
          />
        </div>
        <select v-model="filters.role" class="filter-select">
          <option value="">全部角色</option>
          <option value="ADMIN">管理员</option>
          <option value="PUBLISHER">发行商</option>
          <option value="USER">普通用户</option>
        </select>
        <select v-model="filters.status" class="filter-select">
          <option value="">全部状态</option>
          <option value="ENABLED">启用</option>
          <option value="DISABLED">禁用</option>
        </select>
      </div>

      <div v-loading="loading" class="table-container">
        <el-table v-if="users.length > 0" :data="users" row-key="id" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" sortable />
          <el-table-column prop="username" label="用户名" min-width="140" sortable>
            <template #default="{ row }">
              <strong>{{ row.username }}</strong>
            </template>
          </el-table-column>
          <el-table-column prop="nickname" label="昵称" min-width="120">
            <template #default="{ row }">
              {{ row.nickname || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" min-width="140">
            <template #default="{ row }">
              {{ row.phone || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="role" label="角色" width="160">
            <template #default="{ row }">
              <el-select
                :model-value="row.role"
                :disabled="authStore.currentUser?.id === row.id"
                size="small"
                style="width: 100%"
                @change="handleChangeRole(row, $event as string)"
              >
                <el-option
                  v-for="opt in roleOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <StatusBadge
                :label="row.status === 'ENABLED' ? '启用' : '禁用'"
                :tone="getStatusTone(row.status)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <button
                class="flat-button"
                type="button"
                :disabled="authStore.currentUser?.id === row.id"
                :class="row.status === 'ENABLED' ? 'flat-button--ghost' : ''"
                @click="handleToggleStatus(row)"
              >
                <Ban v-if="row.status === 'ENABLED'" :size="16" />
                <CircleCheck v-else :size="16" />
                {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
              </button>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-state">
          <strong>暂无用户数据</strong>
        </div>
      </div>

      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          :current-page="filters.page"
          :page-size="filters.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.list-toolbar h2 {
  margin: 0.25rem 0 0;
  font-size: 1.6rem;
}

.filter-bar {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
  align-items: center;
}

.filter-search {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  min-width: 220px;
  padding: 0.55rem 0.75rem;
  border: 2px solid var(--fg);
  background: white;
}

.filter-search input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 0.92rem;
  font-family: inherit;
}

.filter-select {
  padding: 0.55rem 0.75rem;
  border: 2px solid var(--fg);
  background: white;
  font-size: 0.92rem;
  font-family: inherit;
  min-width: 120px;
}

.table-container {
  min-width: 0;
  width: 100%;
  overflow-x: auto;
}

.empty-state {
  display: grid;
  justify-items: start;
  gap: 0.9rem;
  padding: 1.5rem;
  border: 2px dashed var(--border);
  background: var(--muted);
}

.empty-state strong {
  font-size: 1.1rem;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 1.25rem;
}

@media (max-width: 960px) {
  .list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-bar {
    flex-direction: column;
  }

  .filter-search {
    min-width: 0;
  }
}
</style>
