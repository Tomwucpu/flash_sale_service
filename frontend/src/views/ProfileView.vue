<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { KeyRound, Pencil, Save, UserRound, X } from 'lucide-vue-next'
import { authApi } from '@/api/auth'
import { ApiClientError } from '@/api/request'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const editingProfile = ref(false)
const savingProfile = ref(false)
const passwordDialogVisible = ref(false)
const changingPassword = ref(false)

const profileForm = reactive({
  nickname: '',
  phone: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const currentUser = computed(() => authStore.currentUser)
const displayName = computed(
  () => currentUser.value?.nickname || currentUser.value?.username || '未登录',
)

const roleLabels: Record<string, string> = {
  ADMIN: '管理员',
  PUBLISHER: '卖家',
  USER: '用户',
}

const statusLabels: Record<string, string> = {
  ENABLED: '启用',
  DISABLED: '禁用',
}

function normalizeOptional(value: string) {
  const trimmed = value.trim()
  return trimmed.length > 0 ? trimmed : null
}

function resetProfileForm() {
  profileForm.nickname = currentUser.value?.nickname ?? ''
  profileForm.phone = currentUser.value?.phone ?? ''
}

function startEditProfile() {
  resetProfileForm()
  editingProfile.value = true
}

function cancelEditProfile() {
  resetProfileForm()
  editingProfile.value = false
}

function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

function openPasswordDialog() {
  resetPasswordForm()
  passwordDialogVisible.value = true
}

async function handleSaveProfile() {
  savingProfile.value = true
  try {
    const profile = await authApi.updateProfile({
      nickname: normalizeOptional(profileForm.nickname),
      phone: normalizeOptional(profileForm.phone),
    })
    authStore.updateCurrentUser(profile)
    editingProfile.value = false
    ElMessage.success('资料已保存')
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '资料保存失败'
    ElMessage.error(message)
  } finally {
    savingProfile.value = false
  }
}

async function handleChangePassword() {
  if (passwordForm.oldPassword.length === 0) {
    ElMessage.warning('请输入旧密码')
    return
  }

  if (passwordForm.newPassword.length < 8 || passwordForm.newPassword.length > 64) {
    ElMessage.warning('新密码长度需在 8 到 64 位之间')
    return
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  changingPassword.value = true
  try {
    await authApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    resetPasswordForm()
    passwordDialogVisible.value = false
    ElMessage.success('密码已修改')
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '密码修改失败'
    ElMessage.error(message)
  } finally {
    changingPassword.value = false
  }
}

watch(currentUser, resetProfileForm, { immediate: true })
</script>

<template>
  <div class="page-shell profile-page">
    <section class="page-header page-header--blue profile-hero">
      <div>
        <div class="eyebrow">Profile</div>
        <h1 class="poster-title">我的</h1>
      </div>
      <div class="profile-summary">
        <article class="stat-block">
          <span>账号</span>
          <strong>{{ currentUser?.username ?? '-' }}</strong>
        </article>
        <article class="stat-block" style="background: #d1fae5">
          <span>角色</span>
          <strong>{{ roleLabels[currentUser?.role ?? ''] ?? currentUser?.role ?? '-' }}</strong>
        </article>
        <article class="stat-block" style="background: #fef3c7">
          <span>状态</span>
          <strong>{{ statusLabels[currentUser?.status ?? ''] ?? currentUser?.status ?? '-' }}</strong>
        </article>
      </div>
    </section>

    <section class="flat-panel profile-panel">
      <div class="profile-panel__heading">
        <UserRound :size="24" />
        <div>
          <div class="eyebrow">Basic Info</div>
          <h2>基础信息</h2>
        </div>
      </div>

      <div class="meta-list profile-current">
        <div class="meta-row">
          <span>用户名</span>
          <strong>{{ currentUser?.username ?? '-' }}</strong>
        </div>
        <div class="meta-row">
          <span>昵称</span>
          <strong>{{ displayName }}</strong>
        </div>
        <div class="meta-row">
          <span>手机号</span>
          <strong>{{ currentUser?.phone || '-' }}</strong>
        </div>
        <div class="meta-row">
          <span>角色</span>
          <strong>{{ roleLabels[currentUser?.role ?? ''] ?? currentUser?.role ?? '-' }}</strong>
        </div>
        <div class="meta-row">
          <span>状态</span>
          <strong>{{ statusLabels[currentUser?.status ?? ''] ?? currentUser?.status ?? '-' }}</strong>
        </div>
      </div>

      <button v-if="!editingProfile" class="flat-button profile-action" type="button" @click="startEditProfile">
        <Pencil :size="18" />
        修改资料
      </button>

      <el-form v-else class="profile-edit-form" label-position="top" @submit.prevent="handleSaveProfile">
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" maxlength="64" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profileForm.phone" maxlength="32" placeholder="请输入手机号" />
        </el-form-item>
        <div class="profile-edit-actions">
          <button class="flat-button flat-button--ghost" type="button" :disabled="savingProfile" @click="cancelEditProfile">
            <X :size="18" />
            取消
          </button>
          <button class="flat-button profile-action" type="submit" :disabled="savingProfile">
            <Save :size="18" />
            {{ savingProfile ? '保存中...' : '保存资料' }}
          </button>
        </div>
      </el-form>
    </section>

    <section class="flat-panel profile-panel">
      <div class="profile-panel__heading">
        <KeyRound :size="24" />
        <div>
          <div class="eyebrow">Password</div>
          <h2>修改密码</h2>
        </div>
      </div>
      <button class="flat-button profile-action" type="button" @click="openPasswordDialog">
        <KeyRound :size="18" />
        修改密码
      </button>
    </section>

    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="min(92vw, 520px)"
      destroy-on-close
      @closed="resetPasswordForm"
    >
      <el-form class="password-dialog-form" label-position="top" @submit.prevent="handleChangePassword">
        <el-form-item label="旧密码">
          <el-input v-model="passwordForm.oldPassword" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="passwordForm.confirmPassword" show-password placeholder="请再次输入新密码" />
        </el-form-item>
        <div class="profile-edit-actions">
          <button
            class="flat-button flat-button--ghost"
            type="button"
            :disabled="changingPassword"
            @click="passwordDialogVisible = false"
          >
            <X :size="18" />
            取消
          </button>
          <button class="flat-button profile-action" type="submit" :disabled="changingPassword">
            <KeyRound :size="18" />
            {{ changingPassword ? '修改中...' : '确认修改' }}
          </button>
        </div>
      </el-form>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-page {
  width: 100%;
  max-width: none;
}

.profile-hero {
  gap: 1.25rem;
}

.profile-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.profile-summary .stat-block strong {
  margin-top: 0.45rem;
  font-size: clamp(1.2rem, 2vw, 1.7rem);
  line-height: 1.15;
  overflow-wrap: anywhere;
}

.profile-panel {
  display: grid;
  gap: 1rem;
  width: 100%;
}

.profile-panel__heading {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.profile-panel__heading h2 {
  margin: 0.25rem 0 0;
  font-size: 1.35rem;
}

.profile-current {
  padding: 1rem;
  border: 2px solid var(--fg);
  background: var(--muted);
}

.profile-current .meta-row:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.profile-panel > .profile-action {
  width: 100%;
}

.profile-edit-form,
.password-dialog-form {
  display: grid;
  gap: 0.35rem;
}

.profile-edit-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.profile-edit-actions .flat-button {
  min-width: 10rem;
}

@media (max-width: 960px) {
  .profile-summary {
    grid-template-columns: 1fr;
  }
}
</style>
