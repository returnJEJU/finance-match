<script setup>
// TODO(담당자): 이 화면을 구현하세요. (기준: 찰떡궁합_UI.pdf)
// 화면: 마이페이지 · 레이아웃: DefaultLayout
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { getCurrentMember } from '@/api/auth'
import { disconnectCouple, getCoupleProfileMessage, updateCoupleProfileMessage } from '@/api/couple'
import { getPersonalSurveyResult } from '@/api/personalSurvey'
import { investmentTypeMeta } from '@/constants/investmentTypeMeta'
import { useAuthStore } from '@/stores/auth'

import {
  UserRound,
  ClipboardList,
  ClipboardPenLine,
  RefreshCw,
  HeartCrack,
  HeartHandshake,
  LogOut,
  UserRoundX,
  Pencil,
  ChevronRight,
  X,
  LoaderCircle,

  // 금융자산 갱신 모달
  CircleCheckBig,
  WalletCards,
  Info,

  // 탈퇴하기
  TriangleAlert,
} from 'lucide-vue-next'

const router = useRouter()
const queryClient = useQueryClient()
const authStore = useAuthStore()

// 실제 화면에 표시되는 소개 문구. 기본 문구는 백엔드 응답을 기준으로 한다.
const profileMessage = ref('')

// 모달 입력창에서 임시로 사용하는 값
const editMessage = ref('')

// 모달 열림 여부
const isEditModalOpen = ref(false)
const profileMessageError = ref('')
const coupleProfileQueryKey = computed(() => ['coupleProfileMessage', authStore.member?.id ?? 'me'])

const {
  data: coupleProfileMessage,
  isError: isProfileMessageLoadError,
  error: coupleProfileMessageError,
} = useQuery({
  queryKey: coupleProfileQueryKey,
  queryFn: getCoupleProfileMessage,
})

const { data: currentMember } = useQuery({
  queryKey: ['currentMember'],
  queryFn: getCurrentMember,
})

const hasCoupleProfile = computed(() => {
  const profile = coupleProfileMessage.value
  return Boolean(profile?.myName && profile?.partnerName)
})

const shouldShowProfileMessageLoadError = computed(
  () =>
    isProfileMessageLoadError.value &&
    coupleProfileMessageError.value?.code !== 'COUPLE_NOT_CONNECTED',
)

const coupleDisplayName = computed(() => {
  const profile = coupleProfileMessage.value
  if (hasCoupleProfile.value) {
    return `${profile.myName} ♡ ${profile.partnerName}`
  }

  return currentMember.value?.name ?? authStore.member?.name ?? '내 프로필'
})

watch(
  coupleProfileMessage,
  (response) => {
    profileMessage.value = response?.profileMessage ?? ''
  },
  { immediate: true },
)

const updateProfileMessageMutation = useMutation({
  mutationFn: updateCoupleProfileMessage,
  onSuccess: (response) => {
    queryClient.setQueryData(coupleProfileQueryKey.value, response)
    profileMessage.value = response.profileMessage
    isEditModalOpen.value = false
    profileMessageError.value = ''
  },
  onError: (error) => {
    profileMessageError.value = error.message || '한 줄 소개를 저장하지 못했어요.'
  },
})

const isProfileMessageSaving = computed(() => updateProfileMessageMutation.isPending.value)

// 모달 열기
const openEditModal = () => {
  editMessage.value = profileMessage.value
  profileMessageError.value = ''
  isEditModalOpen.value = true
}

// 취소
const closeEditModal = () => {
  if (isProfileMessageSaving.value) return
  isEditModalOpen.value = false
}

// 저장
const saveProfileMessage = () => {
  const message = editMessage.value.trim()

  if (!message) {
    profileMessageError.value = '한 줄 소개를 입력해 주세요.'
    return
  }

  updateProfileMessageMutation.mutate(message)
}

// ========================================
// 개인 투자 성향 모달
// ========================================

const isInvestmentModalOpen = ref(false)
const personalSurveyResultQueryKey = computed(() => [
  'personal-survey-result',
  authStore.member?.id ?? 'me',
])

const {
  data: personalSurveyResult,
  isLoading: isPersonalSurveyResultLoading,
  isError: isPersonalSurveyResultError,
  refetch: refetchPersonalSurveyResult,
} = useQuery({
  queryKey: personalSurveyResultQueryKey,
  queryFn: getPersonalSurveyResult,
  enabled: isInvestmentModalOpen,
})

const personalSurveyResultMeta = computed(() => {
  if (!personalSurveyResult.value) return null

  return investmentTypeMeta[personalSurveyResult.value.investmentType] ?? null
})

const openInvestmentModal = () => {
  isInvestmentModalOpen.value = true
}

const closeInvestmentModal = () => {
  isInvestmentModalOpen.value = false
}

// ========================================
// 금융자산 갱신 모달
// ========================================

// null    : 닫힘
// confirm : 갱신 확인
// loading : 갱신 중
// done    : 갱신 완료
const assetRefreshStep = ref(null)

const refreshProgress = ref(0)

// 금융자산 갱신 배너 클릭
const openAssetRefreshModal = () => {
  assetRefreshStep.value = 'confirm'
}

// 모달 닫기
const closeAssetRefreshModal = () => {
  assetRefreshStep.value = null
  refreshProgress.value = 0
}

// 갱신 시작
const startAssetRefresh = () => {
  assetRefreshStep.value = 'loading'
  refreshProgress.value = 60

  // 지금은 백엔드가 없으므로 화면 확인용 가짜 로딩
  setTimeout(() => {
    refreshProgress.value = 100

    setTimeout(() => {
      assetRefreshStep.value = 'done'
    }, 400)
  }, 1200)
}

// ========================================
// 설문 갱신 바텀시트
// ========================================

const isSurveyRefreshSheetOpen = ref(false)
const surveyRefreshTarget = ref(null)

const surveyRefreshOptions = {
  personal: {
    title: '개인 설문 갱신하기',
    description: '나의 금융 스타일을 다시 확인해요',
    confirmTitle: '개인 설문을 갱신하시겠어요?',
    confirmDescription: '기존 개인 설문 결과를 새 응답 기준으로 업데이트합니다.',
  },
  couple: {
    title: '공동 설문 갱신하기',
    description: '우리의 목표와 금융 기준을 다시 맞춰요',
    confirmTitle: '공동 설문을 갱신하시겠어요?',
    confirmDescription: '기존 공동 설문 결과를 새 응답 기준으로 업데이트합니다.',
  },
}

