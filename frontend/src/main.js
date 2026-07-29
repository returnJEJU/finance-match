import '@/assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { VueQueryPlugin } from '@tanstack/vue-query'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia()) // 상태 관리
app.use(router) // 라우팅
app.use(VueQueryPlugin) // 서버 데이터(TanStack Query)

app.mount('#app')
