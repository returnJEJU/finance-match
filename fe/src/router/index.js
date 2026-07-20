import { createRouter, createWebHistory } from 'vue-router'

/**
 * 라우터 기본 구조.
 *
 * 화면(페이지)이 아직 확정되지 않았으므로, 팀원이 자기 화면을 만들면서 여기 routes 에 추가한다.
 * 라우트에는 화면 ID 를 meta.screenId 로 기재한다. 예:
 *   { path: '/login', name: 'login',
 *     component: () => import('@/pages/LoginPage.vue'),
 *     meta: { screenId: 'AUTH-03', public: true } }
 */
const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/pages/HomePage.vue'),
    meta: { public: true },
  },
  // 없는 경로는 홈으로
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/**
 * 인증 가드 (뼈대).
 *
 * 인증 도메인(로그인 화면 + authStore)이 준비되면 아래 주석을 활성화한다.
 * meta.public 이 없는 라우트는 로그인이 필요한 것으로 간주한다.
 */
router.beforeEach(() => {
  // 인증 가드 — 화면과 authStore 가 준비되면 아래처럼 파라미터(to)를 받아 활성화한다:
  //   router.beforeEach((to) => {
  //     const authStore = useAuthStore()
  //     if (!to.meta.public && !authStore.isAuthenticated) {
  //       return { name: 'login', query: { redirect: to.fullPath } }
  //     }
  //     return true
  //   })
  return true
})

export default router