const personalSurveyRefreshQuestions = [
  {
    key: 'annualIncome',
    type: 'money',
    title: '최근 1년간 세전 총소득은 얼마인가요?',
  },
  {
    key: 'monthlyAvailableAmount',
    type: 'money',
    title: '매달 무리 없이 저축하거나 투자할 수 있는 금액은 얼마인가요?',
  },
  {
    key: 'financialAssetRatio',
    type: 'single',
    title: '총 자산 중 금융자산이 차지하는 비중은 어느 정도인가요?',
    options: [
      { value: 'UNDER_10', label: '10% 이내' },
      { value: 'UNDER_30', label: '30% 이내' },
      { value: 'UNDER_50', label: '50% 이내' },
      { value: 'UNDER_80', label: '80% 이내' },
      { value: 'OVER_80', label: '80% 초과' },
    ],
  },
  {
    key: 'investmentExperiences',
    type: 'multi',
    title: '지금까지 거래하거나 가입해 본 금융상품을 모두 선택해 주세요.',
    options: [
      { value: 'LOW_RISK', label: '은행 예적금, 국채, 지방채, MMF, CMA 등' },
      { value: 'MODERATE_LOW_RISK', label: '채권형 펀드, 금융채, 원금보장형 ELS/ELF 등' },
      { value: 'MODERATE_RISK', label: '혼합형 펀드, 중간 등급 회사채, 일부 보장 ELS/ELF 등' },
      {
        value: 'MODERATE_HIGH_RISK',
        label: '인덱스 주식형 펀드, 저신용 회사채, 비보장 ELS/ELF 등',
      },
      { value: 'HIGH_RISK', label: '주식형 펀드, 파생상품 펀드, 주식, 선물·옵션 등' },
    ],
  },
  {
    key: 'financialKnowledge',
    type: 'single',
    title: '금융투자상품에 대한 이해도는 어느 정도인가요?',
    options: [
      { value: 'VERY_LOW', label: '매우 낮음 - 금융상품 중 예·적금에 대해서만 알고 있음' },
      { value: 'LOW', label: '낮음 - 주식, 채권, 펀드의 차이를 구별할 수 있음' },
      {
        value: 'MEDIUM',
        label: '보통 - 주식, 채권, 펀드의 기본적인 특징과 손실 가능성을 대략 이해하고 있음',
      },
      {
        value: 'HIGH',
        label: '높음 - 주식, 채권, 펀드 등의 구조 및 위험을 깊이 있게 이해하고 있음',
      },
      {
        value: 'VERY_HIGH',
        label: '매우 높음 - 파생상품을 포함한 대부분의 금융투자상품의 구조 및 위험을 이해하고 있음',
      },
    ],
  },
  {
    key: 'capitalPreservationAttitude',
    type: 'single',
    title: '투자한다고 가정했을 때 원금 보존 태도는 무엇인가요?',
    options: [
      { value: 'ZERO', label: '원금 보존 추구' },
      { value: 'UNDER_10', label: '10% 이내 손실 감내 가능' },
      { value: 'UNDER_20', label: '20% 이내 손실 감내 가능' },
      { value: 'UNDER_50', label: '50% 이내 손실 감내 가능' },
      { value: 'UNDER_70', label: '70% 이내 손실 감내 가능' },
      { value: 'FULL', label: '전액손실 감내 가능' },
    ],
  },
]

const coupleSurveyRefreshQuestions = [
  {
    key: 'goals',
    type: 'rank-two',
    title: '두 분이 우선으로 생각하는 금융 목표 2개를 골라 주세요.',
    options: [
      { value: 'INVESTMENT', label: '여유 자금 투자' },
      { value: 'RETIREMENT', label: '노후 자금 마련' },
      { value: 'MARRIAGE', label: '결혼 자금 마련' },
      { value: 'HOUSING', label: '부동산 자금 마련' },
      { value: 'SHORT_TERM', label: '사용예정자금 단기운용' },
    ],
  },
  {
    key: 'targetAmount',
    type: 'money',
    title: '1순위 공동 목표를 위해 필요한 금액은 얼마인가요?',
  },
  {
    key: 'targetPeriodMonths',
    type: 'number',
    title: '1순위 공동 목표를 몇 개월 안에 이루고 싶으신가요?',
    unit: '개월',
  },
  {
    key: 'loanPurpose',
    type: 'single',
    title: '앞으로 대출을 받는다면 주된 목적은 무엇인가요?',
    options: [
      { value: 'NONE', label: '현재 대출 계획 없음' },
      { value: 'JEONSE', label: '전세 자금 마련' },
      { value: 'HOUSING', label: '주택 구입 자금 마련' },
      { value: 'CAR', label: '자동차 구입 자금 마련' },
      { value: 'BUSINESS', label: '사업 또는 창업 자금 마련' },
    ],
  },
  {
    key: 'hasLoanWithinOneMonth',
    type: 'boolean',
    title: '최근 1개월 이내 대출을 받았거나, 앞으로 1개월 이내 받을 예정인가요?',
    options: [
      { value: true, label: '예' },
      { value: false, label: '아니요' },
    ],
  },
]

const selectedSurveyRefreshOption = computed(() =>
  surveyRefreshTarget.value ? surveyRefreshOptions[surveyRefreshTarget.value] : null,
)

const activeSurveyRefreshTarget = ref(null)
const surveyRefreshAnswers = ref({})
const surveyRefreshFormError = ref('')

const activeSurveyRefreshOption = computed(() =>
  activeSurveyRefreshTarget.value ? surveyRefreshOptions[activeSurveyRefreshTarget.value] : null,
)

const activeSurveyRefreshQuestions = computed(() =>
  activeSurveyRefreshTarget.value === 'couple'
    ? coupleSurveyRefreshQuestions
    : personalSurveyRefreshQuestions,
)

const openSurveyRefreshSheet = () => {
  isSurveyRefreshSheetOpen.value = true
}

const closeSurveyRefreshSheet = () => {
  isSurveyRefreshSheetOpen.value = false
}

const openSurveyRefreshConfirm = (target) => {
  isSurveyRefreshSheetOpen.value = false
  surveyRefreshTarget.value = target
}

const closeSurveyRefreshConfirm = () => {
  surveyRefreshTarget.value = null
}

const confirmSurveyRefresh = () => {
  if (!selectedSurveyRefreshOption.value) return
  activeSurveyRefreshTarget.value = surveyRefreshTarget.value
  surveyRefreshAnswers.value = {}
  surveyRefreshFormError.value = ''
  surveyRefreshTarget.value = null
}

const closeSurveyRefreshForm = () => {
  activeSurveyRefreshTarget.value = null
  surveyRefreshAnswers.value = {}
  surveyRefreshFormError.value = ''
}

const updateSurveyRefreshAnswer = (key, value) => {
  surveyRefreshAnswers.value = {
    ...surveyRefreshAnswers.value,
    [key]: value,
  }
  surveyRefreshFormError.value = ''
}

const formatSurveyRefreshNumber = (value) => {
  if (!value) return ''
  return Number(value).toLocaleString('ko-KR')
}

const formatKoreanUnitNumber = (value) => {
  if (value < 1000) return Number(value).toLocaleString('ko-KR')

  const thousand = Math.floor(value / 1000)
  const hundred = Math.floor((value % 1000) / 100)
  const ten = Math.floor((value % 100) / 10)
  const one = value % 10
  const parts = []

  if (thousand > 0) parts.push(`${thousand}천`)
  if (hundred > 0) parts.push(`${hundred}백`)
  if (ten > 0) parts.push(`${ten}십`)
  if (one > 0) parts.push(`${one}`)

  return parts.join('')
}

const formatSurveyRefreshKoreanAmount = (value) => {
  if (!value) return '금액 입력'

  const amount = Number(value)
  if (!Number.isFinite(amount) || amount < 0) return '금액 입력'
  if (amount === 0) return '0원'

  const jo = Math.floor(amount / 1_000_000_000_000)
  const eok = Math.floor((amount % 1_000_000_000_000) / 100_000_000)
  const man = Math.floor((amount % 100_000_000) / 10_000)
  const won = amount % 10_000
  const parts = []

  if (jo > 0) parts.push(`${formatKoreanUnitNumber(jo)}조`)
  if (eok > 0) parts.push(`${formatKoreanUnitNumber(eok)}억`)
  if (man > 0) parts.push(`${formatKoreanUnitNumber(man)}만`)
  if (won > 0) parts.push(parts.length > 0 ? `${formatSurveyRefreshNumber(won)}원` : `${won}`)

  return parts.length > 1 && won > 0 ? parts.join(' ') : `${parts.join('')}원`
}

const getSurveyRefreshInputValue = (question) => {
  const value = surveyRefreshAnswers.value[question.key] ?? ''
  return formatSurveyRefreshNumber(value)
}

const updateSurveyRefreshMoney = (key, event) => {
  updateSurveyRefreshAnswer(key, event.target.value.replace(/\D/g, '').slice(0, 15))
}

