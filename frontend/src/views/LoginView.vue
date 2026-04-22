<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, ShieldCheck, Ticket, UserRound } from 'lucide-vue-next'
import { ApiClientError } from '@/api/request'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'FlashSale@123',
})

async function handleLogin() {
  loading.value = true
  try {
    const session = await authStore.login(form)
    ElMessage.success({
      message: `欢迎回来，${session.user.nickname || session.user.username}`,
      duration: 1500,
    })
    await router.push(authStore.isAdminLike ? '/admin/activities' : '/public/home')
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : '登录失败，请检查后端服务是否已启动'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-page__poster">
      <div class="eyebrow">Admin Access</div>
      <h1 class="poster-title">后台登录</h1>
      <div class="login-page__stats">
        <article class="flat-panel flat-panel--blue">
          <ShieldCheck :size="26" />
          <strong>ADMIN / PUBLISHER</strong>
          <span>活动管理权限</span>
        </article>
        <article class="flat-panel flat-panel--green">
          <UserRound :size="26" />
          <strong>USER</strong>
          <span>用户侧浏览</span>
        </article>
      </div>
    </section>

    <section class="login-page__form-wrap">
      <div class="page-header page-header--amber">
        <div class="eyebrow">Sign In</div>
        <h2 class="login-page__heading">后台入口</h2>
        <p class="poster-copy">默认密码：<strong>FlashSale@123</strong></p>
      </div>

      <el-form class="login-page__form flat-panel" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password placeholder="请输入密码" />
        </el-form-item>
        <button class="flat-button login-page__submit" type="button" :disabled="loading" @click="handleLogin">
          <Ticket :size="18" />
          {{ loading ? '登录中...' : '进入控制台' }}
          <ArrowRight :size="18" />
        </button>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  background: linear-gradient(90deg, #dbeafe 0 58%, #f3f4f6 58% 100%);
}

.login-page__poster {
  display: grid;
  align-content: center;
  gap: 1.5rem;
  padding: clamp(2rem, 5vw, 4rem);
  border-right: 2px solid var(--fg);
}

.login-page__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.login-page__stats article {
  display: grid;
  gap: 0.9rem;
  min-height: 220px;
}

.login-page__stats strong {
  font-size: 1.05rem;
}

.login-page__stats span {
  color: var(--fg-soft);
  line-height: 1.6;
}

.login-page__form-wrap {
  display: grid;
  align-content: center;
  gap: 1rem;
  padding: clamp(1.5rem, 4vw, 3rem);
}

.login-page__heading {
  margin: 0;
  font-size: 2rem;
  font-weight: 800;
}

.login-page__form {
  display: grid;
  gap: 1rem;
}

.login-page__submit {
  width: 100%;
}

@media (max-width: 960px) {
  .login-page {
    grid-template-columns: 1fr;
    background: linear-gradient(180deg, #dbeafe 0 45%, #f3f4f6 45% 100%);
  }

  .login-page__poster {
    border-right: 0;
    border-bottom: 2px solid var(--fg);
  }

  .login-page__stats {
    grid-template-columns: 1fr;
  }
}
</style>
