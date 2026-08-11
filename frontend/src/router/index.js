import { createRouter, createWebHistory } from 'vue-router'
import { useAssetLinkStore } from '@/stores/assetLink'
import { useAuthStore } from '@/stores/auth'
import { useSignupStore } from '@/stores/signup'
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
 * 모든 화면은 iPhone 12 Pro Max(428 x 926) 기준 프레임(max-w-[428px]) 위에 렌더된다.
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
        // 회원가입 요청을 보내는 단계라 1·2단계 입력이 반드시 있어야 한다. 스토어는 메모리에만
        // 있어서 새로고침하면 비므로, 그 상태로 들어오면 처음부터 다시 받는다.
        // (화면이 뜬 뒤에 이동을 걸면 진행 중인 내비게이션과 충돌한다)
        beforeEnter: () => (useSignupStore().isReady ? true : { name: 'signup' }),
      },
      {
        path: 'signup/asset',
        name: 'signup-asset',
        component: () => import('@/pages/auth/AssetLinkPage.vue'),
      },
      {
        path: 'signup/asset/institutions',
        name: 'signup-asset-institutions',
        component: () => import('@/pages/auth/InstitutionsPage.vue'),
      },
      {
        path: 'signup/asset/linking',
        name: 'signup-asset-linking',
        component: () => import('@/pages/auth/AssetLinkingPage.vue'),
        // 이미 연동해 결과를 들고 있으면 다시 불러올 것이 없다. 완료 화면으로 바로 보낸다.
        //
        // 연동 화면은 뜨자마자 API 를 부르고, 이미 연동한 회원이면 409 를 받고 나서야 이동한다.
        // 그래서 여기서 막지 않으면 <b>연동 화면이 한 번 번쩍 보였다가</b> 넘어간다.
        beforeEnter: () => (useAssetLinkStore().hasResult ? { name: 'signup-asset-done' } : true),
      },
      {
        path: 'signup/asset/done',
        name: 'signup-asset-done',
        component: () => import('@/pages/auth/AssetLinkedPage.vue'),
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
        // 커플 연동 방법을 선택하는 시작 화면.
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
        meta: {
          activeTab: 'dashboard',
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
  scrollBehavior() {
    return { top: 0 }
  },
})

/**
 * 인증 가드.
 *
 * {@code meta.public} 이 없는 라우트는 로그인이 필요한 것으로 본다. 토큰이 없으면 화면을 띄우지
 * 않고 로그인으로 보내고, 원래 가려던 주소를 {@code redirect} 로 넘긴다.
 *
 * <b>이것은 보안장치가 아니다.</b> 브라우저 코드라 우회할 수 있고, 실제로 데이터를 지키는 것은
 * 백엔드 시큐리티 필터다. 여기서 막는 이유는 <b>헛걸음을 없애기 위해서</b>다 — 없으면 사용자는
 * 화면만 뜨고 내용은 비어 있는 상태를 보고 나서야 로그인이 필요한 줄 알게 된다.
 */
router.beforeEach((to) => {
  const authStore = useAuthStore()

  // localStorage 의 실제 값과 먼저 맞춘다. client.js 의 응답 인터셉터는 401 을 받으면 스토어를
  // 거치지 않고 토큰을 지우므로, 이걸 빼면 스토어만 "아직 로그인 중"이라고 착각해 통과시킨다.
  authStore.syncFromStorage()

  if (!to.meta.public && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
