<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CreditCard, Heart, LoaderCircle, LockKeyhole, RefreshCw } from 'lucide-vue-next'

import { getInvitation } from '@/api/invitation'
import { getOnboardingStatus } from '@/api/onboarding'

const router = useRouter()

const status = ref(null)
const inviteCode = ref('')
const loading = ref(true)
const refreshing = ref(false)
const refreshMessage = ref('')

// 초대코드는 생성됐지만 아직 커플 연동이 되지 않은 상태
const showInvitationCode = computed(() => {
  return (
    status.value?.hasInvitation === true &&
    status.value?.coupleConnected === false &&
    status.value?.personalSurveyCompleted === true
  )
})

// 커플 연동은 됐지만 상대방의 개인 설문이 끝나지 않은 상태
const showPartnerSurveyWaiting = computed(() => {
  return (
    status.value?.coupleConnected === true &&
    status.value?.personalSurveyCompleted === true &&
    status.value?.partnerPersonalSurveyCompleted !== true
  )
})

// 내 개인 설문이 완료되지 않은 상태
const showMySurveyWaiting = computed(() => {
  return status.value != null && status.value.personalSurveyCompleted === false
})

// 궁합 계산을 시작할 수 있는 상태
const isReadyForMatch = computed(() => {
  return (
    status.value?.coupleConnected === true &&
    status.value?.personalSurveyCompleted === true &&
    status.value?.partnerPersonalSurveyCompleted === true
  )
})

const loadWaitingStatus = async () => {
  const latestStatus = await getOnboardingStatus()
  status.value = latestStatus

  // 초대코드가 있고 아직 커플 연동 전이면 실제 초대코드를 가져온다.
  if (
    latestStatus.hasInvitation === true &&
    latestStatus.coupleConnected === false &&
    latestStatus.personalSurveyCompleted === true
  ) {
    const invitation = await getInvitation()
    inviteCode.value = invitation.inviteCode || ''
  } else {
    inviteCode.value = ''
  }

  // 양쪽 설문이 모두 끝났으면 궁합 계산 화면으로 이동한다.
  if (isReadyForMatch.value) {
    router.replace('/match/calculating')
  }
}

const handleRefresh = async () => {
  if (refreshing.value) {
    return
  }

  refreshing.value = true
  refreshMessage.value = ''

  try {
    await loadWaitingStatus()

    if (showInvitationCode.value) {
      refreshMessage.value = '아직 상대방과 연결되지 않았습니다.'
      return
    }

    if (showMySurveyWaiting.value) {
      refreshMessage.value = '아직 내 개인설문이 완료되지 않았습니다.'
      return
    }

    if (showPartnerSurveyWaiting.value) {
      refreshMessage.value = '상대방의 개인설문이 완료되지 않았습니다.'
      return
    }

    if (!status.value?.coupleConnected) {
      refreshMessage.value = '아직 상대방과 연결되지 않았습니다.'
    }
  } catch (error) {
    refreshMessage.value =
      error?.message || '상태를 확인하지 못했습니다. 잠시 후 다시 시도해주세요.'
  } finally {
    refreshing.value = false
  }
}

