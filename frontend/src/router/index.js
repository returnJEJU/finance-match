import { createRouter, createWebHistory } from 'vue-router'
import BlankLayout from '@/layouts/BlankLayout.vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

/**
 * 라우터 — 화면 스캐폴드.
 *
 * 레이아웃을 부모 라우트로 두고, 화면(페이지)을 자식으로 둔다. (중첩 라우트)
 *   - BlankLayout   : 퍼널(온보딩·커플·설문·계산중 등) — 네비바 없음
 *   - DefaultLayout : 허브(대시보드·추천·리포트·마이) — 네비바 있음
 * 각 페이지는 뼈대만 있으니, 담당자가 해당 파일의 <template> 을 채우면 된다.
 *
 * meta:
 *   - public   : 로그인 없이 접근 가능 (인증 가드용)
 *   - title    : 상단바 제목 (없으면 로고 표시) — DefaultLayout 에서 사용
 *   - showBack : 상단바 뒤로가기 버튼 표시 — DefaultLayout 에서 사용
 *
 * 모든 화면은 iPhone 16(393 x 852) 기준 프레임(max-w-[393px]) 위에 렌더된다.
 */
const routes = [
  // ── 퍼널 (BlankLayout · 네비바 없음) ──
  {
    path: '/',
    component: BlankLayout,
    children: [
      {
        path: '',
        name: 'onboarding',
        component: () => import('@/pages/OnboardingPage.vue'),
        meta: { public: true },
      },
      {
        path: 'signup',
        name: 'signup',
        component: () => import('@/pages/auth/SignupPage.vue'),
        meta: { public: true },
      },
      {
        path: 'signup/agree',
        name: 'signup-agree',
        component: () => import('@/pages/auth/SignupAgreePage.vue'),
        meta: { public: true },
      },
      {
        path: 'signup/cert',
        name: 'signup-cert',
        component: () => import('@/pages/auth/SignupCertPage.vue'),
        meta: { public: true },
      },
      {
        path: 'signup/asset',
        name: 'signup-asset',
        component: () => import('@/pages/auth/AssetLinkPage.vue'),
        meta: { public: true },
      },
      {
        path: 'signup/asset/institutions',
        name: 'signup-asset-institutions',
        component: () => import('@/pages/auth/InstitutionsPage.vue'),
        meta: { public: true },
      },
      {
        path: 'signup/asset/linking',
        name: 'signup-asset-linking',
        component: () => import('@/pages/auth/AssetLinkingPage.vue'),
        meta: { public: true },
      },
      {
        path: 'signup/asset/done',
        name: 'signup-asset-done',
        component: () => import('@/pages/auth/AssetLinkedPage.vue'),
        meta: { public: true },
      },
      {
        path: 'login',
        name: 'login',
        component: () => import('@/pages/auth/LoginPage.vue'),
        meta: { public: true },
      },
      {
        path: 'service-introduction',
        name: 'service-introduction',
        component: () => import('@/pages/ServiceIntroductionPage.vue'),
      },
      {
        path: 'couple',
        name: 'couple-start',
        component: () => import('@/pages/CoupleStartPage.vue'),
      },
      {
        path: 'couple/invite',
        name: 'couple-invite',
        component: () => import('@/pages/InviteCodePage.vue'),
      },
      {
        path: 'couple/invite/created',
        name: 'couple-invite-created',
        component: () => import('@/pages/InviteCreatedPage.vue'),
      },
      {
        path: 'couple/connected',
        name: 'couple-connected',
        component: () => import('@/pages/CoupleConnectedPage.vue'),
      },
      {
        path: 'survey/couple',
        name: 'survey-couple',
        component: () => import('@/pages/CoupleSurveyPage.vue'),
      },
      {
        path: 'survey/personal',
        name: 'survey-personal',
        component: () => import('@/pages/PersonalSurveyPage.vue'),
      },
      {
        path: 'survey/result',
        name: 'survey-result',
        component: () => import('@/pages/SurveyResultPage.vue'),
      },
      {
        path: 'match/calculating',
        name: 'match-calculating',
        component: () => import('@/pages/MatchCalculatingPage.vue'),
      },
    ],
  },

  // ── 허브 (DefaultLayout · 네비바 있음) ──
  {
    path: '/',
    component: DefaultLayout,
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/pages/DashboardPage.vue'),
      },
      {
        path: 'dashboard/waiting',
        name: 'dashboard-waiting',
        component: () => import('@/pages/DashboardWaitingPage.vue'),
        // 상대방이 설문을 마치기 전까지는 갈 수 있는 화면이 없다.
        // 하단 탭(navLocked)과 마찬가지로 상단바 뒤로가기도 두지 않는다.
        meta: {
          activeTab: 'dashboard',
          navLocked: true,
        },
      },
      {
        path: 'report',
        name: 'report',
        component: () => import('@/pages/ReportPage.vue'),
        meta: { title: '리포트' },
      },
      {
        path: 'recommend',
        name: 'recommend',
        component: () => import('@/pages/RecommendPage.vue'),
        meta: { title: '상품 추천' },
      },
      {
        path: 'recommend/products',
        name: 'recommend-products',
        component: () => import('@/pages/ProductListPage.vue'),
        meta: { title: '상품 목록', showBack: true },
      },
      {
        path: 'my',
        name: 'my',
        component: () => import('@/pages/MyPage.vue'),
        meta: { title: '마이페이지' },
      },
    ],
  },

  // 없는 경로 → 홈
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
  // 활성화 시 파라미터(to)를 받아 사용:
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
