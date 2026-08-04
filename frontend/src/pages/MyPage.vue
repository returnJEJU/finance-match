<script setup>
// TODO(담당자): 이 화면을 구현하세요. (기준: 찰떡궁합_UI.pdf)
// 화면: 마이페이지 · 레이아웃: DefaultLayout
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import stableImage from '@/assets/images/characters/types/type-stable.png'
import { disconnectCouple, getCoupleProfileMessage, updateCoupleProfileMessage } from '@/api/couple'
import { useAuthStore } from '@/stores/auth'

import {
  UserRound,
  ClipboardList,
  RefreshCw,
  HeartCrack,
  Heart,
  LogOut,
  UserRoundX,
  Pencil,
  ChevronRight,
  X,

  // 투자성향 모달
  ShieldCheck,

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

// 실제 화면에 표시되는 소개 문구
const profileMessage = ref('우리의 금융 여정')

// 모달 입력창에서 임시로 사용하는 값
const editMessage = ref('')

// 모달 열림 여부
const isEditModalOpen = ref(false)
const profileMessageError = ref('')
const coupleProfileQueryKey = computed(() => ['coupleProfileMessage', authStore.member?.id ?? 'me'])

const { data: coupleProfileMessage, isError: isProfileMessageLoadError } = useQuery({
  queryKey: coupleProfileQueryKey,
  queryFn: getCoupleProfileMessage,
})

const coupleDisplayName = computed(() => {
  const profile = coupleProfileMessage.value
  if (profile?.myName && profile?.partnerName) {
    return `${profile.myName} ♡ ${profile.partnerName}`
  }

  return '내 프로필'
})

watch(
  coupleProfileMessage,
  (response) => {
    if (response?.profileMessage) {
      profileMessage.value = response.profileMessage
    }
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

// 지금은 화면 확인용 목데이터
// 나중에 DB 연결하면 API 응답값으로 교체

const investmentProfile = ref({
  type: '안정형',
  characterImage: stableImage,
  description: '안정적인 조회가 중요해요!',
  content: `
민수님은 수익보다는 원금의 안정성을 우선으로 생각하는 타입이에요.
큰 수익보다는 손실 가능성을 낮추고 꾸준하게 자산을 지키는 것을 중요하게 생각해요.
투자 과정에서 가격 변동이나 원금 손실에 부담을 크게 느낄 수 있어요.
예·적금이나 채권 등 안정적인 상품을 중심으로 구성하는 것이 좋아요.
원금 보존을 중심으로 안정적으로 자산을 운용하는 포트폴리오가 가장 잘 맞을 거예요!
  `.trim(),
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
// 찜한 상품 바텀시트
// ========================================

const isFavoriteSheetOpen = ref(false)

const favoriteProducts = ref([
  {
    id: 1,
    name: '글로벌 테크 TOP10 ETF',
    risk: '초고위험',
    riskClass: 'bg-red-100 text-red-500',
    description: '우상향하는 미국 빅테크 기업 집중 투자',
    tags: ['ETF', '해외주식'],
  },
  {
    id: 2,
    name: '미국 배당 귀족주 포트폴리오',
    risk: '위험중립형',
    riskClass: 'bg-sky-100 text-sky-500',
    description: '분기별 안정적인 배당 수익 확보',
    tags: ['펀드', '해외주식'],
  },
  {
    id: 3,
    name: '친환경 신재생 에너지 펀드',
    risk: '적극투자형',
    riskClass: 'bg-orange-100 text-orange-500',
    description: '미래 성장을 주도할 그린 에너지 투자',
    tags: ['펀드', '테마주식'],
  },
])

const openFavoriteSheet = () => {
  sheetDragY.value = 0
  isFavoriteSheetOpen.value = true
}

const closeFavoriteSheet = () => {
  isFavoriteSheetOpen.value = false
  sheetDragY.value = 0
}

// ========================================
// 바텀시트 드래그
// ========================================

const sheetStartY = ref(0)
const sheetDragY = ref(0)
const isSheetDragging = ref(false)

// 드래그 시작
const startSheetDrag = (event) => {
  sheetStartY.value = event.touches[0].clientY
  isSheetDragging.value = true
}

// 드래그 중
const moveSheetDrag = (event) => {
  if (!isSheetDragging.value) return

  const currentY = event.touches[0].clientY
  const distance = currentY - sheetStartY.value

  // 위쪽으로는 움직이지 못하게
  sheetDragY.value = Math.max(distance, 0)
}

// 드래그 종료
const endSheetDrag = () => {
  isSheetDragging.value = false

  // 100px 이상 내렸으면 닫기
  if (sheetDragY.value >= 100) {
    // 아래로 내려가는 애니메이션
    sheetDragY.value = window.innerHeight

    setTimeout(() => {
      isFavoriteSheetOpen.value = false
      sheetDragY.value = 0
    }, 250)

    return
  }

  // 충분히 내리지 않았으면 원래 위치로 복귀
  sheetDragY.value = 0
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

const openLogoutModal = () => {
  isLogoutModalOpen.value = true
}

const closeLogoutModal = () => {
  isLogoutModalOpen.value = false
}

const confirmLogout = () => {
  authStore.logout()
  queryClient.clear()
  isLogoutModalOpen.value = false
  router.replace({ name: 'login' })
}

// ========================================
// 회원 탈퇴 바텀시트
// ========================================

const isWithdrawSheetOpen = ref(false)

const openWithdrawSheet = () => {
  isWithdrawSheetOpen.value = true
}

const closeWithdrawSheet = () => {
  isWithdrawSheetOpen.value = false
}

// 현재는 화면 확인용
const confirmWithdraw = () => {
  console.log('회원 탈퇴 - 추후 API 연결')

  isWithdrawSheetOpen.value = false
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

        <div class="mt-1 flex items-center text-[13px] text-gray-500">
          <span>{{ profileMessage }}</span>
          <span v-if="isProfileMessageLoadError" class="ml-1 text-red-400">
            저장된 소개를 불러오지 못했어요
          </span>

          <button
            type="button"
            class="cursor-pointer ml-1 flex items-center justify-center"
            @click="openEditModal"
          >
            <Pencil :size="11" :stroke-width="2" class="text-gray-500" />
          </button>
        </div>
      </div>
    </section>

    <!-- 내 정보 관리 -->
    <section class="mt-3">
      <h2 class="mb-2 text-[13px] font-medium text-gray-500">내 정보 관리</h2>

      <div
        class="flex cursor-pointer items-center rounded-xl border border-gray-200 bg-white px-3 py-3 shadow-sm"
        @click="openInvestmentModal"
      >
        <!-- 아이콘 -->
        <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-gray-100">
          <ClipboardList :size="20" :stroke-width="1.8" class="text-[#7c7500]" />
        </div>

        <!-- 텍스트 -->
        <div class="ml-3 flex-1">
          <p class="text-[18px] font-bold text-gray-900">나의 투자 성향</p>

          <p class="mt-0.5 text-[13px] text-gray-500">개인 투자 성향 결과 확인</p>
        </div>

        <!-- 오른쪽 화살표 -->
        <ChevronRight :size="18" :stroke-width="1.8" class="text-[#b8b18a]" />
      </div>
    </section>

    <!-- 데이터 관리 -->
    <section class="mt-3">
      <h2 class="mb-2 text-[13px] font-medium text-gray-500">데이터 관리</h2>

      <div
        class="flex cursor-pointer items-center rounded-xl border border-gray-200 bg-white px-3 py-3 shadow-sm"
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
    </section>

    <!-- 상품 추천 -->
    <section class="mt-3">
      <h2 class="mb-2 text-[13px] font-medium text-gray-500">상품 추천</h2>

      <div
        class="flex cursor-pointer items-center rounded-xl border border-gray-200 bg-white px-3 py-3 shadow-sm transition hover:bg-gray-50"
        @click="openFavoriteSheet"
      >
        <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-gray-100">
          <Heart :size="20" :stroke-width="1.8" class="text-pink-400" />
        </div>

        <div class="ml-3 flex-1">
          <p class="text-[18px] font-bold text-gray-900">찜한 상품</p>

          <p class="mt-0.5 text-[13px] text-gray-500">찜한 목록으로 이동합니다</p>
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
    <div class="relative w-full max-w-[400px] rounded-[24px] bg-white px-5 pb-6 pt-6 shadow-xl">
      <!-- 닫기 -->
      <button
        type="button"
        class="absolute right-4 top-4 flex h-8 w-8 items-center justify-center text-gray-400"
        @click="closeInvestmentModal"
      >
        <X :size="21" :stroke-width="1.8" />
      </button>

      <!-- 제목 -->
      <h2 class="text-center text-[15px] font-bold text-gray-900">개인 투자 성향 결과</h2>

      <!-- 투자 성향 -->
      <div class="mt-7 text-center">
        <p class="text-[13px] font-medium text-gray-500">민수님의 투자 성향은</p>

        <p class="mt-1 text-[22px] font-bold text-gray-900">
          <span class="text-emerald-400">
            {{ investmentProfile.type }}
          </span>
          입니다.
        </p>
      </div>

      <!-- 캐릭터 영역 -->
      <div class="relative mx-auto mt-5 flex h-[145px] w-[145px] items-center justify-center">
        <!-- 뒤쪽 은은한 배경 -->
        <div class="absolute h-[130px] w-[130px] rounded-full bg-yellow-100 blur-2xl"></div>

        <!--          나중에 캐릭터 이미지가 준비되면 아래 div 대신 img로 변경-->
        <img
          :src="investmentProfile.characterImage"
          alt="투자성향 캐릭터"
          class="relative z-10 h-[135px] object-contain"
        />

        <!--        <div-->
        <!--          class="relative z-10 flex h-[105px] w-[105px] items-center justify-center rounded-full bg-emerald-50"-->
        <!--        >-->
        <!--          <UserRound :size="52" :stroke-width="1.5" class="text-emerald-400" />-->
        <!--        </div>-->
      </div>

      <!-- 설명 카드 -->
      <div class="mt-5 rounded-xl border border-gray-200 bg-white px-4 py-4">
        <!-- 설명 제목 -->
        <div class="flex items-center justify-center gap-2">
          <ShieldCheck :size="19" :stroke-width="1.8" class="text-gray-700" />

          <h3 class="text-[18px] font-bold text-gray-800">
            {{ investmentProfile.description }}
          </h3>
        </div>

        <!-- 상세 내용 -->
        <p class="mt-4 whitespace-pre-line text-[13px] leading-[1.9] text-gray-600">
          {{ investmentProfile.content }}
        </p>
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
  <!-- 찜한 상품 바텀시트 -->
  <!-- ======================================== -->
  <div
    v-if="isFavoriteSheetOpen"
    class="fixed inset-0 z-[100] bg-black/40"
    @click.self="closeFavoriteSheet"
  >
    <div
      class="absolute bottom-0 left-1/2 max-h-[82vh] w-full max-w-[430px] overflow-y-auto rounded-t-[28px] bg-white px-5 pb-8 pt-3 shadow-2xl"
      :style="{
        transform: `translate(-50%, ${sheetDragY}px)`,
        transition: isSheetDragging ? 'none' : 'transform 0.25s ease',
      }"
    >
      <!-- 상단 핸들 -->
      <div
        class="flex cursor-grab touch-none justify-center py-2 active:cursor-grabbing"
        @touchstart="startSheetDrag"
        @touchmove.prevent="moveSheetDrag"
        @touchend="endSheetDrag"
      >
        <div class="h-1.5 w-12 rounded-full bg-gray-300"></div>
      </div>

      <!-- 헤더 -->
      <div class="mt-5 flex items-start justify-between">
        <div>
          <h2 class="text-[22px] font-bold text-gray-900">찜한 상품</h2>

          <p class="mt-2 text-[12px] text-gray-500">찜한 상품을 확인하고 비교해 보세요.</p>
        </div>

        <button
          type="button"
          class="flex h-8 w-8 cursor-pointer items-center justify-center text-gray-600"
          @click="closeFavoriteSheet"
        >
          <X :size="24" :stroke-width="2" />
        </button>
      </div>

      <!-- 상품 목록 -->
      <div class="mt-6 space-y-4">
        <div
          v-for="product in favoriteProducts"
          :key="product.id"
          class="flex items-center rounded-2xl border border-gray-200 bg-white px-5 py-5 shadow-sm"
        >
          <!-- 상품 정보 -->
          <div class="min-w-0 flex-1">
            <!-- 상품명 -->
            <h3 class="text-[14px] font-bold leading-[1.4] text-gray-900">
              {{ product.name }}
            </h3>

            <!-- 위험 등급 -->
            <span
              class="mt-1.5 inline-block rounded px-2 py-0.5 text-[13px] font-medium"
              :class="product.riskClass"
            >
              {{ product.risk }}
            </span>

            <!-- 설명 -->
            <p class="mt-1.5 text-[13px] leading-[1.5] text-gray-500">
              {{ product.description }}
            </p>

            <!-- 태그 -->
            <div class="mt-2 flex gap-2">
              <span
                v-for="tag in product.tags"
                :key="tag"
                class="rounded bg-gray-100 px-2 py-1 text-[13px] text-gray-500"
              >
                {{ tag }}
              </span>
            </div>
          </div>

          <!-- 찜 하트 -->
          <button
            type="button"
            class="ml-4 flex h-10 w-10 shrink-0 cursor-pointer items-center justify-center"
          >
            <Heart :size="23" :stroke-width="1.8" fill="currentColor" class="text-pink-400" />
          </button>
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
          class="h-12 flex-1 cursor-pointer rounded-xl bg-gray-100 text-[18px] font-bold text-gray-600 transition hover:bg-gray-200"
          @click="closeLogoutModal"
        >
          취소
        </button>

        <!-- 로그아웃 -->
        <button
          type="button"
          class="h-12 flex-1 cursor-pointer rounded-xl bg-yellow-300 text-[18px] font-bold text-gray-900 transition hover:bg-yellow-400"
          @click="confirmLogout"
        >
          로그아웃
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
        탈퇴 시 계정 정보와 연결 정보가 삭제되며<br />
        복구할 수 없습니다.
      </p>

      <!-- 버튼 -->
      <div class="mt-8 flex gap-3">
        <!-- 취소 -->
        <button
          type="button"
          class="h-12 flex-1 cursor-pointer rounded-xl bg-gray-100 text-[18px] font-bold text-gray-600 transition hover:bg-gray-200"
          @click="closeWithdrawSheet"
        >
          취소
        </button>

        <!-- 탈퇴 -->
        <button
          type="button"
          class="h-12 flex-1 cursor-pointer rounded-xl bg-red-500 text-[18px] font-bold text-white transition hover:bg-red-600"
          @click="confirmWithdraw"
        >
          탈퇴하기
        </button>
      </div>
    </div>
  </div>
</template>
