<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, LogIn, ShieldPlus, UserPlus } from 'lucide-vue-next'
import { ApiClientError } from '@/api/request'
import { authApi } from '@/api/auth'

const router = useRouter()

const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
})

function normalizeOptional(value: string) {
  const trimmed = value.trim()
  return trimmed.length > 0 ? trimmed : undefined
}

async function handleRegister() {
  if (form.username.trim().length === 0) {
    ElMessage.warning('请输入用户名')
    return
  }

  if (form.confirmPassword.length === 0) {
    ElMessage.warning('请再次输入确认密码')
    return
  }

  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  if (form.password.length < 8 || form.password.length > 64) {
    ElMessage.warning('密码长度需在 8 到 64 位之间')
    return
  }

  loading.value = true
  try {
    const profile = await authApi.register({
      username: form.username.trim(),
      password: form.password,
      nickname: normalizeOptional(form.nickname),
      phone: normalizeOptional(form.phone),
    })
    ElMessage.success({
      message: `注册成功，欢迎 ${profile.nickname || profile.username}`,
      duration: 1600,
    })
    await router.push({
      path: '/login',
      query: {
        username: profile.username,
      },
    })
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '注册失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <section class="register-page__poster">
      <div class="eyebrow">New User</div>
      <h1 class="poster-title">用户注册</h1>
      <article class="flat-panel flat-panel--green register-page__poster-card">
        <ShieldPlus :size="28" />
        <strong>注册后默认角色为 USER</strong>
        <span>可参与公开活动浏览、秒杀与下单流程</span>
      </article>
    </section>

    <section class="register-page__form-wrap">
      <div class="page-header page-header--blue">
        <div class="eyebrow">Create Account</div>
        <h2 class="register-page__heading">创建新账号</h2>
        <p class="poster-copy">用户名必填，密码长度 8 到 64 位。</p>
      </div>

      <el-form class="register-page__form flat-panel" label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirmPassword" show-password placeholder="请再次输入密码" />
        </el-form-item>
        <el-form-item label="昵称（可选）">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号（可选）">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <button
          class="flat-button register-page__submit"
          type="submit"
          :disabled="loading"
        >
          <UserPlus :size="18" />
          {{ loading ? '注册中...' : '完成注册' }}
          <ArrowRight :size="18" />
        </button>
      </el-form>

      <RouterLink class="flat-button flat-button--ghost register-page__login-entry" to="/login">
        <LogIn :size="18" />
        已有账号，去登录
      </RouterLink>
    </section>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(380px, 0.92fr);
  background: linear-gradient(90deg, #d1fae5 0 56%, #f3f4f6 56% 100%);
}

.register-page__poster {
  display: grid;
  align-content: center;
  gap: 1.25rem;
  padding: clamp(2rem, 5vw, 4rem);
  border-right: 2px solid var(--fg);
}

.register-page__poster-card {
  display: grid;
  gap: 0.75rem;
  max-width: 460px;
}

.register-page__poster-card strong {
  font-size: 1.1rem;
}

.register-page__poster-card span {
  color: var(--fg-soft);
  line-height: 1.6;
}

.register-page__form-wrap {
  display: grid;
  align-content: center;
  gap: 1rem;
  padding: clamp(1.5rem, 4vw, 3rem);
}

.register-page__heading {
  margin: 0;
  font-size: 2rem;
  font-weight: 800;
}

.register-page__form {
  display: grid;
  gap: 0.45rem;
}

.register-page__submit,
.register-page__login-entry {
  width: 100%;
}

@media (max-width: 960px) {
  .register-page {
    grid-template-columns: 1fr;
    background: linear-gradient(180deg, #d1fae5 0 42%, #f3f4f6 42% 100%);
  }

  .register-page__poster {
    border-right: 0;
    border-bottom: 2px solid var(--fg);
  }
}
</style>