const toggleSurveyRefreshAnswer = (key, value) => {
  const currentValue = surveyRefreshAnswers.value[key] ?? []
  updateSurveyRefreshAnswer(
    key,
    currentValue.includes(value)
      ? currentValue.filter((selectedValue) => selectedValue !== value)
      : [...currentValue, value],
  )
}

const toggleSurveyRefreshGoal = (value) => {
  const currentValue = surveyRefreshAnswers.value.goals ?? []

  if (currentValue.includes(value)) {
    updateSurveyRefreshAnswer(
      'goals',
      currentValue.filter((selectedValue) => selectedValue !== value),
    )
    return
  }

  updateSurveyRefreshAnswer('goals', [...currentValue.slice(-1), value])
}

const getSurveyRefreshGoalRank = (value) => {
  const selectedGoals = surveyRefreshAnswers.value.goals ?? []
  const index = selectedGoals.indexOf(value)
  return index >= 0 ? `${index + 1}순위` : ''
}

const isSurveyRefreshQuestionAnswered = (question) => {
  const value = surveyRefreshAnswers.value[question.key]

  if (question.type === 'multi') return Array.isArray(value) && value.length > 0
  if (question.type === 'rank-two') return Array.isArray(value) && value.length === 2
  if (question.type === 'boolean') return value !== undefined
  return Boolean(value)
}

const submitSurveyRefreshForm = () => {
  const isComplete = activeSurveyRefreshQuestions.value.every(isSurveyRefreshQuestionAnswered)
  if (!isComplete) {
    surveyRefreshFormError.value = '설문 문항에 모두 답변해 주세요.'
    return
  }

  window.alert('설문 갱신이 완료되었습니다.')
  closeSurveyRefreshForm()
}

// ========================================
// 커플 연결 끊기 바텀시트
// ========================================

// 0 = 닫힘
// 1~3 = 확인 단계
const disconnectStep = ref(0)
const disconnectError = ref('')

const disconnectCoupleMutation = useMutation({
  mutationFn: disconnectCouple,
  onSuccess: () => {
    queryClient.removeQueries({ queryKey: ['coupleProfileMessage'] })
    disconnectStep.value = 0
    disconnectError.value = ''
    router.push({ name: 'couple-start' })
  },
  onError: (error) => {
    disconnectError.value = error.message || '커플 연결을 끊지 못했어요.'
  },
})

const isDisconnectingCouple = computed(() => disconnectCoupleMutation.isPending.value)

const openDisconnectSheet = () => {
  disconnectError.value = ''
  disconnectStep.value = 1
}

const closeDisconnectSheet = () => {
  if (isDisconnectingCouple.value) return
  disconnectStep.value = 0
  disconnectError.value = ''
}

const nextDisconnectStep = () => {
  if (disconnectStep.value < 3) {
    disconnectError.value = ''
    disconnectStep.value++
  }
}

const finishDisconnect = () => {
  disconnectCoupleMutation.mutate()
}

// ========================================
// 로그아웃 모달
// ========================================

const isLogoutModalOpen = ref(false)
const isLoggingOut = ref(false)

const openLogoutModal = () => {
  isLogoutModalOpen.value = true
}

const closeLogoutModal = () => {
  if (isLoggingOut.value) return
  isLogoutModalOpen.value = false
}

const confirmLogout = async () => {
  if (isLoggingOut.value) return

  isLoggingOut.value = true

  try {
    await authStore.logout()
  } catch {
    // 서버 호출 실패와 관계없이 현재 브라우저 세션은 로그아웃 상태로 정리한다.
  } finally {
    queryClient.clear()
    isLogoutModalOpen.value = false
    router.replace({ name: 'login', query: { logout: 'success' } })
    isLoggingOut.value = false
  }
}

// ========================================
// 회원 탈퇴 바텀시트
// ========================================

const WITHDRAW_CONFIRMATION_TEXT = '회원 탈퇴'
const isWithdrawSheetOpen = ref(false)
const isWithdrawing = ref(false)
const withdrawPassword = ref('')
const withdrawConfirmationText = ref('')
const withdrawPasswordError = ref('')
const withdrawConfirmationError = ref('')
const withdrawGeneralError = ref('')

const resetWithdrawState = () => {
  withdrawPassword.value = ''
  withdrawConfirmationText.value = ''
  withdrawPasswordError.value = ''
  withdrawConfirmationError.value = ''
  withdrawGeneralError.value = ''
}

const openWithdrawSheet = () => {
  resetWithdrawState()
  isWithdrawSheetOpen.value = true
}

const closeWithdrawSheet = () => {
  if (isWithdrawing.value) return
  resetWithdrawState()
  isWithdrawSheetOpen.value = false
}

const moveToOnboardingAfterWithdraw = (query = {}) => {
  queryClient.clear()
  isWithdrawSheetOpen.value = false
  router.replace({ name: 'onboarding', query })
}

const confirmWithdraw = async () => {
  if (isWithdrawing.value) return

  withdrawPasswordError.value = ''
  withdrawConfirmationError.value = ''
  withdrawGeneralError.value = ''

  const password = withdrawPassword.value
  const confirmationText = withdrawConfirmationText.value.trim()
  let hasInputError = false

  if (!password) {
    withdrawPasswordError.value = '비밀번호를 입력해 주세요.'
    hasInputError = true
  }

  if (!confirmationText) {
    withdrawConfirmationError.value = '확인 문구를 입력해 주세요.'
    hasInputError = true
  }

  if (hasInputError) return

  isWithdrawing.value = true

  try {
    await authStore.withdrawAccount({ password, confirmationText })
    resetWithdrawState()
    moveToOnboardingAfterWithdraw({ withdraw: 'success' })
  } catch (error) {
    if (error?.code === 'INVALID_PASSWORD') {
      withdrawPasswordError.value = error.message || '비밀번호가 일치하지 않습니다.'
      return
    }

    if (error?.code === 'INVALID_CONFIRMATION') {
      withdrawConfirmationError.value = error.message || '확인 문구가 일치하지 않습니다.'
      return
    }

    if (
      error?.status === 401 ||
      error?.code === 'UNAUTHORIZED' ||
      error?.code === 'MEMBER_ALREADY_WITHDRAWN' ||
      error?.code === 'MEMBER_NOT_FOUND'
    ) {
      moveToOnboardingAfterWithdraw()
      return
    }

    withdrawGeneralError.value =
      error?.message || '회원 탈퇴에 실패했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    isWithdrawing.value = false
  }
}
</script>

