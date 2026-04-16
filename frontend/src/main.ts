import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import { createPinia } from 'pinia'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import { configureHttpAuth } from '@/api/http'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus)

const authStore = useAuthStore(pinia)
authStore.hydrateFromStorage()

configureHttpAuth({
  getAccessToken: () => authStore.accessToken,
  onUnauthorized: () => {
    authStore.clearSession()
    if (window.location.pathname !== '/login') {
      router.push('/login')
    }
  },
})

app.mount('#app')