onMounted(async () => {
  try {
    await loadWaitingStatus()
  } catch (error) {
    refreshMessage.value =
      error?.message || '상태를 확인하지 못했습니다. 잠시 후 다시 시도해주세요.'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="flex min-h-[calc(100vh-108px)] flex-col px-5 py-4">
    <!-- 상단 안내 문구 -->
    <p class="text-center text-[15px] font-semibold text-brand-ink">
      함께 만드는 안정적인 우리의 금융 미래 💛
    </p>

    <!-- 흰색 메인 카드 -->
    <div
      class="mt-5 flex flex-1 flex-col items-center rounded-[30px] bg-white px-6 pt-12 pb-10 shadow-[0_4px_20px_rgba(0,0,0,0.04)]"
    >
      <!-- 최초 상태 조회 -->
      <div v-if="loading" class="flex flex-1 flex-col items-center justify-center">
        <LoaderCircle class="animate-spin text-brand-ink" :size="36" :stroke-width="2" />

        <p class="mt-4 text-[14px] font-semibold text-ink-sub">진행 상태를 확인하고 있어요.</p>
      </div>

      <template v-else>
        <!-- 자물쇠 영역 -->
        <div class="relative flex h-[230px] w-[230px] shrink-0 items-center justify-center">
          <!-- 연한 노란색 원 -->
          <div class="absolute inset-0 rounded-full bg-brand-soft" />

          <!-- 원 안쪽 장식 -->
          <div class="absolute inset-[18px] rounded-full border-[10px] border-white/50" />

          <!-- 왼쪽 하트 -->
          <Heart class="absolute top-1 left-0 text-[#ebe9dc]" :size="43" :stroke-width="1.8" />

          <!-- 오른쪽 카드 장식 -->
          <CreditCard
            class="absolute right-0 bottom-8 rotate-12 text-[#d7d3c0]"
            :size="36"
            :stroke-width="1.5"
          />

          <!-- 가운데 흰색 원 -->
          <div
            class="relative flex h-[112px] w-[112px] items-center justify-center rounded-full bg-white shadow-[0_8px_20px_rgba(90,85,30,0.16)]"
          >
            <LockKeyhole class="text-brand-ink" :size="54" :stroke-width="2.4" />
          </div>
        </div>

        <!-- 상태별 안내 영역 -->
        <div class="mt-10 min-h-[94px] text-center">
          <!-- 초대코드는 있지만 커플 연동 전 -->
          <template v-if="showInvitationCode">
            <p class="text-[15px] font-medium text-ink-sub">나의 코드를 상대방에게 보내주세요</p>

            <h1
              class="mt-3 font-mono text-[32px] leading-none font-extrabold tracking-[0.12em] text-ink"
            >
              {{ inviteCode || '--------' }}
            </h1>
          </template>

          <!-- 커플 연동 후 상대방 개인 설문 대기 -->
          <template v-else-if="showPartnerSurveyWaiting">
            <h1 class="text-[18px] font-bold text-ink">아직 궁합을 계산할 수 없습니다</h1>

            <p class="mt-3 text-[15px] leading-6 text-ink-sub">
              상대방이 개인 설문을 완료하면<br />
              새로고침 버튼을 눌러 확인해주세요.
            </p>
          </template>

          <!-- 내 개인 설문 미완료 -->
          <template v-else-if="showMySurveyWaiting">
            <h1 class="text-[18px] font-bold text-ink">내 개인 설문이 완료되지 않았습니다</h1>

            <p class="mt-3 text-[15px] leading-6 text-ink-sub">
              개인 설문을 완료한 뒤<br />
              새로고침 버튼을 눌러 확인해주세요.
            </p>
          </template>

          <!-- 초대코드와 커플 연동이 모두 없는 상태 -->
          <template v-else>
            <h1 class="text-[18px] font-bold text-ink">아직 궁합을 계산할 수 없습니다</h1>

            <p class="mt-3 text-[15px] leading-6 text-ink-sub">
              커플 연동 상태를 확인하려면<br />
              새로고침 버튼을 눌러주세요.
            </p>
          </template>
        </div>

        <!-- 새로고침 버튼 -->
        <button
          type="button"
          class="mt-12 flex h-12 w-[200px] shrink-0 items-center justify-center gap-2 rounded-full border-2 border-brand bg-white text-[15px] font-semibold text-ink transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="refreshing"
          @click="handleRefresh"
        >
          <RefreshCw :size="17" :stroke-width="2.2" :class="{ 'animate-spin': refreshing }" />

          {{ refreshing ? '확인 중...' : '새로고침' }}
        </button>

        <!-- 새로고침 결과 -->
        <p
          v-if="refreshMessage"
          role="status"
          class="mt-4 text-center text-[12px] leading-5 font-semibold text-red-500"
        >
          {{ refreshMessage }}<br />
          잠시만 기다려주세요.
        </p>
      </template>
    </div>
  </section>
</template>