<template>
  <!--    <section class="p-4">-->
  <!--      &lt;!&ndash; TODO: 마이페이지 화면 UI &ndash;&gt;-->
  <!--      <h1 class="text-lg font-bold text-gray-900">마이페이지</h1>-->
  <!--      <p class="mt-2 text-sm text-gray-400">준비 중 — 담당자가 채웁니다.</p>-->
  <!--    </section>-->
  <div class="px-4 pt-3 pb-8">
    <!-- 프로필 -->
    <section
      class="flex items-center rounded-xl border border-gray-200 bg-white px-4 py-3 shadow-sm"
    >
      <!-- 프로필 아이콘 -->
      <div
        class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full border-[3px] border-yellow-400 bg-yellow-50"
      >
        <UserRound :size="24" :stroke-width="2.5" class="text-[#6f6900]" />
      </div>

      <!-- 사용자 정보 -->
      <div class="ml-3 flex-1">
        <div class="text-[20px] leading-tight font-bold text-gray-900">{{ coupleDisplayName }}</div>

        <div
          v-if="profileMessage || shouldShowProfileMessageLoadError || hasCoupleProfile"
          class="mt-1 flex items-center text-[13px] text-gray-500"
        >
          <span v-if="profileMessage">{{ profileMessage }}</span>
          <span v-if="shouldShowProfileMessageLoadError" class="ml-1 text-red-400">
            저장된 소개를 불러오지 못했어요
          </span>

          <button
            v-if="hasCoupleProfile"
            type="button"
            class="ml-1 flex h-7 w-7 cursor-pointer items-center justify-center rounded-full text-gray-500 transition hover:bg-gray-100"
            @click="openEditModal"
          >
            <Pencil :size="16" :stroke-width="2" />
          </button>
        </div>
      </div>
    </section>

    <!-- 내 정보 관리 -->
    <section class="mt-3">
      <h2 class="mb-2 text-[13px] font-medium text-gray-500">내 정보 관리</h2>

      <div
        class="flex cursor-pointer items-center rounded-xl border border-gray-200 bg-white px-3 py-3 shadow-sm transition hover:bg-gray-50"
        @click="openInvestmentModal"
      >
        <!-- 아이콘 -->
        <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-gray-100">
          <ClipboardList :size="20" :stroke-width="1.8" class="text-[#7c7500]" />
        </div>

        <!-- 텍스트 -->
        <div class="ml-3 flex-1">
          <p class="text-[18px] font-bold text-gray-900">나의 금융 스타일</p>

          <p class="mt-0.5 text-[13px] text-gray-500">나의 금융 스타일 결과 확인</p>
        </div>

        <!-- 오른쪽 화살표 -->
        <ChevronRight :size="18" :stroke-width="1.8" class="text-[#b8b18a]" />
      </div>
    </section>

    <!-- 데이터 관리 -->
    <section class="mt-3">
      <h2 class="mb-2 text-[13px] font-medium text-gray-500">데이터 관리</h2>

      <div
        class="flex cursor-pointer items-center rounded-xl border border-gray-200 bg-white px-3 py-3 shadow-sm transition hover:bg-gray-50"
        @click="openAssetRefreshModal"
      >
        <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-gray-100">
          <RefreshCw :size="20" :stroke-width="1.8" class="text-[#7c7500]" />
        </div>

        <div class="ml-3 flex-1">
          <p class="text-[18px] font-bold text-gray-900">금융자산 갱신하기</p>

          <p class="mt-0.5 text-[13px] text-gray-500">연결된 금융자산 정보를 최신으로 업데이트</p>
        </div>

        <ChevronRight :size="18" :stroke-width="1.8" class="text-[#b8b18a]" />
      </div>

      <div
        class="mt-2 flex cursor-pointer items-center rounded-xl border border-gray-200 bg-white px-3 py-3 shadow-sm transition hover:bg-gray-50"
        @click="openSurveyRefreshSheet"
      >
        <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-gray-100">
          <ClipboardPenLine :size="20" :stroke-width="1.8" class="text-[#7c7500]" />
        </div>

        <div class="ml-3 flex-1">
          <p class="text-[18px] font-bold text-gray-900">설문 갱신하기</p>

          <p class="mt-0.5 text-[13px] text-gray-500">설문 정보를 최신으로 업데이트</p>
        </div>

        <ChevronRight :size="18" :stroke-width="1.8" class="text-[#b8b18a]" />
      </div>
    </section>

    <!-- 커플 관리 -->
    <section class="mt-3">
      <h2 class="mb-2 text-[13px] font-medium text-gray-500">커플 관리</h2>

      <div
        class="flex cursor-pointer items-center rounded-xl border border-gray-200 bg-white px-3 py-3 shadow-sm transition hover:bg-gray-50"
        @click="openDisconnectSheet"
      >
        <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-gray-100">
          <HeartCrack :size="20" :stroke-width="1.8" class="text-[#7c7500]" />
        </div>

        <div class="ml-3 flex-1">
          <p class="text-[18px] font-bold text-gray-900">커플 연결 끊기</p>

          <p class="mt-0.5 text-[13px] text-gray-500">현재 커플 관계를 해제합니다</p>
        </div>

        <ChevronRight :size="18" :stroke-width="1.8" class="text-[#b8b18a]" />
      </div>
    </section>

    <!-- 기타 -->
    <section class="mt-3">
      <h2 class="mb-2 text-[13px] font-medium text-gray-500">기타</h2>

      <div class="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
        <!-- 로그아웃 -->
        <div
          class="flex cursor-pointer items-center px-3 py-4 transition hover:bg-gray-50"
          @click="openLogoutModal"
        >
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-gray-100">
            <LogOut :size="20" :stroke-width="1.8" class="text-[#7c7500]" />
          </div>

          <p class="ml-3 flex-1 text-[18px] font-bold text-gray-800">로그아웃</p>

          <ChevronRight :size="18" :stroke-width="1.8" class="text-[#b8b18a]" />
        </div>

        <!-- 구분선 -->
        <div class="mx-3 border-t border-gray-100"></div>

        <!-- 탈퇴 -->
        <div
          class="flex cursor-pointer items-center px-3 py-4 transition hover:bg-gray-50"
          @click="openWithdrawSheet"
        >
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-red-50">
            <UserRoundX :size="20" :stroke-width="1.8" class="text-red-500" />
          </div>

          <p class="ml-3 flex-1 text-[18px] font-bold text-red-500">탈퇴하기</p>

          <ChevronRight :size="18" :stroke-width="1.8" class="text-[#b8b18a]" />
        </div>
      </div>
    </section>
  </div>
  <!-- 한 줄 소개 수정 모달 -->
  <div
    v-if="isEditModalOpen"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
  >
    <div class="w-full max-w-[400px] rounded-2xl bg-white px-5 pb-5 pt-4 shadow-xl">
      <!-- 상단 -->
      <div class="relative flex items-center justify-center">
        <h2 class="text-[15px] font-bold text-gray-900">한 줄 소개 수정</h2>

        <button
          type="button"
          class="absolute right-0 flex h-7 w-7 items-center justify-center text-gray-400"
          @click="closeEditModal"
        >
          <X :size="20" :stroke-width="1.8" />
        </button>
      </div>

      <!-- 안내 문구 -->
      <p class="mt-4 text-center text-[13px] text-gray-500">나를 소개하는 한 줄을 입력해주세요.</p>

      <!-- 입력 영역 -->
      <div class="relative mt-4 rounded-xl border border-gray-200 bg-white">
        <textarea
          v-model="editMessage"
          maxlength="50"
          rows="3"
          placeholder="한 줄 소개를 입력해주세요."
          :disabled="isProfileMessageSaving"
          class="h-[86px] w-full resize-none rounded-xl bg-transparent px-3 py-3 text-[18px] font-medium text-gray-800 outline-none placeholder:text-gray-300"
        ></textarea>

        <!-- 글자수 -->
        <span class="absolute bottom-2 right-3 text-[9px] text-gray-300">
          {{ editMessage.length }}/50
        </span>
      </div>

      <p v-if="profileMessageError" class="mt-2 text-center text-[13px] text-red-500">
        {{ profileMessageError }}
      </p>

      <!-- 버튼 -->
      <div class="mt-5 flex gap-2">
        <!-- 취소 -->
        <button
          type="button"
          class="h-11 flex-1 rounded-xl bg-gray-100 text-[18px] font-semibold text-gray-600"
          :disabled="isProfileMessageSaving"
          @click="closeEditModal"
        >
          취소
        </button>

        <!-- 저장 -->
        <button
          type="button"
          class="h-11 flex-1 rounded-xl bg-yellow-300 text-[18px] font-bold text-gray-900 disabled:opacity-60"
          :disabled="isProfileMessageSaving"
          @click="saveProfileMessage"
        >
          {{ isProfileMessageSaving ? '저장 중' : '저장' }}
        </button>
      </div>
    </div>
  </div>
  <!-- ================================================= -->
  <!-- 개인 투자 성향 결과 모달 -->
  <!-- ================================================= -->
  <div
    v-if="isInvestmentModalOpen"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
    @click.self="closeInvestmentModal"
  >
    <div
      class="relative max-h-[88vh] w-full max-w-[400px] overflow-y-auto rounded-[24px] bg-white px-5 pb-6 pt-6 shadow-xl"
    >
      <!-- 닫기 -->
      <button
        type="button"
        class="absolute right-4 top-4 flex h-8 w-8 items-center justify-center text-gray-400"
        @click="closeInvestmentModal"
      >
        <X :size="21" :stroke-width="1.8" />
      </button>

      <h2 class="text-center text-[15px] font-bold text-gray-900">나의 금융 스타일 결과</h2>

      <div
        v-if="isPersonalSurveyResultLoading"
        class="flex min-h-[360px] items-center justify-center"
      >
        <LoaderCircle :size="28" class="animate-spin text-[#777000]" />
      </div>

      <div
        v-else-if="isPersonalSurveyResultError || !personalSurveyResultMeta"
        class="flex min-h-[360px] flex-col items-center justify-center px-4 text-center"
      >
        <p class="text-[16px] leading-7 font-medium text-red-500">
          금융 스타일 결과를 불러오지 못했어요.<br />
          잠시 후 다시 시도해 주세요.
        </p>

        <button
          type="button"
          class="mt-5 rounded-full border border-gray-200 bg-white px-6 py-3 text-[18px] font-semibold text-gray-900"
          @click="refetchPersonalSurveyResult"
        >
          다시 시도하기
        </button>
      </div>

      <section v-else class="flex flex-col">
        <div class="mt-7 text-center">
          <p class="text-[18px] font-medium tracking-[-0.2px]">
            {{ personalSurveyResult.name }}님의 금융 스타일은
          </p>

          <p class="mt-2 flex flex-wrap items-baseline justify-center tracking-[-1px]">
            <strong
              class="text-[38px] leading-[1.2] font-extrabold"
              :class="personalSurveyResultMeta.accentClass"
            >
              {{ personalSurveyResult.investmentType }}
            </strong>
          </p>
        </div>

        <div class="relative mt-4 flex h-[190px] items-center justify-center">
          <div
            class="absolute h-[190px] w-[270px] rounded-full bg-[radial-gradient(circle,_rgba(255,244,79,0.32)_0%,_rgba(255,244,79,0.14)_50%,_transparent_74%)]"
            aria-hidden="true"
          ></div>

          <img
            :src="personalSurveyResultMeta.character"
            :alt="`${personalSurveyResult.investmentType} 캐릭터`"
            class="relative h-[165px] w-[165px] object-contain"
          />
        </div>

        <article
          class="mt-3 flex max-h-[255px] flex-col overflow-hidden rounded-xl border border-gray-200 bg-white px-4 py-4"
        >
          <div class="flex flex-none items-start gap-2">
            <component
              :is="personalSurveyResultMeta.icon"
              class="mt-0.5 h-5 w-5 flex-none text-[#777000]"
              :stroke-width="2"
              aria-hidden="true"
            />

            <h3 class="text-[17px] leading-[1.4] font-bold text-gray-800">
              {{ personalSurveyResult.headline }}
            </h3>
          </div>

          <div
            class="mt-4 min-h-0 flex-1 overflow-y-auto pr-2 text-[16px] leading-[1.8] text-gray-600"
          >
            <p>{{ personalSurveyResult.description }}</p>
          </div>
        </article>
      </section>
    </div>
  </div>

  <!-- ======================================== -->
  <!-- 설문 갱신 바텀시트 -->
  <!-- ======================================== -->
  <div
    v-if="isSurveyRefreshSheetOpen"
    class="fixed inset-0 z-[100] bg-black/40"
    @click.self="closeSurveyRefreshSheet"
  >
    <div
      class="absolute bottom-0 left-1/2 w-full max-w-[430px] -translate-x-1/2 rounded-t-[28px] bg-white px-5 pb-7 pt-3 shadow-2xl"
    >
      <div class="mx-auto h-1.5 w-12 rounded-full bg-gray-200"></div>

      <div class="mt-5 flex items-start justify-between">
        <div>
          <h2 class="text-[22px] font-bold text-gray-900">설문 갱신하기</h2>

          <p class="mt-2 text-[12px] text-gray-500">갱신할 설문을 선택해 주세요.</p>
        </div>

        <button
          type="button"
          class="flex h-8 w-8 cursor-pointer items-center justify-center text-gray-600"
          @click="closeSurveyRefreshSheet"
        >
          <X :size="24" :stroke-width="2" />
        </button>
      </div>

      <div class="mt-6 space-y-3">
        <button
          type="button"
          class="flex w-full cursor-pointer items-center rounded-xl border border-gray-200 bg-white px-4 py-4 text-left transition hover:bg-gray-50"
          @click="openSurveyRefreshConfirm('personal')"
        >
          <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-yellow-50">
            <ClipboardList :size="22" :stroke-width="1.9" class="text-[#7c7500]" />
          </div>

          <div class="ml-3 min-w-0 flex-1">
            <p class="text-[17px] font-bold text-gray-900">개인 설문 갱신하기</p>

            <p class="mt-1 text-[13px] leading-[1.5] text-gray-500">
              나의 금융 스타일을 다시 확인해요
            </p>
          </div>

          <ChevronRight :size="18" :stroke-width="1.8" class="shrink-0 text-[#b8b18a]" />
        </button>

        <button
          type="button"
          class="flex w-full cursor-pointer items-center rounded-xl border border-gray-200 bg-white px-4 py-4 text-left transition hover:bg-gray-50"
          @click="openSurveyRefreshConfirm('couple')"
        >
          <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-pink-50">
            <HeartHandshake :size="22" :stroke-width="1.9" class="text-pink-500" />
          </div>

          <div class="ml-3 min-w-0 flex-1">
            <p class="text-[17px] font-bold text-gray-900">공동 설문 갱신하기</p>

            <p class="mt-1 text-[13px] leading-[1.5] text-gray-500">
              우리의 목표와 금융 기준을 다시 맞춰요
            </p>
          </div>

          <ChevronRight :size="18" :stroke-width="1.8" class="shrink-0 text-[#b8b18a]" />
        </button>
      </div>

      <button
        type="button"
        class="mt-4 h-12 w-full cursor-pointer rounded-xl bg-gray-100 text-[18px] font-bold text-gray-600 transition hover:bg-gray-200"
        @click="closeSurveyRefreshSheet"
      >
        취소
      </button>
    </div>
  </div>

  <!-- ======================================== -->
  <!-- 설문 갱신 확인 -->
  <!-- ======================================== -->
  <div
    v-if="selectedSurveyRefreshOption"
    class="fixed inset-0 z-[110] flex items-center justify-center bg-black/40 px-5"
    @click.self="closeSurveyRefreshConfirm"
  >
    <div class="w-full max-w-[350px] rounded-[24px] bg-white px-6 pb-6 pt-8 shadow-xl">
      <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-yellow-50">
        <ClipboardPenLine :size="28" :stroke-width="2.1" class="text-[#7c7500]" />
      </div>

      <h2 class="mt-6 text-center text-[17px] font-bold text-gray-900">
        {{ selectedSurveyRefreshOption.confirmTitle }}
      </h2>

      <p class="mt-3 text-center text-[12px] leading-[1.7] text-gray-400">
        {{ selectedSurveyRefreshOption.confirmDescription }}<br />
        설문 입력창을 엽니다.
      </p>

      <div class="mt-7 flex gap-3">
        <button
          type="button"
          class="h-12 flex-1 cursor-pointer rounded-xl bg-gray-100 text-[18px] font-bold text-gray-600 transition hover:bg-gray-200"
          @click="closeSurveyRefreshConfirm"
        >
          취소
        </button>

        <button
          type="button"
          class="h-12 flex-1 cursor-pointer rounded-xl bg-yellow-300 text-[18px] font-bold text-gray-900 transition hover:bg-yellow-400"
          @click="confirmSurveyRefresh"
        >
          갱신하기
        </button>
      </div>
    </div>
  </div>

  <!-- ======================================== -->
  <!-- 설문 갱신 입력 모달 -->
  <!-- ======================================== -->
  <div
    v-if="activeSurveyRefreshOption"
    class="fixed inset-0 z-[110] flex items-center justify-center bg-black/40 px-4"
    @click.self="closeSurveyRefreshForm"
  >
    <div
      class="relative flex max-h-[88vh] w-full max-w-[400px] flex-col rounded-[24px] bg-white shadow-xl"
    >
      <div class="shrink-0 border-b border-gray-100 px-5 pb-4 pt-5">
        <button
          type="button"
          class="absolute right-4 top-4 flex h-8 w-8 cursor-pointer items-center justify-center text-gray-500"
          @click="closeSurveyRefreshForm"
        >
          <X :size="23" :stroke-width="2" />
        </button>

        <h2 class="pr-10 text-[20px] font-bold text-gray-900">
          {{ activeSurveyRefreshOption.title }}
        </h2>

        <p class="mt-2 text-[13px] leading-[1.5] text-gray-500">
          {{ activeSurveyRefreshOption.description }}
        </p>
      </div>

      <div class="min-h-0 flex-1 overflow-y-auto px-5 py-5">
        <div
          v-for="(question, index) in activeSurveyRefreshQuestions"
          :key="question.key"
          class="mb-7 last:mb-0"
        >
          <h3 class="text-[17px] leading-[1.5] font-bold text-gray-900">
            <span class="text-[#7c7500]">Q{{ index + 1 }}.</span>
            {{ question.title }}
          </h3>

          <div v-if="question.type === 'money' || question.type === 'number'" class="mt-3">
            <div
              class="flex h-12 items-center rounded-xl border border-gray-200 bg-white px-3 focus-within:border-[#7c7500]"
            >
              <input
                :value="getSurveyRefreshInputValue(question)"
                type="text"
                inputmode="numeric"
                class="min-w-0 flex-1 bg-transparent text-right text-[18px] font-semibold text-gray-900 outline-none"
                @input="updateSurveyRefreshMoney(question.key, $event)"
              />
              <span class="ml-3 text-[14px] font-medium text-gray-600">
                {{ question.unit ?? '원' }}
              </span>
            </div>
            <p v-if="question.type === 'money'" class="mt-1.5 text-right text-[13px] text-gray-500">
              {{ formatSurveyRefreshKoreanAmount(surveyRefreshAnswers[question.key] ?? '') }}
            </p>
          </div>

          <div v-else-if="question.type === 'single'" class="mt-3 space-y-2">
            <button
              v-for="option in question.options"
              :key="option.value"
              type="button"
              class="flex min-h-11 w-full cursor-pointer items-center justify-center rounded-xl border px-3 py-2 text-center text-[15px] leading-[1.4] font-semibold transition"
              :class="
                surveyRefreshAnswers[question.key] === option.value
                  ? 'border-[#7c7500] bg-yellow-50 text-gray-900'
                  : 'border-gray-200 bg-white text-gray-700'
              "
              @click="updateSurveyRefreshAnswer(question.key, option.value)"
            >
              {{ option.label }}
            </button>
          </div>

          <div v-else-if="question.type === 'multi'" class="mt-3 space-y-2">
            <button
              v-for="option in question.options"
              :key="option.value"
              type="button"
              class="flex min-h-11 w-full cursor-pointer items-center justify-center rounded-xl border px-3 py-2 text-center text-[15px] leading-[1.4] font-semibold transition"
              :class="
                (surveyRefreshAnswers[question.key] ?? []).includes(option.value)
                  ? 'border-[#7c7500] bg-yellow-50 text-gray-900'
                  : 'border-gray-200 bg-white text-gray-700'
              "
              @click="toggleSurveyRefreshAnswer(question.key, option.value)"
            >
              {{ option.label }}
            </button>
          </div>

          <div v-else-if="question.type === 'rank-two'" class="mt-3 space-y-2">
            <button
              v-for="option in question.options"
              :key="option.value"
              type="button"
              class="flex min-h-11 w-full cursor-pointer items-center justify-between rounded-xl border px-3 py-2 text-left text-[15px] leading-[1.4] font-semibold transition"
              :class="
                getSurveyRefreshGoalRank(option.value)
                  ? 'border-[#7c7500] bg-yellow-50 text-gray-900'
                  : 'border-gray-200 bg-white text-gray-700'
              "
              @click="toggleSurveyRefreshGoal(option.value)"
            >
              <span>{{ option.label }}</span>
              <span
                v-if="getSurveyRefreshGoalRank(option.value)"
                class="ml-3 shrink-0 rounded bg-white px-1.5 py-0.5 text-[12px] font-bold text-[#7c7500]"
              >
                {{ getSurveyRefreshGoalRank(option.value) }}
              </span>
            </button>
          </div>

          <div v-else-if="question.type === 'boolean'" class="mt-3 grid grid-cols-2 gap-2">
            <button
              v-for="option in question.options"
              :key="String(option.value)"
              type="button"
              class="h-11 cursor-pointer rounded-xl border text-[16px] font-bold transition"
              :class="
                surveyRefreshAnswers[question.key] === option.value
                  ? 'border-[#7c7500] bg-yellow-50 text-gray-900'
                  : 'border-gray-200 bg-white text-gray-700'
              "
              @click="updateSurveyRefreshAnswer(question.key, option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
      </div>

      <div class="shrink-0 border-t border-gray-100 px-5 pb-5 pt-4">
        <p
          v-if="surveyRefreshFormError"
          class="mb-3 text-center text-[13px] font-semibold text-red-500"
        >
          {{ surveyRefreshFormError }}
        </p>

        <button
          type="button"
          class="h-12 w-full cursor-pointer rounded-xl bg-yellow-300 text-[18px] font-bold text-gray-900 transition hover:bg-yellow-400"
          @click="submitSurveyRefreshForm"
        >
          갱신하기
        </button>
      </div>
    </div>
  </div>

  <!-- ======================================== -->
  <!-- 금융자산 갱신 확인 -->
  <!-- ======================================== -->
  <div
    v-if="assetRefreshStep === 'confirm'"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
    @click.self="closeAssetRefreshModal"
  >
    <div class="relative w-full max-w-[400px] rounded-[24px] bg-white px-6 pb-6 pt-9 shadow-xl">
      <!-- 닫기 -->
      <button
        type="button"
        class="absolute right-4 top-4 text-gray-700"
        @click="closeAssetRefreshModal"
      >
        <X :size="22" :stroke-width="2" />
      </button>

      <!-- 아이콘 -->
      <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-yellow-50">
        <RefreshCw :size="28" :stroke-width="2.2" class="text-yellow-500" />
      </div>

      <!-- 제목 -->
      <h2 class="mt-6 text-center text-[18px] font-bold leading-[1.4] text-gray-900">
        금융 자산을<br />
        갱신하시겠습니까?
      </h2>

      <!-- 설명 -->
      <p class="mt-3 text-center text-[12px] leading-[1.7] text-gray-500">
        연결된 계좌의 최신 정보를 불러와<br />
        자산 현황을 업데이트합니다.
      </p>

      <!-- 버튼 -->
      <div class="mt-7 flex gap-3">
        <button
          type="button"
          class="h-12 flex-1 rounded-xl bg-gray-100 text-[18px] font-bold text-gray-600"
          @click="closeAssetRefreshModal"
        >
          취소
        </button>

        <button
          type="button"
          class="h-12 flex-1 rounded-xl bg-yellow-300 text-[18px] font-bold text-gray-900"
          @click="startAssetRefresh"
        >
          갱신하기
        </button>
      </div>
    </div>
  </div>

  <!-- ======================================== -->
  <!-- 금융자산 갱신 중 -->
  <!-- ======================================== -->
  <div
    v-if="assetRefreshStep === 'loading'"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
  >
    <div class="relative w-full max-w-[400px] rounded-[24px] bg-white px-6 pb-8 pt-10 shadow-xl">
      <!-- 닫기 -->
      <button
        type="button"
        class="absolute right-4 top-4 text-gray-700"
        @click="closeAssetRefreshModal"
      >
        <X :size="22" :stroke-width="2" />
      </button>

      <!-- 로딩 아이콘 -->
      <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-yellow-50">
        <RefreshCw :size="28" :stroke-width="2.2" class="animate-spin text-[#69672d]" />
      </div>

      <!-- 제목 -->
      <h2 class="mt-7 text-center text-[18px] font-bold text-gray-900">
        금융 자산 정보를 불러오는 중이에요
      </h2>

      <p class="mt-2 text-center text-[12px] text-gray-500">잠시만 기다려주세요</p>

      <!-- 진행률 -->
      <div class="mt-8 flex items-center gap-4">
        <div class="h-[6px] flex-1 overflow-hidden rounded-full bg-gray-200">
          <div
            class="h-full rounded-full bg-yellow-300 transition-all duration-500"
            :style="{ width: `${refreshProgress}%` }"
          ></div>
        </div>

        <span class="w-8 text-right text-[13px] font-medium text-gray-500">
          {{ refreshProgress }}%
        </span>
      </div>
    </div>
  </div>

  <!-- ======================================== -->
  <!-- 금융자산 갱신 완료 -->
  <!-- ======================================== -->
  <div
    v-if="assetRefreshStep === 'done'"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
    @click.self="closeAssetRefreshModal"
  >
    <div class="relative w-full max-w-[400px] rounded-[24px] bg-white px-6 pb-7 pt-9 shadow-xl">
      <!-- 닫기 -->
      <button
        type="button"
        class="absolute right-4 top-4 text-gray-700"
        @click="closeAssetRefreshModal"
      >
        <X :size="22" :stroke-width="2" />
      </button>

      <!-- 완료 아이콘 -->
      <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-yellow-300">
        <CircleCheckBig :size="42" :stroke-width="2.2" class="text-[#6d6900]" />
      </div>

      <!-- 완료 문구 -->
      <h2 class="mt-5 text-center text-[17px] font-bold text-gray-900">자산 연동 완료!</h2>

      <p class="mt-2 text-center text-[12px] text-gray-500">은행·증권 전체를 한 번에 불러왔어요</p>

      <!-- 자산 카드 -->
      <div class="mt-5 rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
        <!-- 라벨 -->
        <div class="flex items-center justify-between">
          <span class="rounded-full bg-gray-100 px-3 py-1 text-[13px] text-gray-600">
            총 자산
          </span>

          <WalletCards :size="18" :stroke-width="2" class="text-yellow-500" />
        </div>

        <!-- 총 금액 -->
        <p class="mt-4 text-[27px] font-bold tracking-tight text-gray-900">₩84,200,000</p>

        <!-- 자산 비율 바 -->
        <div class="mt-4 flex h-[9px] overflow-hidden rounded-full">
          <div class="h-full bg-cyan-300" style="width: 60%"></div>

          <div class="h-full bg-yellow-300" style="width: 25%"></div>

          <div class="h-full bg-pink-300" style="width: 15%"></div>
        </div>

        <!-- 비율 -->
        <div class="mt-3 grid grid-cols-3 text-[13px]">
          <div>
            <div class="flex items-center gap-1 text-gray-500">
              <span class="h-2 w-2 rounded-full bg-cyan-300"></span>
              예적금
            </div>

            <p class="mt-1 font-bold text-gray-700">60%</p>
          </div>

          <div>
            <div class="flex items-center gap-1 text-gray-500">
              <span class="h-2 w-2 rounded-full bg-yellow-300"></span>
              투자
            </div>

            <p class="mt-1 font-bold text-gray-700">25%</p>
          </div>

          <div>
            <div class="flex items-center gap-1 text-gray-500">
              <span class="h-2 w-2 rounded-full bg-pink-300"></span>
              기타
            </div>

            <p class="mt-1 font-bold text-gray-700">15%</p>
          </div>
        </div>

        <!-- 안내 -->
        <div class="mt-5 flex items-start gap-2 rounded-xl bg-gray-50 px-3 py-3">
          <Info :size="15" :stroke-width="1.8" class="mt-[1px] shrink-0 text-gray-500" />

          <p class="text-[9px] leading-[1.6] text-gray-500">
            연동된 정보는 파트너와 함께 실시간으로 업데이트되며, 개인 식별 정보는 암호화되어
            안전하게 관리됩니다.
          </p>
        </div>
      </div>
    </div>
  </div>

  <!-- ======================================== -->
  <!-- 커플 연결 끊기 바텀시트 -->
  <!-- ======================================== -->
  <div
    v-if="disconnectStep > 0"
    class="fixed inset-0 z-[100] bg-black/40"
    @click.self="closeDisconnectSheet"
  >
    <div
      class="absolute bottom-0 left-1/2 w-full max-w-[430px] -translate-x-1/2 rounded-t-[28px] bg-white px-5 pb-6 pt-3 shadow-2xl"
    >
      <!-- 상단 손잡이 -->
      <div class="mx-auto h-1.5 w-12 rounded-full bg-gray-300"></div>

      <!-- ================================ -->
      <!-- 1단계 -->
      <!-- ================================ -->
      <template v-if="disconnectStep === 1">
        <!-- 아이콘 -->
        <div class="mx-auto mt-4 flex h-14 w-14 items-center justify-center rounded-full bg-red-50">
          <HeartCrack :size="30" :stroke-width="2" class="text-red-500" />
        </div>

        <h2 class="mt-5 text-center text-[17px] font-bold text-gray-900">
          커플 연결을 끊으시겠어요?
        </h2>

        <p class="mt-3 text-center text-[12px] leading-[1.7] text-gray-500">
          연결을 끊으면 함께 만든 리포트와<br />
          추천 결과가 삭제돼요.
        </p>

        <!-- 버튼 -->
        <div class="mt-8 flex gap-3">
          <button
            type="button"
            class="h-12 flex-1 cursor-pointer rounded-xl bg-gray-100 text-[18px] font-bold text-gray-600 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="isDisconnectingCouple"
            @click="closeDisconnectSheet"
          >
            취소
          </button>

          <button
            type="button"
            class="h-12 flex-1 cursor-pointer rounded-xl bg-yellow-300 text-[18px] font-bold text-gray-900 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="isDisconnectingCouple"
            @click="nextDisconnectStep"
          >
            다음
          </button>
        </div>

        <!-- 단계 -->
        <div class="mt-3 text-center text-[13px]">
          <span class="font-bold text-red-400">1</span>
          <span class="text-gray-300"> / 3</span>
        </div>
      </template>

      <!-- ================================ -->
      <!-- 2단계 -->
      <!-- ================================ -->
      <template v-if="disconnectStep === 2">
        <div class="mx-auto mt-4 flex h-14 w-14 items-center justify-center rounded-full bg-red-50">
          <HeartCrack :size="30" :stroke-width="2" class="text-red-500" />
        </div>

        <h2 class="mt-5 text-center text-[17px] font-bold text-gray-900">
          정말 연결을 끊으시겠어요?
        </h2>

        <p class="mt-3 text-center text-[12px] leading-[1.7] text-gray-500">
          이 작업은 되돌릴 수 없어요.<br />
          다시 연결하려면 공동 설문부터 진행해야 해요.
        </p>

        <div class="mt-8 flex gap-3">
          <button
            type="button"
            class="h-12 flex-1 cursor-pointer rounded-xl bg-gray-100 text-[18px] font-bold text-gray-600 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="isDisconnectingCouple"
            @click="closeDisconnectSheet"
          >
            취소
          </button>

          <button
            type="button"
            class="h-12 flex-1 cursor-pointer rounded-xl bg-yellow-300 text-[18px] font-bold text-gray-900 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="isDisconnectingCouple"
            @click="nextDisconnectStep"
          >
            다음
          </button>
        </div>

        <div class="mt-3 text-center text-[13px]">
          <span class="font-bold text-red-400">2</span>
          <span class="text-gray-300"> / 3</span>
        </div>
      </template>

      <!-- ================================ -->
      <!-- 3단계 -->
      <!-- ================================ -->
      <template v-if="disconnectStep === 3">
        <div class="mx-auto mt-4 flex h-14 w-14 items-center justify-center rounded-full bg-red-50">
          <HeartCrack :size="30" :stroke-width="2" class="text-red-500" />
        </div>

        <h2 class="mt-5 text-center text-[17px] font-bold text-gray-900">
          정말 마지막으로 확인할게요!
        </h2>

        <p class="mt-3 text-center text-[12px] font-semibold leading-[1.7] text-red-500">
          커플 연결을 끊으면<br />
          연결 정보와 리포트/추천 결과가 삭제돼요.
        </p>

        <p v-if="disconnectError" class="mt-3 text-center text-[13px] text-red-500">
          {{ disconnectError }}
        </p>

        <div class="mt-8 flex gap-3">
          <button
            type="button"
            class="h-12 flex-1 cursor-pointer rounded-xl bg-gray-100 text-[18px] font-bold text-gray-600 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="isDisconnectingCouple"
            @click="closeDisconnectSheet"
          >
            취소
          </button>

          <button
            type="button"
            class="h-12 flex-1 cursor-pointer rounded-xl bg-red-500 text-[18px] font-bold text-white hover:bg-red-600 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="isDisconnectingCouple"
            @click="finishDisconnect"
          >
            {{ isDisconnectingCouple ? '연결 해제 중' : '연결 끊기' }}
          </button>
        </div>

        <div class="mt-3 text-center text-[13px]">
          <span class="font-bold text-red-500">3</span>
          <span class="text-gray-300"> / 3</span>
        </div>
      </template>
    </div>
  </div>

  <!-- ======================================== -->
  <!-- 로그아웃 확인 모달 -->
  <!-- ======================================== -->
  <div
    v-if="isLogoutModalOpen"
    class="fixed inset-0 z-[100] flex items-center justify-center bg-black/40 px-5"
    @click.self="closeLogoutModal"
  >
    <div class="w-full max-w-[350px] rounded-[24px] bg-white px-6 pb-6 pt-8 shadow-xl">
      <!-- 아이콘 -->
      <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-yellow-50">
        <LogOut :size="29" :stroke-width="2" class="text-yellow-500" />
      </div>

      <!-- 제목 -->
      <h2 class="mt-6 text-center text-[17px] font-bold text-gray-900">로그아웃 하시겠어요?</h2>

      <!-- 설명 -->
      <p class="mt-3 text-center text-[12px] leading-[1.7] text-gray-400">
        현재 계정에서 로그아웃됩니다.<br />
        다시 이용하려면 로그인해 주세요.
      </p>

      <!-- 버튼 -->
      <div class="mt-7 flex gap-3">
        <!-- 취소 -->
        <button
          type="button"
          class="h-12 flex-1 cursor-pointer rounded-xl bg-gray-100 text-[18px] font-bold text-gray-600 transition hover:bg-gray-200 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="isLoggingOut"
          @click="closeLogoutModal"
        >
          취소
        </button>

        <!-- 로그아웃 -->
        <button
          type="button"
          class="h-12 flex-1 cursor-pointer rounded-xl bg-yellow-300 text-[18px] font-bold text-gray-900 transition hover:bg-yellow-400 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="isLoggingOut"
          @click="confirmLogout"
        >
          {{ isLoggingOut ? '로그아웃 중' : '로그아웃' }}
        </button>
      </div>
    </div>
  </div>

  <!-- ======================================== -->
  <!-- 회원 탈퇴 바텀시트 -->
  <!-- ======================================== -->
  <div
    v-if="isWithdrawSheetOpen"
    class="fixed inset-0 z-[100] bg-black/40"
    @click.self="closeWithdrawSheet"
  >
    <div
      class="absolute bottom-0 left-1/2 w-full max-w-[430px] -translate-x-1/2 rounded-t-[28px] bg-white px-5 pb-7 pt-3 shadow-2xl"
    >
      <!-- 상단 손잡이 -->
      <div class="mx-auto h-1.5 w-12 rounded-full bg-gray-200"></div>

      <!-- 경고 아이콘 -->
      <div class="mx-auto mt-6 flex h-14 w-14 items-center justify-center rounded-full bg-red-50">
        <TriangleAlert :size="27" :stroke-width="2" class="text-red-500" />
      </div>

      <!-- 제목 -->
      <h2 class="mt-6 text-center text-[18px] font-bold text-gray-900">탈퇴하시겠어요?</h2>

      <!-- 설명 -->
      <p class="mt-3 text-center text-[12px] leading-[1.7] text-gray-400">
        탈퇴 시 계정 정보와 커플 연결,<br />
        함께 만든 리포트와 추천 결과가 삭제됩니다.<br />
        같은 이메일로 다시 가입할 수 없습니다.
      </p>

      <div class="mt-6 space-y-4">
        <div>
          <label class="text-[13px] font-semibold text-gray-700">현재 비밀번호</label>
          <input
            v-model="withdrawPassword"
            type="password"
            autocomplete="current-password"
            placeholder="비밀번호를 입력하세요"
            :disabled="isWithdrawing"
            class="mt-2 h-12 w-full rounded-xl border border-gray-200 bg-white px-3 text-[16px] text-gray-900 outline-none placeholder:text-gray-300 disabled:bg-gray-50"
          />
          <p v-if="withdrawPasswordError" class="mt-2 text-[13px] font-medium text-red-500">
            {{ withdrawPasswordError }}
          </p>
        </div>

        <div>
          <label class="text-[13px] font-semibold text-gray-700">확인 문구</label>
          <input
            v-model="withdrawConfirmationText"
            type="text"
            placeholder="회원 탈퇴"
            :disabled="isWithdrawing"
            class="mt-2 h-12 w-full rounded-xl border border-gray-200 bg-white px-3 text-[16px] text-gray-900 outline-none placeholder:text-gray-300 disabled:bg-gray-50"
          />
          <p class="mt-2 text-[12px] leading-[1.5] text-gray-400">
            확인을 위해 '{{ WITHDRAW_CONFIRMATION_TEXT }}'를 입력해 주세요.
          </p>
          <p v-if="withdrawConfirmationError" class="mt-2 text-[13px] font-medium text-red-500">
            {{ withdrawConfirmationError }}
          </p>
        </div>
      </div>

      <p v-if="withdrawGeneralError" class="mt-4 text-center text-[13px] font-medium text-red-500">
        {{ withdrawGeneralError }}
      </p>

      <!-- 버튼 -->
      <div class="mt-8 flex gap-3">
        <!-- 취소 -->
        <button
          type="button"
          class="h-12 flex-1 cursor-pointer rounded-xl bg-gray-100 text-[18px] font-bold text-gray-600 transition hover:bg-gray-200 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="isWithdrawing"
          @click="closeWithdrawSheet"
        >
          취소
        </button>

        <!-- 탈퇴 -->
        <button
          type="button"
          class="h-12 flex-1 cursor-pointer rounded-xl bg-red-500 text-[18px] font-bold text-white transition hover:bg-red-600 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="isWithdrawing"
          @click="confirmWithdraw"
        >
          {{ isWithdrawing ? '탈퇴 처리 중' : '탈퇴하기' }}
        </button>
      </div>
    </div>
  </div>
</template>
